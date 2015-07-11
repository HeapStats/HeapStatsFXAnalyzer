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

package jp.co.ntt.oss.heapstats.plugin.builtin.snapshot;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.xml.bind.JAXB;
import jp.co.ntt.oss.heapstats.WindowController;
import jp.co.ntt.oss.heapstats.container.snapshot.DiffData;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.SnapShotHeader;
import jp.co.ntt.oss.heapstats.container.snapshot.SummaryData;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.task.CSVDumpGC;
import jp.co.ntt.oss.heapstats.task.CSVDumpHeap;
import jp.co.ntt.oss.heapstats.task.DiffCalculator;
import jp.co.ntt.oss.heapstats.task.ParseHeader;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;
import jp.co.ntt.oss.heapstats.utils.TaskAdapter;
import jp.co.ntt.oss.heapstats.xml.binding.Filter;
import jp.co.ntt.oss.heapstats.xml.binding.Filters;

/**
 * FXML Controller of SnapShot builtin plugin.
 *
 * @author Yasumasa Suenaga
 */
public class SnapShotController extends PluginController implements Initializable{
    
    @FXML
    private ComboBox<SnapShotHeader> startCombo;
    
    @FXML
    private ComboBox<SnapShotHeader> endCombo;
    
    @FXML
    private TextField snapshotList;
    
    @FXML
    private TableView<SummaryData.SummaryDataEntry> summaryTable;
    
    @FXML
    private TableColumn<SummaryData.SummaryDataEntry, String> keyColumn;
    
    @FXML
    private TableColumn<SummaryData.SummaryDataEntry, String> valueColumn;
    
    @FXML
    private StackedAreaChart<String, Long> heapChart;
    
    private XYChart.Series<String, Long> youngUsage;
    
    private XYChart.Series<String, Long> oldUsage;
    
    private XYChart.Series<String, Long> free;
    
    @FXML
    private LineChart<String, Long> gcTimeChart;
    
    private XYChart.Series<String, Long> gcTime;
    
    @FXML
    private AreaChart<String, Long> metaspaceChart;
    
    private XYChart.Series<String, Long> metaspaceUsage;
    
    private XYChart.Series<String, Long> metaspaceCapacity;
    
    @FXML
    private TableView<Filter> excludeTable;
    
    @FXML
    private TableColumn<Filter, Boolean> hideColumn;
    
    @FXML
    private TableColumn<Filter, String> excludeNameColumn;
    
    @FXML
    private TextField searchText;
    
    @FXML
    private ListView<String> searchList;
    
    @FXML
    private StackedAreaChart<String, Long> topNChart;
    
    @FXML
    private TableView<DiffData> lastDiffTable;
    
    @FXML
    private TableColumn<DiffData, String> colorColumn;
    
    @FXML
    private TableColumn<DiffData, String> classNameColumn;
    
    @FXML
    private TableColumn<DiffData, String> classLoaderColumn;
    
    @FXML
    private TableColumn<DiffData, Long> instanceColumn;
    
    @FXML
    private TableColumn<DiffData, Long> totalSizeColumn;
    
    @FXML
    private ComboBox<SnapShotHeader> snapShotTimeCombo;
    
    @FXML
    private TableView<Map.Entry<String, String>> snapShotSummaryTable;
    
    @FXML
    private TableColumn<Map.Entry<String, String>, String> snapShotSummaryKey;

    @FXML
    private TableColumn<Map.Entry<String, String>, String> snapShotSummaryValue;
    
    @FXML
    private PieChart usagePieChart;
    
    @FXML
    private TableColumn<ObjectData, String> objColorColumn;

    @FXML
    private TableView<ObjectData> objDataTable;
    
    @FXML
    private TableColumn<ObjectData, String> objClassNameColumn;

    @FXML
    private TableColumn<ObjectData, String> objClassLoaderColumn;

    @FXML
    private TableColumn<ObjectData, Long> objInstancesColumn;

    @FXML
    private TableColumn<ObjectData, Long> objSizeColumn;
    
    @FXML
    private Tab histogramTab;
    
    @FXML
    private Button okBtn;
    
    @FXML
    private Button selectFilterApplyBtn;
    
    private List<SnapShotHeader> currentTarget;
    
    private Set<String> currentClassNameSet;

    private Map<LocalDateTime, List<ObjectData>> topNList;
    
    private boolean searchFilterEnable;
    
    private boolean excludeFilterEnable;
    
    
    /**
     * Initialize Series in Chart.
     * This method uses to avoid RuntimeException which is related to:
     *   RT-37994: [FXML] ProxyBuilder does not support read-only collections
     *   https://javafx-jira.kenai.com/browse/RT-37994
     */
    private void initializeChartSeries(){
        youngUsage = new XYChart.Series<>();
        youngUsage.setName("Young");
        oldUsage = new XYChart.Series<>();
        oldUsage.setName("Old");
        free = new XYChart.Series<>();
        free.setName("Free");
        heapChart.getData().addAll(youngUsage, oldUsage, free);
        
        gcTime = new XYChart.Series<>();
        gcTime.setName("GC Time");
        gcTimeChart.getData().add(gcTime);
        
        metaspaceCapacity = new XYChart.Series<>();
        metaspaceCapacity.setName("Capacity");
        metaspaceUsage = new XYChart.Series<>();
        metaspaceUsage.setName("Usage");
        metaspaceChart.getData().addAll(metaspaceCapacity, metaspaceUsage);
        
        searchFilterEnable = false;
        excludeFilterEnable = false;
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, rb);
        
        startCombo.setConverter(new SnapShotHeaderConverter());
        endCombo.setConverter(new SnapShotHeaderConverter());
        snapShotTimeCombo.setConverter(new SnapShotHeaderConverter());
        
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        hideColumn.setCellValueFactory(new PropertyValueFactory<>("hide"));
        hideColumn.setCellFactory(CheckBoxTableCell.forTableColumn(hideColumn));
        excludeNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        colorColumn.setCellFactory(p -> new TableCell<DiffData, String>() {
                                              @Override
                                              protected void updateItem(String item, boolean empty) {
                                                super.updateItem(item, empty);
                                                String style = Optional.ofNullable((DiffData)getTableRow().getItem())
                                                                       .filter(d -> d.isRanked())
                                                                       .map(d -> "-fx-background-color: " + ChartColorManager.getNextColor(d.getClassName()))
                                                                       .orElse("-fx-background-color: transparent;");
                                                setStyle(style);
                                              }
                                            });
        
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        classLoaderColumn.setCellValueFactory(new PropertyValueFactory<>("classLoaderName"));
        instanceColumn.setCellValueFactory(new PropertyValueFactory<>("instances"));
        totalSizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));

        snapShotSummaryKey.setCellValueFactory(new PropertyValueFactory<>("key"));
        snapShotSummaryValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        objColorColumn.setCellFactory(p -> new TableCell<ObjectData, String>() {
                                                 @Override
                                                 protected void updateItem(String item, boolean empty) {
                                                   super.updateItem(item, empty);
                                                   String style = Optional.ofNullable((ObjectData)getTableRow().getItem())
                                                                          .filter(o -> topNChart.getData().stream().anyMatch(d -> d.getName().equals(o.getName())))
                                                                          .map(o -> "-fx-background-color: " + ChartColorManager.getNextColor(o.getName()))
                                                                          .orElse("-fx-background-color: transparent;");
                                                   setStyle(style);
                                                 }
                                               });

        objClassNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        objClassLoaderColumn.setCellValueFactory(new PropertyValueFactory<>("loaderName"));
        objInstancesColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        objSizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
        
        searchList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        heapChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        gcTimeChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        metaspaceChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        topNChart.lookup(".chart").setStyle("-fx-background-color: " + HeapStatsUtils.getChartBgColor() + ";");
        
        initializeChartSeries();
        
        okBtn.disableProperty().bind(startCombo.getSelectionModel().selectedIndexProperty().greaterThanOrEqualTo(endCombo.getSelectionModel().selectedIndexProperty()));
        selectFilterApplyBtn.disableProperty().bind(searchList.selectionModelProperty().getValue().selectedItemProperty().isNull());
    }
    
    /**
     * Event handler of SnapShot file button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    public void onSnapshotFileClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));

        dialog.setTitle(resource.getString("dialog.filechooser.title"));
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("SnapShot file (*.dat)", "*.dat"),
                                            new ExtensionFilter("All files", "*.*"));
        
        List<File> snapshotFileList = dialog.showOpenMultipleDialog(WindowController.getInstance().getOwner());
        
        if(snapshotFileList != null){
            HeapStatsUtils.setDefaultDirectory(snapshotFileList.get(0).getParent());
            List<String> files = snapshotFileList.stream()
                                                 .map(File::getAbsolutePath)
                                                 .collect(Collectors.toList());
            snapshotList.setText(files.stream().collect(Collectors.joining("; ")));
            
            TaskAdapter<ParseHeader> task = new TaskAdapter<>(new ParseHeader(files, HeapStatsUtils.getReplaceClassName()));
            task.setOnSucceeded(evt -> {
                                         ObservableList<SnapShotHeader> list = FXCollections.observableArrayList(task.getTask().getSnapShotList());
                                         startCombo.setItems(list);
                                         endCombo.setItems(list);
                                         startCombo.getSelectionModel().selectFirst();
                                         endCombo.getSelectionModel().selectLast();
                                       });
            super.bindTask(task);
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
    }
    
    private void setTopNChartColor(XYChart.Series<String, Long> series){
        String color = ChartColorManager.getNextColor(series.getName());

        series.getNode().lookup(".chart-series-area-line").setStyle(String.format("-fx-stroke: %s;", color));
        series.getNode().lookup(".chart-series-area-fill").setStyle(String.format("-fx-fill: %s;", color));

        series.getData().stream()
                        .map(d -> d.getNode().lookup(".chart-area-symbol"))
                        .forEach(n -> n.setStyle(String.format("-fx-background-color: %s, white;", color)));
    }
    
    /**
     * Build TopN Data for Chart with givien data.
     * 
     * @param header SnapShot header which you want to build.
     * @param seriesMap Chart series map which is contains class name as key, chart series as value.
     * @param objData  ObjectData which is you want to build.
     */
    private void buildTopNChartData(SnapShotHeader header, Map<String, XYChart.Series<String, Long>> seriesMap, ObjectData objData){
        XYChart.Series<String, Long> series = seriesMap.get(objData.getName());

        if(series == null){
            series = new XYChart.Series<>();
            series.setName(objData.getName());
            topNChart.getData().add(series);
            seriesMap.put(objData.getName(), series);
        }

        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        String time = converter.toString(header.getSnapShotDate());
        long yValue = objData.getTotalSize() / 1024 / 1024;
        XYChart.Data<String, Long> data = new XYChart.Data<>(time, yValue);

        series.getData().add(data);
        String tip = String.format("%s: %s, %d MB", series.getName(), time, yValue);
        Tooltip.install(data.getNode(), new Tooltip(tip));
    }
    
    /**
     * onSucceeded event handler for DiffTask.
     * 
     * @param diff Target task.
     * @param seriesMap Chart series map which is contains class name as key, chart series as value.
     */
    private void onDiffTaskSucceeded(DiffCalculator diff, Map<String, XYChart.Series<String, Long>> seriesMap){
        topNList = diff.getTopNList();

        currentTarget.stream()
                     .forEachOrdered(h ->  topNList.get(h.getSnapShotDate()).stream()
                                                                            .forEachOrdered(o -> buildTopNChartData(h, seriesMap, o)));
        
        lastDiffTable.getItems().addAll(diff.getLastDiffList());
        snapShotTimeCombo.getSelectionModel().selectLast();
        topNChart.getData().forEach(this::setTopNChartColor);
        
        long maxVal = topNChart.getData().stream()
                               .flatMap(s -> s.dataProperty().get().stream())
                               .collect(Collectors.groupingBy(d -> d.getXValue(), Collectors.summingLong(d -> d.getYValue())))
                               .values()
                               .stream()
                               .mapToLong(Long::longValue)
                               .max()
                               .getAsLong();
        ((ValueAxis)topNChart.getYAxis()).setUpperBound(maxVal * 1.05d);
    }
    
    /**
     * Drawing Top N data to Chart and Table.
     * 
     * @param target SnapShot to be drawed.
     * @param includeOthers *Others* data should be included in this Top N data.
     * @param predicate Filter function.
     */
    private void drawTopNData(List<SnapShotHeader> target, boolean includeOthers, Predicate<? super ObjectData> predicate){                                     
        topNChart.getData().clear();
        lastDiffTable.getItems().clear();
        Map<String, XYChart.Series<String, Long>> seriesMap = new HashMap<>();
        
        TaskAdapter<DiffCalculator> diff = new TaskAdapter<>(new DiffCalculator(target, HeapStatsUtils.getRankLevel(), includeOthers, predicate, HeapStatsUtils.getReplaceClassName()));
        diff.setOnSucceeded(evt -> onDiffTaskSucceeded(diff.getTask(), seriesMap));
        super.bindTask(diff);
        Thread diffThread = new Thread(diff);
        diffThread.start();
    }

    /**
     * Event handler of OK button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOkClick(ActionEvent event){
        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        
        int startIdx = startCombo.getSelectionModel().getSelectedIndex();
        int endIdx = endCombo.getSelectionModel().getSelectedIndex();
        currentTarget = startCombo.getItems().subList(startIdx, endIdx + 1);
        
        /* Java Heap Usage Chart */
        ObservableList<XYChart.Data<String, Long>> youngUsageBuf = FXCollections.observableArrayList();
        ObservableList<XYChart.Data<String, Long>> oldUsageBuf = FXCollections.observableArrayList();
        ObservableList<XYChart.Data<String, Long>> freeBuf = FXCollections.observableArrayList();
        
        /* GC time Chart */
        ObservableList<XYChart.Data<String, Long>> gcTimeBuf = FXCollections.observableArrayList();
        
        /* Metaspace Chart */
        ObservableList<XYChart.Data<String, Long>> metaspaceUsageBuf = FXCollections.observableArrayList();
        ObservableList<XYChart.Data<String, Long>> metaspaceCapacityBuf = FXCollections.observableArrayList();

        snapShotTimeCombo.setItems(FXCollections.observableArrayList(currentTarget));
        drawTopNData(currentTarget, true, null);
        
        currentClassNameSet = new HashSet<>();

        currentTarget.stream()
                     .forEachOrdered(d -> {
                                             String time = converter.toString(d.getSnapShotDate());
                                      
                                             youngUsageBuf.add(new XYChart.Data<>(time, d.getNewHeap() / 1024 / 1024));
                                             oldUsageBuf.add(new XYChart.Data<>(time, d.getOldHeap() / 1024 / 1024));
                                             freeBuf.add(new XYChart.Data<>(time, (d.getTotalCapacity() - d.getNewHeap() - d.getOldHeap()) / 1024 / 1024));
                                      
                                             gcTimeBuf.add(new XYChart.Data<>(time, d.getGcTime()));
                                      
                                             metaspaceUsageBuf.add(new XYChart.Data<>(time, d.getMetaspaceUsage() / 1024 / 1024));
                                             metaspaceCapacityBuf.add(new XYChart.Data<>(time, d.getMetaspaceCapacity() / 1024 / 1024));
                                             
                                             currentClassNameSet.addAll(d.getSnapShot(HeapStatsUtils.getReplaceClassName())
                                                                         .values()
                                                                         .stream()
                                                                         .map(s -> s.getName())
                                                                         .collect(Collectors.toSet()));
                                          });
        
        /* Replace new chart data */
        youngUsage.setData(youngUsageBuf);
        oldUsage.setData(oldUsageBuf);
        free.setData(freeBuf);
        
        gcTime.setData(gcTimeBuf);
        
        metaspaceUsage.setData(metaspaceUsageBuf);
        metaspaceCapacity.setData(metaspaceCapacityBuf);
        
        SummaryData summary = new SummaryData(currentTarget);
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));
        summaryTable.setItems(FXCollections.observableArrayList(new SummaryData.SummaryDataEntry(resource.getString("summary.snapshot.count"), Integer.toString(summary.getCount())),
                                                                new SummaryData.SummaryDataEntry(resource.getString("summary.gc.count"),String.format("%d (Full: %d, Young: %d)", summary.getFullCount() + summary.getYngCount(), summary.getFullCount(), summary.getYngCount())),
                                                                new SummaryData.SummaryDataEntry(resource.getString("summary.heap.usage"), String.format("%.1f MB", summary.getLatestHeapUsage() / 1024.0d / 1024.0d)),
                                                                new SummaryData.SummaryDataEntry(resource.getString("summary.metaspace.usage"), String.format("%.1f MB", summary.getLatestMetaspaceUsage() / 1024.0d / 1024.0d)),
                                                                new SummaryData.SummaryDataEntry(resource.getString("summary.gc.time"), String.format("%d ms", summary.getMaxGCTime())),
                                                                new SummaryData.SummaryDataEntry(resource.getString("summary.snapshot.size"), String.format("%.1f KB", summary.getMaxSnapshotSize() / 1024.0d)),
                                                                new SummaryData.SummaryDataEntry(resource.getString("summary.snapshot.entrycount"), Long.toString(summary.getMaxEntryCount()))
                                                               ));
    }
    
    /**
     * Event handler of changing search TextField.
     * 
     * @param event KeyEvent of this event.
     */
    @FXML
    private void onSearchTextChanged(KeyEvent event){
        searchList.getItems().clear();
        searchList.getItems().addAll(currentClassNameSet.stream()
                                                        .filter(n -> n.contains(searchText.getText()))
                                                        .collect(Collectors.toList()));
    }

    /**
     * Event handler of SnapShot TIme.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSnapShotTimeSelected(ActionEvent event){
        SnapShotHeader header = snapShotTimeCombo.getSelectionModel().getSelectedItem();
        if(header == null){
            return;
        }

        ObservableList<Map.Entry<String, String>> summaryList = snapShotSummaryTable.getItems();
        summaryList.clear();
        usagePieChart.getData().clear();
        objDataTable.getItems().clear();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));
        
        summaryList.addAll(
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.date"), (new LocalDateTimeConverter()).toString(header.getSnapShotDate())),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.entries"), Long.toString(header.getNumEntries())),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.instances"), Long.toString(header.getNumInstances())),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.heap"), String.format("%.02f MB", (double)(header.getNewHeap() + header.getOldHeap()) / 1024.0d /1024.0d)),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.metaspace"), String.format("%.02f MB", (double)(header.getMetaspaceUsage()) / 1024.0d /1024.0d)),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.cause"), header.getCauseString()),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.gccause"), header.getGcCause()),
                new AbstractMap.SimpleEntry<>(resource.getString("snapshot.gctime"), String.format("%d ms", header.getGcTime())));
        
        usagePieChart.getData().addAll(topNList.get(header.getSnapShotDate()).stream()
                                                                             .map(o -> new PieChart.Data(o.getName(), o.getTotalSize()))
                                                                             .collect(Collectors.toList()));
        usagePieChart.getData().stream()
                               .forEach(d -> d.getNode().setStyle("-fx-pie-color: " + ChartColorManager.getNextColor(d.getName())));
        
        objDataTable.setItems(FXCollections.observableArrayList(
                header.getSnapShot(HeapStatsUtils.getReplaceClassName()).values().stream().collect(Collectors.toList())));
    }
    
    /**
     * Drawing and Showing table with beging selected value.
     * 
     * @param enableSearch Search filter should be enabled.
     * @param enableHide Hide filter should be enabled.
     */
    private Predicate<? super ObjectData> getFilter(boolean enableSearch, boolean enableHide){
        HashSet<String> targetSet = new HashSet<>(searchList.getSelectionModel().getSelectedItems());
        Predicate<ObjectData> searchFilter = o -> targetSet.contains(o.getName());
        
        List<String> hideRegexList = excludeTable.getItems().stream()
                                                 .filter(f -> f.isHide())
                                                 .flatMap(f -> f.getClasses().getName().stream())
                                                 .map(s -> ".*" + s + ".*")
                                                 .collect(Collectors.toList());
        Predicate<ObjectData> hideFilter = o -> hideRegexList.stream().noneMatch(s -> o.getName().matches(s));
        
        Predicate<ObjectData> filter;
        
        if(enableSearch && enableHide){
            filter = searchFilter.and(hideFilter);
        }
        else if(enableSearch){
            filter = searchFilter;
        }
        else if(enableHide){
            filter = hideFilter;
        }
        else{
            filter = null;
        }
        
        return filter;
    }
    
    /**
     * Selection method for incremental search.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSelectFilterApply(ActionEvent event){
        searchFilterEnable = true;
        Predicate<? super ObjectData> filter = getFilter(searchFilterEnable, excludeFilterEnable);
        drawTopNData(currentTarget, false, filter);
    }
    
    /**
     * Event handler of clear button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSelectFilterClear(ActionEvent event){
        searchFilterEnable = false;
        excludeFilterEnable = false;
        drawTopNData(currentTarget, false, null);
    }
    
    /**
     * Event handler for adding exclude filter.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onAddClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));

        dialog.setTitle(resource.getString("dialog.filterchooser.title"));
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("Filter file (*.xml)", "*.xml"),
                                            new ExtensionFilter("All files", "*.*"));
        
        List<File> excludeFilterList = dialog.showOpenMultipleDialog(((Node)event.getSource()).getScene().getWindow());
        if(excludeFilterList != null){
            excludeFilterList.stream()
                             .map(f -> (Filters)JAXB.unmarshal(f, Filters.class))
                             .filter(f -> f != null)
                             .flatMap(f -> f.getFilter().stream()
                                                        .map(e -> new BindingFilter(e)))
                             .forEach(f -> excludeTable.getItems().addAll(f));
        }
        
    }
    
    /**
     * Event handler of apply button in exclude filter.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onHiddenFilterApply(ActionEvent event){
        excludeFilterEnable = true;
        Predicate<? super ObjectData> filter = getFilter(searchFilterEnable, excludeFilterEnable);
        drawTopNData(currentTarget, false, filter);
    }
    
    /**
     * Returns plugin name.
     * This value is used to show in main window tab.
     * 
     * @return Plugin name.
     */
    @Override
    public String getPluginName() {
        return "SnapShot Data";
    }
    
    /**
     * Get SnapShot header which is selected.
     * This method returns snapshot header which is selected ins SnapShot Data tab.
     * 
     * @return selected snapshot header.
     */
    public SnapShotHeader getSelectedSnapShotHeader(){
        return snapShotTimeCombo.getSelectionModel().getSelectedItem();
    }
    
    /**
     * Get selected snapshot.
     * This method returns snapshot which is selected in SnapShot Data tab.
     * 
     * @return selected snapshot.
     */
    public Map<Long, ObjectData> getSelectedSnapShot(){
        return snapShotTimeCombo.getSelectionModel().getSelectedItem().getSnapShot(HeapStatsUtils.getReplaceClassName());
    }
    
    /**
     * Get selected object.
     * If histogram tab is active and diff data is selected, this method returns
     * tag which is selected. Other case, this method returns tag which is selected
     * in snapshot data tab.
     * 
     * If no object is selected, throws IllegalStateException.
     * 
     * @return class tag which is selected.
     * @throws IllegalStateException no object is selected.
     */
    public long getSelectedClassTag() throws IllegalStateException{
        
        if(histogramTab.isSelected() && (lastDiffTable.getSelectionModel().getSelectedItem() != null)){
            return lastDiffTable.getSelectionModel().getSelectedItem().getTag();
        }
        else if(objDataTable.getSelectionModel().getSelectedItem() != null){
            return objDataTable.getSelectionModel().getSelectedItem().getTag();
        }
        else{
            throw new IllegalStateException("Object is not selected");
        }
        
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
    
    /**
     * Dump GC Statistics to CSV.
     * 
     * @param isSelected If this value is true, this method dumps data which is selected time range,
     *                    otherwise this method dumps all snapshot data.
     */
    public void dumpGCStatisticsToCSV(boolean isSelected){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Select CSV files");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("CSV file (*.csv)", "*.csv"),
                                            new ExtensionFilter("All files", "*.*"));
        File csvFile = dialog.showSaveDialog(WindowController.getInstance().getOwner());
        
        if(csvFile != null){
            TaskAdapter<CSVDumpGC> task = new TaskAdapter<>(new CSVDumpGC(csvFile, isSelected ? currentTarget : startCombo.getItems()));
            super.bindTask(task);
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
    }
    
    /**
     * Dump Java Class Histogram to CSV.
     * 
     * @param isSelected If this value is true, this method dumps data which is selected in class filter,
     *                    otherwise this method dumps all snapshot data.
     */
    public void dumpClassHistogramToCSV(boolean isSelected){
        FileChooser dialog = new FileChooser();
        ResourceBundle resource = ResourceBundle.getBundle("snapshotResources", new Locale(HeapStatsUtils.getLanguage()));

        dialog.setTitle(resource.getString("dialog.csvchooser.title"));
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("CSV file (*.csv)", "*.csv"),
                                            new ExtensionFilter("All files", "*.*"));
        File csvFile = dialog.showSaveDialog(WindowController.getInstance().getOwner());
        
        if(csvFile != null){
            Predicate<? super ObjectData> filter = (searchFilterEnable || excludeFilterEnable) ? getFilter(searchFilterEnable, excludeFilterEnable) : null;
            TaskAdapter<CSVDumpHeap> task = new TaskAdapter<>(new CSVDumpHeap(csvFile, isSelected ? currentTarget : startCombo.getItems(), isSelected ? filter : null, HeapStatsUtils.getReplaceClassName()));
            super.bindTask(task);
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
    }

    @Override
    public Runnable getOnCloseRequest() {
        return null;
    }
    
    @Override
    public void setData(Object data, boolean select) {
        super.setData(data, select);
        snapshotList.setText((String)data);
        
        TaskAdapter<ParseHeader> task = new TaskAdapter<>(new ParseHeader(Arrays.asList((String)data), HeapStatsUtils.getReplaceClassName()));
        task.setOnSucceeded(evt ->{
                                     startCombo.setItems(FXCollections.observableArrayList(task.getTask().getSnapShotList()));
                                     endCombo.setItems(FXCollections.observableArrayList(task.getTask().getSnapShotList()));
                                     startCombo.getSelectionModel().selectFirst();
                                     endCombo.getSelectionModel().selectLast();
                                  });
        super.bindTask(task);
        
        Thread parseThread = new Thread(task);
        parseThread.start();
    }
    
}
