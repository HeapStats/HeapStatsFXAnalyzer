/*
 * Copyright (C) 2014-2015 Yasumasa Suenaga
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package jp.co.ntt.oss.heapstats.plugin.builtin.log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Popup;
import jp.co.ntt.oss.heapstats.WindowController;
import jp.co.ntt.oss.heapstats.container.log.ArchiveData;
import jp.co.ntt.oss.heapstats.container.log.DiffData;
import jp.co.ntt.oss.heapstats.container.log.LogData;
import jp.co.ntt.oss.heapstats.container.log.SummaryData;
import jp.co.ntt.oss.heapstats.lambda.ConsumerWrapper;
import jp.co.ntt.oss.heapstats.lambda.FunctionWrapper;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.task.ParseLogFile;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;
import jp.co.ntt.oss.heapstats.utils.TaskAdapter;

/**
 * FXML Controller of LOG builtin plugin.
 *
 * @author Yasumasa Suenaga
 */
public class LogController extends PluginController implements Initializable{
    
    @FXML
    private ComboBox<LocalDateTime> startCombo;
    
    @FXML
    private ComboBox<LocalDateTime> endCombo;
    
    @FXML
    private TextField logFileList;
    
    @FXML
    private GridPane chartGrid;
    
    @FXML
    private StackedAreaChart<String, Double> javaCPUChart;
    
    private XYChart.Series<String, Double> javaUserUsage;
    
    private XYChart.Series<String, Double> javaSysUsage;
    
    @FXML
    private StackedAreaChart<String, Double> systemCPUChart;
    
    private XYChart.Series<String, Double> systemUserUsage;
    
    private XYChart.Series<String, Double> systemNiceUsage;
    
    private XYChart.Series<String, Double> systemSysUsage;
    
    private XYChart.Series<String, Double> systemIdleUsage;
    
    private XYChart.Series<String, Double> systemIOWaitUsage;
    
    private XYChart.Series<String, Double> systemIRQUsage;
    
    private XYChart.Series<String, Double> systemSoftIRQUsage;
    
    private XYChart.Series<String, Double> systemStealUsage;
    
    private XYChart.Series<String, Double> systemGuestUsage;
    
    @FXML
    private LineChart<String, Long> javaMemoryChart;
    
    private XYChart.Series<String, Long> javaVSZUsage;
    
    private XYChart.Series<String, Long> javaRSSUsage;
    
    @FXML
    private LineChart<String, Long> safepointChart;
    
    private XYChart.Series<String, Long> safepoints;
    
    @FXML
    private LineChart<String, Long> safepointTimeChart;
    
    private XYChart.Series<String, Long> safepointTime;
    
    @FXML
    private LineChart<String, Long> threadChart;
    
    private XYChart.Series<String, Long> threads;
    
    @FXML
    private LineChart<String, Long> monitorChart;
    
    private XYChart.Series<String, Long> monitors;
    
    @FXML
    private TableView<SummaryData.SummaryDataEntry> procSummary;
    
    @FXML
    private TableColumn<SummaryData.SummaryDataEntry, String> categoryColumn;

    @FXML
    private TableColumn<SummaryData.SummaryDataEntry, String> valueColumn;
    
    @FXML
    private ComboBox<ArchiveData> archiveCombo;
    
    @FXML
    private TableView<Map.Entry<String, String>> archiveEnvInfoTable;

    @FXML
    private TableColumn<Map.Entry<String, String>, String> archiveKeyColumn;

    @FXML
    private TableColumn<Map.Entry<String, String>, String> archiveVauleColumn;

    @FXML
    private ComboBox<String> fileCombo;
    
    @FXML
    private TextArea logArea;
    
    @FXML
    private Button okBtn;

    List<LogData> logEntries;
    
    List<DiffData> diffEntries;
    
    private Popup chartPopup;
    
    private Text popupText;
    
    private List<LocalDateTime> suspectList;
    
    
    /**
     * Initialize Series in Chart.
     * This method uses to avoid RuntimeException which is related to:
     *   RT-37994: [FXML] ProxyBuilder does not support read-only collections
     *   https://javafx-jira.kenai.com/browse/RT-37994
     */
    @SuppressWarnings("unchecked")
    private void initializeChartSeries(){
        threads = new XYChart.Series<>();
        threads.setName("Threads");
        threadChart.getData().add(threads);

        javaUserUsage = new XYChart.Series<>();
        javaUserUsage.setName("user");
        javaSysUsage = new XYChart.Series<>();
        javaSysUsage.setName("sys");
        javaCPUChart.getData().addAll(javaUserUsage, javaSysUsage);
        
        systemUserUsage = new XYChart.Series<>();
        systemUserUsage.setName("user");
        systemNiceUsage = new XYChart.Series<>();
        systemNiceUsage.setName("nice");
        systemSysUsage = new XYChart.Series<>();
        systemSysUsage.setName("sys");
        systemIdleUsage = new XYChart.Series<>();
        systemIdleUsage.setName("idle");
        systemIOWaitUsage = new XYChart.Series<>();
        systemIOWaitUsage.setName("I/O wait");
        systemIRQUsage = new XYChart.Series<>();
        systemIRQUsage.setName("IRQ");
        systemSoftIRQUsage = new XYChart.Series<>();
        systemSoftIRQUsage.setName("soft IRQ");
        systemStealUsage = new XYChart.Series<>();
        systemStealUsage.setName("steal");
        systemGuestUsage = new XYChart.Series<>();
        systemGuestUsage.setName("guest");
        systemCPUChart.getData().addAll(systemUserUsage, systemNiceUsage, systemSysUsage,
                                         systemIdleUsage, systemIOWaitUsage, systemIRQUsage,
                                         systemSoftIRQUsage, systemStealUsage, systemGuestUsage);
        
        javaVSZUsage = new XYChart.Series<>();
        javaVSZUsage.setName("VSZ");
        javaRSSUsage = new XYChart.Series<>();
        javaRSSUsage.setName("RSS");
        javaMemoryChart.getData().addAll(javaVSZUsage, javaRSSUsage);
        
        safepoints = new XYChart.Series<>();
        safepoints.setName("Safepoints");
        safepointChart.getData().add(safepoints);
        
        safepointTime = new XYChart.Series<>();
        safepointTime.setName("Safepoint Time");
        safepointTimeChart.getData().add(safepointTime);
        
        monitors = new XYChart.Series<>();
        monitors.setName("Monitors");
        monitorChart.getData().add(monitors);
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, rb);
        
        startCombo.setConverter(new LocalDateTimeConverter());
        endCombo.setConverter(new LocalDateTimeConverter());
        archiveCombo.setConverter(new ArchiveDataConverter());
        
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        archiveKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        archiveVauleColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        String bgcolor = "-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";";
        javaCPUChart.lookup(".chart").setStyle(bgcolor);
        systemCPUChart.lookup(".chart").setStyle(bgcolor);
        javaMemoryChart.lookup(".chart").setStyle(bgcolor);
        safepointChart.lookup(".chart").setStyle(bgcolor);
        safepointTimeChart.lookup(".chart").setStyle(bgcolor);
        threadChart.lookup(".chart").setStyle(bgcolor);
        monitorChart.lookup(".chart").setStyle(bgcolor);
        
        initializeChartSeries();
        
        chartPopup = new Popup();
        popupText = new Text();
        chartPopup.getContent().add(popupText);
        
        okBtn.disableProperty().bind(startCombo.getSelectionModel().selectedIndexProperty().greaterThanOrEqualTo(endCombo.getSelectionModel().selectedIndexProperty()));
        
        setOnWindowResize((v, o, n) -> Platform.runLater(() -> {
                                                                  drawArchiveLine();
                                                                  drawRebootSuspectLine();
                                                               }));
    }
    
    /**
     * onSucceeded event handler for LogFileParser.
     * 
     * @param parser Targeted LogFileParser.
     */
    private void onLogFileParserSucceeded(ParseLogFile parser){
        startCombo.getItems().clear();
        endCombo.getItems().clear();
        archiveCombo.getItems().clear();

        logEntries  = parser.getLogEntries();
        diffEntries = parser.getDiffEntries();
        List<LocalDateTime> timeline = logEntries.stream()
                                                 .map(d -> d.getDateTime())
                                                 .collect(Collectors.toList());        
        startCombo.getItems().addAll(timeline);
        startCombo.getSelectionModel().selectFirst();
        endCombo.getItems().addAll(timeline);
        endCombo.getSelectionModel().selectLast();

        logEntries.stream()
                  .filter(d -> d.getArchivePath() != null)
                  .map(new FunctionWrapper<>(ArchiveData::new))
                  .peek(new ConsumerWrapper<>(a -> a.parseArchive()))
                  .forEach(archiveCombo.getItems()::add);                                          
    }
    
    /**
     * Event handler of LogFile button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    public void onLogFileClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        ResourceBundle resource = ResourceBundle.getBundle("logResources", new Locale(HeapStatsUtils.getLanguage()));
        dialog.setTitle(resource.getString("dialog.filechooser.title"));
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("Log file (*.csv)", "*.csv"),
                                            new ExtensionFilter("All files", "*.*"));
        
        List<File> logList = dialog.showOpenMultipleDialog(WindowController.getInstance().getOwner());
        
        if(logList != null){
            HeapStatsUtils.setDefaultDirectory(logList.get(0).getParent());
            String logListStr = logList.stream()
                                       .map(File::getAbsolutePath)
                                       .collect(Collectors.joining("; "));

            logFileList.setText(logListStr);
            
            TaskAdapter<ParseLogFile> task = new TaskAdapter<>(new ParseLogFile(logList));
            task.setOnSucceeded(evt -> onLogFileParserSucceeded(task.getTask()));
            super.bindTask(task);
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
    }
    
    private void drawLineInternal(StackPane target, List<String> drawList, String style){
        AnchorPane anchor = null;
        XYChart chart = null;
        
        for(Node node : ((StackPane)target).getChildren()){
            
            if(node instanceof AnchorPane){
                anchor = (AnchorPane)node;
            }
            else if(node instanceof XYChart){
                chart = (XYChart)node;
            }
            
            if((anchor != null) && (chart != null)){
                break;
            }
            
        }
        
        if((anchor == null) || (chart == null)){
            throw new IllegalStateException("Could not find node to draw line.");
        }
                                                        
        CategoryAxis xAxis = (CategoryAxis)chart.getXAxis();
        Axis yAxis = chart.getYAxis();
        Label chartTitle = (Label)chart.getChildrenUnmodifiable().stream()
                                                                 .filter(n -> n.getStyleClass().contains("chart-title"))
                                                                 .findFirst()
                                                                 .get();
        
        double startX = xAxis.getLayoutX() + xAxis.getStartMargin() - 1.0d;
        double yPos = yAxis.getLayoutY() + chartTitle.getLayoutY() + chartTitle.getHeight();
        List<Rectangle> rectList = drawList.stream()
                                           .map(s -> new Rectangle(xAxis.getDisplayPosition(s) + startX, yPos, 2.0d, yAxis.getHeight()))
                                           .peek(r -> ((Rectangle)r).setStyle(style))
                                           .collect(Collectors.toList());
        anchor.getChildren().addAll(rectList);
    }
    
    private void drawArchiveLine(){
        
        if(archiveCombo.getItems().isEmpty()){
            return;
        }
        
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        List<String> archiveDateList = archiveCombo.getItems().stream()
                                                              .map(a -> converter.toString(a.getDate()))
                                                              .collect(Collectors.toList());
        chartGrid.getChildren().stream()
                               .filter(n -> n instanceof StackPane)
                               .forEach(p -> drawLineInternal((StackPane)p, archiveDateList, "-fx-fill: black;"));
    }
    
    /**
     * Draw line which represents to suspect to reboot.
     * This method does not clear AnchorPane to draw lines.
     * So this method must be called after drawArchiveLine().
     */
    private void drawRebootSuspectLine(){
        
        if((suspectList == null) || suspectList.isEmpty()){
            return;
        }
        
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        List<String> suspectRebootDateList = suspectList.stream()
                                                        .map(d -> converter.toString(d))
                                                        .collect(Collectors.toList());
        chartGrid.getChildren().stream()
                               .filter(n -> n instanceof StackPane)
                               .forEach(p -> drawLineInternal((StackPane)p, suspectRebootDateList, "-fx-fill: goldenrod;"));
    }
    
    /**
     * Task class for drawing log chart data.
     */
    private class DrawLogChartTask extends Task<Void>{
        
        /**
         * Start time which is drawn.
         */
        private final LocalDateTime start;
        
        /**
         * End time which is drawn.
         */
        private final  LocalDateTime end;

        /**
         * Constructor of DrawLogChartTask.
         * 
         * @param start Start Time
         * @param end End time
         */
        public DrawLogChartTask(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected Void call() throws Exception {
            LocalDateTimeConverter converter = new LocalDateTimeConverter();
            List<LogData> targetLogData = logEntries.parallelStream()
                                                    .filter(d -> ((d.getDateTime().compareTo(start) >= 0) && (d.getDateTime().compareTo(end) <= 0)))
                                                    .collect(Collectors.toList());
            List<DiffData> targetDiffData = diffEntries.parallelStream()
                                                       .filter(d -> ((d.getDateTime().compareTo(start) >= 0) && (d.getDateTime().compareTo(end) <= 0)))
                                                       .collect(Collectors.toList());
        
            /* Java CPU */
            ObservableList<XYChart.Data<String, Double>> javaUserUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> javaSysUsageBuf = FXCollections.observableArrayList();

            /* System CPU */
            ObservableList<XYChart.Data<String, Double>> systemUserUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemNiceUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemSysUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemIdleUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemIOWaitUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemIRQUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemSoftIRQUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemStealUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Double>> systemGuestUsageBuf = FXCollections.observableArrayList();

            /* Java Memory */
            ObservableList<XYChart.Data<String, Long>> javaVSZUsageBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Long>> javaRSSUsageBuf = FXCollections.observableArrayList();

            /* Safepoints */
            ObservableList<XYChart.Data<String, Long>> safepointsBuf = FXCollections.observableArrayList();
            ObservableList<XYChart.Data<String, Long>> safepointTimeBuf = FXCollections.observableArrayList();

            /* Threads */
            ObservableList<XYChart.Data<String, Long>> threadsBuf = FXCollections.observableArrayList();

            /* Monitor contantion */
            ObservableList<XYChart.Data<String, Long>> monitorsBuf = FXCollections.observableArrayList();
            
            LongAdder counter = new LongAdder();
            long totalLoopCount = targetDiffData.size() + targetLogData.size();
        
            /* Generate graph data */
            targetDiffData.forEach(d -> {
                                           String time = converter.toString(d.getDateTime());
                                              
                                           javaUserUsageBuf.add(new XYChart.Data<>(time, d.getJavaUserUsage()));
                                           javaSysUsageBuf.add(new XYChart.Data<>(time, d.getJavaSysUsage()));
                                           systemUserUsageBuf.add(new XYChart.Data<>(time, d.getCpuUserUsage()));
                                           systemNiceUsageBuf.add(new XYChart.Data<>(time, d.getCpuNiceUsage()));
                                           systemSysUsageBuf.add(new XYChart.Data<>(time, d.getCpuSysUsage()));
                                           systemIdleUsageBuf.add(new XYChart.Data<>(time, d.getCpuIdleUsage()));
                                           systemIOWaitUsageBuf.add(new XYChart.Data<>(time, d.getCpuIOWaitUsage()));
                                           systemIRQUsageBuf.add(new XYChart.Data<>(time, d.getCpuIRQUsage()));
                                           systemSoftIRQUsageBuf.add(new XYChart.Data<>(time, d.getCpuSoftIRQUsage()));
                                           systemStealUsageBuf.add(new XYChart.Data<>(time, d.getCpuStealUsage()));
                                           systemGuestUsageBuf.add(new XYChart.Data<>(time, d.getCpuGuestUsage()));
                                           monitorsBuf.add(new XYChart.Data<>(time, d.getJvmSyncPark()));
                                           safepointsBuf.add(new XYChart.Data<>(time, d.getJvmSafepoints()));
                                           safepointTimeBuf.add(new XYChart.Data<>(time, d.getJvmSafepointTime()));
                                                  
                                           counter.increment();
                                           updateProgress(counter.longValue(), totalLoopCount);
                                        });
            targetLogData.stream()
                         .forEachOrdered(d -> {
                                                 String time = converter.toString(d.getDateTime());

                                                 javaVSZUsageBuf.add(new XYChart.Data<>(time, d.getJavaVSSize() / 1024 / 1024));
                                                 javaRSSUsageBuf.add(new XYChart.Data<>(time, d.getJavaRSSize() / 1024 / 1024));
                                                 threadsBuf.add(new XYChart.Data<>(time, d.getJvmLiveThreads()));

                                                 counter.increment();
                                                 updateProgress(counter.longValue(), totalLoopCount);
                                              });
        
            Platform.runLater(() -> {
                                       /* Replace new chart data */
                                       javaUserUsage.setData(javaUserUsageBuf);
                                       javaSysUsage.setData(javaSysUsageBuf);

                                       systemUserUsage.setData(systemUserUsageBuf);
                                       systemNiceUsage.setData(systemNiceUsageBuf);
                                       systemSysUsage.setData(systemSysUsageBuf);
                                       systemIdleUsage.setData(systemIdleUsageBuf);
                                       systemIOWaitUsage.setData(systemIOWaitUsageBuf);
                                       systemIRQUsage.setData(systemIRQUsageBuf);
                                       systemSoftIRQUsage.setData(systemSoftIRQUsageBuf);
                                       systemStealUsage.setData(systemStealUsageBuf);
                                       systemGuestUsage.setData(systemGuestUsageBuf);

                                       monitors.setData(monitorsBuf);

                                       safepoints.setData(safepointsBuf);
                                       safepointTime.setData(safepointTimeBuf);

                                       javaVSZUsage.setData(javaVSZUsageBuf);
                                       javaRSSUsage.setData(javaRSSUsageBuf);

                                       threads.setData(threadsBuf);

                                       /* Put summary data to table */
                                       SummaryData summary = new SummaryData(targetLogData, targetDiffData);
                                       ResourceBundle resource = ResourceBundle.getBundle("logResources", new Locale(HeapStatsUtils.getLanguage()));
                                       procSummary.setItems(FXCollections.observableArrayList(new SummaryData.SummaryDataEntry(resource.getString("summary.cpu.average"), String.format("%.1f %%", summary.getAverageCPUUsage())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.cpu.peak"), String.format("%.1f %%", summary.getMaxCPUUsage())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.vsz.average"), String.format("%.1f MB", summary.getAverageVSZ())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.vsz.peak"), String.format("%.1f MB", summary.getMaxVSZ())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.rss.average"), String.format("%.1f MB", summary.getAverageRSS())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.rss.peak"), String.format("%.1f MB", summary.getMaxRSS())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.threads.average"), String.format("%.1f", summary.getAverageLiveThreads())),
                                                                                              new SummaryData.SummaryDataEntry(resource.getString("summary.threads.peak"), Long.toString(summary.getMaxLiveThreads()))
                                                                                             ));
                                       
                                       /*
                                        * drawArchiveLine() needs positions in each chart.
                                        * So I call it next event.
                                        */
                                       suspectList = targetDiffData.stream()
                                                                   .filter(d -> d.hasMinusData())
                                                                   .map(d -> d.getDateTime())
                                                                   .collect(Collectors.toList());
                                       Platform.runLater(() -> {
                                                                  drawArchiveLine();
                                                                  drawRebootSuspectLine();
                                                               });
                                    });
            
            return null;
        }
        
    }

    /**
     * Event handler of OK button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOkClick(ActionEvent event){
        /* Get range */
        LocalDateTime start = startCombo.getValue();
        LocalDateTime end   = endCombo.getValue();
        
        DrawLogChartTask task = new DrawLogChartTask(start, end);
        super.bindTask(task);
        Thread drawChartThread = new Thread(task);
        drawChartThread.start();        
    }
    
    /**
     * Event handler of archive combobox.
     * This handler is fired that user select archive.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onArchiveComboAction(ActionEvent event){
        archiveEnvInfoTable.getItems().clear();
        fileCombo.getItems().clear();
        ArchiveData target = archiveCombo.getValue();
        
        if(target == null){
            return;
        }
        
        /*
         * Convert Map to List.
         * Map.Entry of HashMap (HashMap$Node) is package private class. So JavaFX
         * cannot access them through reflection API.
         * Thus I convert Map.Entry to AbstractMap.SimpleEntry.
         */
        target.getEnvInfo().entrySet().forEach(e -> archiveEnvInfoTable.getItems().add(new AbstractMap.SimpleEntry<>(e)));
        
        fileCombo.getItems().addAll(target.getFileList());
    }
    
    
    /**
     * Event handler of selecting log in this archive.
     * This handler is fired that user select log.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onFileComboAction(ActionEvent event){
        ArchiveData target = archiveCombo.getValue();
        String file = fileCombo.getValue();
        
        try {
            logArea.setText(target.getFileContents(file));
        } catch (IOException ex) {
            HeapStatsUtils.showExceptionDialog(ex);
            logArea.setText("");
        }

    }
    
    /**
     * Show popup window with pointing data in chart.
     * 
     * @param chart Target chart.
     * @param xValue value in X Axis.
     * @param event Mouse event.
     */
    private void showChartPopup(XYChart<String, Number> chart, String xValue, MouseEvent event){
        String label = chart.getData().stream()
                                      .map(s -> s.getName() + " = " + s.getData().stream()
                                                                                 .filter(d -> d.getXValue().equals(xValue))
                                                                                 .map(d -> d.getYValue().toString())
                                                                                 .findAny()
                                                                                 .orElse("<none>"))
                                      .collect(Collectors.joining("\n"));

        popupText.setText(xValue + "\n" + label);
        chartPopup.show(chart, event.getScreenX() + 3.0d, event.getScreenY() + 3.0d);
    }
    
    @FXML
    @SuppressWarnings("unchecked")
    private void onChartMouseMoved(MouseEvent event){
        XYChart<String, Number> chart = (XYChart<String, Number>)event.getSource();
        CategoryAxis xAxis = (CategoryAxis)chart.getXAxis();
        double startXPoint = xAxis.getLayoutX() + xAxis.getStartMargin();
        
        Optional.ofNullable(chart.getXAxis().getValueForDisplay(event.getX() - startXPoint))
                .ifPresent(v -> showChartPopup(chart, v, event));
    }
    
    @FXML
    private void onChartMouseExited(MouseEvent event){
        chartPopup.hide();
    }
    
    /**
     * Returns plugin name.
     * This value is used to show in main window tab.
     * 
     * @return Plugin name.
     */
    @Override
    public String getPluginName() {
        return "Log Data";
    }

    @Override
    public EventHandler<Event> getOnPluginTabSelected() {
        return null;
    }

    @Override
    public String getLicense() {
        return PluginController.LICENSE_GPL_V2;
    }

    @Override
    public Map<String, String> getLibraryLicense() {
        return null;
    }

    @Override
    public Runnable getOnCloseRequest() {
        return null;
    }

    @Override
    public void setData(Object data, boolean select) {
        super.setData(data, select);
        logFileList.setText((String)data);
        
        TaskAdapter<ParseLogFile> task = new TaskAdapter<>(new ParseLogFile(Arrays.asList(new File((String)data))));
        task.setOnSucceeded(evt -> onLogFileParserSucceeded(task.getTask()));
        super.bindTask(task);
        
        Thread parseThread = new Thread(task);
        parseThread.start();
    }
    
}
