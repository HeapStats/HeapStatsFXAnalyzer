/*
 * Copyright (C) 2014 Yasumasa Suenaga
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.model.ArchiveData;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.model.DiffData;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.model.LogData;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.model.SummaryData;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.InfoDialog;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;

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
    private StackedAreaChart<String, Double> javaCPUChart;
    
    @FXML
    private StackedAreaChart<String, Double> systemCPUChart;
    
    @FXML
    private LineChart<String, Long> javaMemoryChart;
    
    @FXML
    private LineChart<String, Long> safepointChart;
    
    @FXML
    private LineChart<String, Long> safepointTimeChart;
    
    @FXML
    private LineChart<String, Long> threadChart;
    
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

    List<LogData> logEntries;
    
    List<DiffData> diffEntries;
    
    
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
        
        javaCPUChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        systemCPUChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        javaMemoryChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        safepointChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        threadChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
    }
    
    /**
     * Event handler of LogFile button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onLogFileClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Select log files");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("Log file (*.csv)", "*.csv"),
                                            new ExtensionFilter("All files", "*.*"));
        
        List<File> logList = dialog.showOpenMultipleDialog(((Node)event.getSource()).getScene().getWindow());
        
        if(logList != null){
            HeapStatsUtils.setDefaultDirectory(logList.get(0).getParent());
            String logListStr = logList.stream()
                                       .map(File::getAbsolutePath)
                                       .collect(Collectors.joining("; "));

            logFileList.setText(logListStr);
            
            final LogFileParser parser = new LogFileParser(logList);
            parser.setOnSucceeded(evt ->{
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
                                          
                                          List<ArchiveData> archiveList = logEntries.stream()
                                                                                     .filter(d -> d.getArchivePath() != null)
                                                                                     .map(d -> new ArchiveData(d))
                                                                                     .collect(Collectors.toList());
                                          if(archiveList.size() > 0){
                                            archiveList.forEach(a -> a.parseArchive());
                                            archiveCombo.getItems().addAll(archiveList);
                                          }
                                          
                                        });
            super.bindTask(parser);
            
            Thread parseThread = new Thread(parser);
            parseThread.start();
        }
        
    }

    /**
     * Event handler of OK button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOkClick(ActionEvent event){
        
        if(logEntries == null){
            InfoDialog dialog1 = new InfoDialog("Error", "Please select log file.", null);
            dialog1.show();
            return;
        }
        
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        
        /* Clear all data */
        javaCPUChart.getData().clear();
        systemCPUChart.getData().clear();
        javaMemoryChart.getData().clear();
        safepointChart.getData().clear();
        safepointTimeChart.getData().clear();
        procSummary.getItems().clear();
        
        /* Get range */
        LocalDateTime start = startCombo.getValue();
        LocalDateTime end   = endCombo.getValue();
        List<LogData> targetLogData = logEntries.parallelStream()
                                                     .filter(d -> ((d.getDateTime().compareTo(start) >= 0) && (d.getDateTime().compareTo(end) <= 0)))
                                                     .collect(Collectors.toList());
        List<DiffData> targetDiffData = diffEntries.parallelStream()
                                                        .filter(d -> ((d.getDateTime().compareTo(start) >= 0) && (d.getDateTime().compareTo(end) <= 0)))
                                                        .collect(Collectors.toList());
        
        /* Java CPU */
        XYChart.Series<String, Double> javaUserUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> javaSysUsage = new XYChart.Series<>();
        javaUserUsage.setName("user");
        javaSysUsage.setName("sys");
        javaCPUChart.getData().addAll(javaUserUsage, javaSysUsage);
        javaUserUsage.getNode().setId("javaCPUUserSeries");
        javaSysUsage.getNode().setId("javaCPUSysSeries");

        /* System CPU */
        XYChart.Series<String, Double> systemUserUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemNiceUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemSysUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemIdleUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemIOWaitUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemIRQUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemSoftIRQUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemStealUsage = new XYChart.Series<>();
        XYChart.Series<String, Double> systemGuestUsage = new XYChart.Series<>();
        systemUserUsage.setName("user");
        systemNiceUsage.setName("nice");
        systemSysUsage.setName("sys");
        systemIdleUsage.setName("idle");
        systemIOWaitUsage.setName("I/O wait");
        systemIRQUsage.setName("IRQ");
        systemSoftIRQUsage.setName("soft IRQ");
        systemStealUsage.setName("steal");
        systemGuestUsage.setName("guest");
        systemCPUChart.getData().addAll(systemUserUsage, systemNiceUsage, systemSysUsage,
                                        systemIdleUsage, systemIOWaitUsage, systemIRQUsage,
                                        systemSoftIRQUsage, systemStealUsage, systemGuestUsage);
        systemUserUsage.getNode().setId("systemCPUUserSeries");
        systemNiceUsage.getNode().setId("systemCPUNiceSeries");
        systemSysUsage.getNode().setId("systemCPUSysSeries");
        systemIdleUsage.getNode().setId("systemCPUIdleSeries");
        systemIOWaitUsage.getNode().setId("systemCPUIOWaitSeries");
        systemIRQUsage.getNode().setId("systemCPUIRQSeries");
        systemSoftIRQUsage.getNode().setId("systemCPUSoftIRQSeries");
        systemStealUsage.getNode().setId("systemCPUStealSeries");
        systemGuestUsage.getNode().setId("systemCPUGuestSeries");
        
        /* Java Memory */
        XYChart.Series<String, Long> javaVSZUsage = new XYChart.Series<>();
        XYChart.Series<String, Long> javaRSSUsage = new XYChart.Series<>();
        javaVSZUsage.setName("VSZ");
        javaRSSUsage.setName("RSS");
        javaMemoryChart.getData().addAll(javaVSZUsage, javaRSSUsage);

        /*
         * This code does not work.
         * I want to set color style to seies through CSS ID.
         */
        //javaVSZUsage.getNode().setId("javaVSZSeries");
        //javaRSSUsage.getNode().setId("javaRSSSeries");
        
        /* Safepoints */
        XYChart.Series<String, Long> safepoints = new XYChart.Series<>();
        XYChart.Series<String, Long> safepointTime = new XYChart.Series<>();
        safepoints.setName("Safepoints");
        safepointTime.setName("Safepoint Time");
        safepointChart.getData().add(safepoints);
        safepointTimeChart.getData().add(safepointTime);
        
        /* Threads */
        XYChart.Series<String, Long> threads = new XYChart.Series<>();
        XYChart.Series<String, Long> monitors = new XYChart.Series<>();
        threads.setName("Threads");
        monitors.setName("Monitors");
        threadChart.getData().addAll(threads, monitors);
        
        /* Generate graph data */
        targetDiffData.stream()
                      .forEachOrdered(d -> {
                                              String time = converter.toString(d.getDateTime());
                                              String label = archiveCombo.getItems().stream()
                                                                                    .filter(a -> a.getDate().equals(d.getDateTime()))
                                                                                    .findAny()
                                                                                    .map(a -> String.format("(%s)", a.getEnvInfo().get("LogTrigger")))
                                                                                    .orElse("");
                                              
                                              addChartDataAsPercent(javaUserUsage, time, d.getJavaUserUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(javaSysUsage, time, d.getJavaSysUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemUserUsage, time, d.getCpuUserUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemNiceUsage, time, d.getCpuNiceUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemSysUsage, time, d.getCpuSysUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemIdleUsage, time, d.getCpuIdleUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemIOWaitUsage, time, d.getCpuIOWaitUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemIRQUsage, time, d.getCpuIRQUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemSoftIRQUsage, time, d.getCpuSoftIRQUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemStealUsage, time, d.getCpuStealUsage(), label, label.length() > 0);
                                              addChartDataAsPercent(systemGuestUsage, time, d.getCpuGuestUsage(), label, label.length() > 0);
                                              addChartDataLong(monitors, time, d.getJvmSyncPark(), label, label.length() > 0);
                                           });
        targetLogData.stream()
                     .forEachOrdered(d -> {
                                             String time = converter.toString(d.getDateTime());
                                             String label = archiveCombo.getItems().stream()
                                                                                   .filter(a -> a.getDate().equals(d.getDateTime()))
                                                                                   .findAny()
                                                                                   .map(a -> String.format("(%s)", a.getEnvInfo().get("LogTrigger")))
                                                                                   .orElse("");

                                             addChartDataLong(javaVSZUsage, time, d.getJavaVSSize() / 1024 / 1024, "MB " + label, label.length() > 0);
                                             addChartDataLong(javaRSSUsage, time, d.getJavaRSSize() / 1024 / 1024, "MB " + label, label.length() > 0);
                                             addChartDataLong(safepoints, time, d.getJvmSafepoints(), label, label.length() > 0);
                                             addChartDataLong(safepointTime, time, d.getJvmSafepointTime(), "ms " + label, label.length() > 0);
                                             addChartDataLong(threads, time, d.getJvmLiveThreads(), label, label.length() > 0);
                                          });
        
        /* Put summary data to table */
        procSummary.getItems().addAll((new SummaryData(targetLogData, targetDiffData)).getSummaryAsList());
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
            Logger.getLogger(LogController.class.getName()).log(Level.SEVERE, null, ex);
            logArea.setText("");
        }

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
    
}
