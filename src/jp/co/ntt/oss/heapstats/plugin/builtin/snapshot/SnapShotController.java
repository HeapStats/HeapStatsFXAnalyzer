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

package jp.co.ntt.oss.heapstats.plugin.builtin.snapshot;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.xml.bind.JAXB;
import jp.co.ntt.oss.heapstats.container.ObjectData;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.handler.DiffTask;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.handler.ParseHeaderTask;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.handler.SnapShotParseTask;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.model.DiffData;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.model.SummaryData;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;
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
    
    @FXML
    private LineChart<String, Long> gcTimeChart;
    
    @FXML
    private AreaChart<String, Long> metaspaceChart;
    
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
    
    private List<SnapShotHeader> currentTarget;

    private Map<LocalDateTime, List<ObjectData>> topNList;
    
    private Map<SnapShotHeader, Map<Long, ObjectData>> snapShots;

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

        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        classLoaderColumn.setCellValueFactory(new PropertyValueFactory<>("classLoaderName"));
        instanceColumn.setCellValueFactory(new PropertyValueFactory<>("instances"));
        totalSizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));

        snapShotSummaryKey.setCellValueFactory(new PropertyValueFactory<>("key"));
        snapShotSummaryValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        objClassNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        objClassLoaderColumn.setCellValueFactory(new PropertyValueFactory<>("loaderName"));
        objInstancesColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        objSizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
        
        searchList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
    
    /**
     * Event handler of SnapShot file button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSnapshotFileClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Select SnapShot files");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("SnapShot file (*.dat)", "*.dat"),
                                            new ExtensionFilter("All files", "*.*"));
        
        List<File> snapshotFileList = dialog.showOpenMultipleDialog(((Node)event.getSource()).getScene().getWindow());
        
        if(snapshotFileList != null){
            HeapStatsUtils.setDefaultDirectory(snapshotFileList.get(0).getParent());
            String snapshotListStr = snapshotFileList.stream()
                                                     .map(File::getAbsolutePath)
                                                     .collect(Collectors.joining("; "));

            snapshotList.setText(snapshotListStr);
            
            ParseHeaderTask task = new ParseHeaderTask(snapshotFileList.stream()
                                                                             .map(File::getAbsolutePath)
                                                                             .collect(Collectors.toList()));
            task.setOnSucceeded(evt ->{
                                         startCombo.getItems().clear();
                                         endCombo.getItems().clear();
                                          
                                         startCombo.getItems().addAll(task.getSnapShotList());
                                         startCombo.getSelectionModel().selectFirst();
                                         endCombo.getItems().addAll(task.getSnapShotList());
                                         endCombo.getSelectionModel().selectLast();
                                      });
            super.bindTask(task);
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
    }
    
    /**
     * Drawing Top N data to Chart and Table.
     * 
     * @param target SnapShot to be drawed.
     */
    private void drawTopNData(Map<SnapShotHeader, Map<Long, ObjectData>> target){                                     
        topNChart.getData().clear();
        lastDiffTable.getItems().clear();

        LocalDateTimeConverter converter = new LocalDateTimeConverter();
        Map<String, XYChart.Series<String, Long>> seriesMap = new HashMap<>();
        
        DiffTask diffTask = new DiffTask(target, HeapStatsUtils.getRankLevel());
        diffTask.setOnSucceeded(evt -> {
                                          topNList = diffTask.getTopNList();
                                          currentTarget.stream()
                                                       .forEachOrdered(h -> {
                                                                               List<ObjectData> list = topNList.get(h.getSnapShotDate());
                                                                               list.stream()
                                                                                   .forEachOrdered(o -> {
                                                                                                           XYChart.Series<String, Long> series = seriesMap.get(o.getName());

                                                                                                           if(series == null){
                                                                                                             series = new XYChart.Series<>();
                                                                                                             series.setName(o.getName());
                                                                                                             topNChart.getData().add(series);
                                                                                                             seriesMap.put(o.getName(), series);
                                                                                                           }

                                                                                                           String time = converter.toString(h.getSnapShotDate());
                                                                                                           addChartDataLong(series, time, o.getTotalSize() / 1024 / 1024, "MB");
                                                                                                        });
                                                                            });
                                          lastDiffTable.getItems().addAll(diffTask.getLastDiffList());
                                          snapShotTimeCombo.getSelectionModel().selectLast();
                                       });
                                     
        super.bindTask(diffTask);
        Thread diffThread = new Thread(diffTask);
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
        summaryTable.getItems().clear();
        heapChart.getData().clear();
        gcTimeChart.getData().clear();
        metaspaceChart.getData().clear();
        snapShotTimeCombo.getItems().clear();
        
        int startIdx = startCombo.getSelectionModel().getSelectedIndex();
        int endIdx = endCombo.getSelectionModel().getSelectedIndex();
        currentTarget = startCombo.getItems().subList(startIdx, endIdx + 1);
        
        /* Java Heap Usage Chart */
        XYChart.Series<String, Long> youngUsage = new XYChart.Series<>();
        XYChart.Series<String, Long> oldUsage = new XYChart.Series<>();
        XYChart.Series<String, Long> free = new XYChart.Series<>();
        youngUsage.setName("Young");
        oldUsage.setName("Old");
        free.setName("Free");
        heapChart.getData().addAll(youngUsage, oldUsage, free);
        
        /* GC time Chart */
        XYChart.Series<String, Long> gcTime = new XYChart.Series<>();
        gcTime.setName("GC TIme");
        gcTimeChart.getData().add(gcTime);
        
        /* Metaspace Chart */
        XYChart.Series<String, Long> metaspaceUsage = new XYChart.Series<>();
        XYChart.Series<String, Long> metaspaceCapacity = new XYChart.Series<>();
        metaspaceUsage.setName("Usage");
        metaspaceCapacity.setName("Capacity");
        metaspaceChart.getData().addAll(metaspaceUsage, metaspaceCapacity);

        snapShotTimeCombo.getItems().addAll(currentTarget);
        SnapShotParseTask task = new SnapShotParseTask(currentTarget);
        task.setOnSucceeded(evt -> {
                                     snapShots = task.getSnapShots();
                                     drawTopNData(snapShots);
                                   });
        super.bindTask(task);
            
        Thread parseThread = new Thread(task);
        parseThread.start();

        currentTarget.stream()
                     .forEachOrdered(d -> {
                                             String time = converter.toString(d.getSnapShotDate());
                                      
                                             addChartDataLong(youngUsage, time, d.getNewHeap() / 1024 / 1024, "MB");
                                             addChartDataLong(oldUsage, time, d.getOldHeap() / 1024 / 1024, "MB");
                                             addChartDataLong(free, time, (d.getTotalCapacity() - d.getNewHeap() - d.getOldHeap()) / 1024 / 1024, "MB");
                                      
                                             addChartDataLong(gcTime, time, d.getGcTime(), "ms");
                                      
                                             addChartDataLong(metaspaceUsage, time, d.getMetaspaceUsage() / 1024 / 1024, "MB");
                                             addChartDataLong(metaspaceCapacity, time, d.getMetaspaceCapacity() / 1024 / 1024, "MB");
                                          });
        
        summaryTable.getItems().addAll((new SummaryData(currentTarget)).getSummaryAsList());
    }
    
    /**
     * Event handler of changing search TextField.
     * 
     * @param event KeyEvent of this event.
     */
    @FXML
    private void onSearchTextChanged(KeyEvent event){
        searchList.getItems().clear();
        searchList.getItems().addAll(lastDiffTable.getItems().parallelStream()
                                                             .map(d -> d.getClassName())
                                                             .filter(s -> s.contains(searchText.getText()))
                                                             .collect(Collectors.toList()));
    }

    /**
     * Event handler of SnapShot TIme.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSnapShotTimeSelected(ActionEvent event){
        ObservableList<Map.Entry<String, String>> summaryList = snapShotSummaryTable.getItems();
        summaryList.clear();
        usagePieChart.getData().clear();
        objDataTable.getItems().clear();
        SnapShotHeader header = snapShotTimeCombo.getSelectionModel().getSelectedItem();
        
        summaryList.addAll(
                new AbstractMap.SimpleEntry<>("Date", (new LocalDateTimeConverter()).toString(header.getSnapShotDate())),
                new AbstractMap.SimpleEntry<>("Entries", Long.toString(header.getNumEntries())),
                new AbstractMap.SimpleEntry<>("Total Java heap usage", String.format("%.02f MB", (double)(header.getNewHeap() + header.getOldHeap()) / 1024.0d /1024.0d)),
                new AbstractMap.SimpleEntry<>("Total Metaspace usage", String.format("%.02f MB", (double)(header.getMetaspaceUsage()) / 1024.0d /1024.0d)),
                new AbstractMap.SimpleEntry<>("SnapShot cause", header.getCauseString()),
                new AbstractMap.SimpleEntry<>("GC cause", header.getGcCause()));
        
        usagePieChart.getData().addAll(topNList.get(header.getSnapShotDate()).stream()
                                                                             .map(o -> new PieChart.Data(o.getName(), o.getTotalSize()))
                                                                             .collect(Collectors.toList()));
        
        objDataTable.getItems().addAll(snapShots.get(header).values().stream().collect(Collectors.toList()));
    }
    
    /**
     * Drawing and Showing table with beging selected value.
     * 
     * @param predicate This value is used as filter in SnapShot.
     */
    private void applyFilter(Predicate<? super ObjectData> predicate){
        Map<SnapShotHeader, Map<Long, ObjectData>> target = new HashMap<>();

        snapShots.forEach((h, m) -> {
                                       Map<Long, ObjectData> objectList = new ConcurrentHashMap<>();
                                       
                                       m.values().parallelStream()
                                                 .filter(predicate)
                                                 .forEach(o -> objectList.put(o.getTag(), o));
                                       
                                       target.put(h, objectList);
                                    });
        
        drawTopNData(target);
    }
    
    /**
     * Selection method for incremental search.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSelectFilterApply(ActionEvent event){
        HashSet<String> targetSet = new HashSet<>(searchList.getSelectionModel().getSelectedItems());
        applyFilter(o -> targetSet.contains(o.getName()));
    }
    
    /**
     * Event handler of clear button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onSelectFilterClear(ActionEvent event){
        drawTopNData(snapShots);
    }
    
    /**
     * Event handler for adding exclude filter.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onAddClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Filter files");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new ExtensionFilter("Filter file (*.xml)", "*.xml"),
                                            new ExtensionFilter("All files", "*.*"));
        
        List<File> excludeFilterList = dialog.showOpenMultipleDialog(((Node)event.getSource()).getScene().getWindow());
        
        excludeFilterList.stream()
                         .forEach(f -> {
                                          Filters filters = (Filters)JAXB.unmarshal(f, Filters.class);

                                          if(filters != null){
                                            excludeTable.getItems().addAll(filters.getFilter());
                                          }
                                             
                                       }); 
            
    }
    
    /**
     * Event handler of apply button in exclude filter.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onHiddenFilterApply(ActionEvent event){
        List<String> hideRegexList = excludeTable.getItems().stream()
                                                 .filter(f -> f.isHide())
                                                 .flatMap(f -> f.getClasses().getName().stream())
                                                 .map(s -> ".*" + s + ".*")
                                                 .collect(Collectors.toList());
        applyFilter(o -> hideRegexList.stream().noneMatch(s -> o.getName().matches(s)));
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
        return snapShots.get(snapShotTimeCombo.getSelectionModel().getSelectedItem());
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
    
}
