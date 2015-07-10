/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import jp.co.ntt.oss.heapstats.WindowController;
import jp.co.ntt.oss.heapstats.container.threadrecord.ThreadStat;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.task.ThreadRecordParseTask;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.TaskAdapter;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;

/**
 * FXML Controller class
 *
 * @author Yasu
 */
public class ThreadRecorderController extends PluginController implements Initializable {

    private static final int TIMELINE_PADDING = 8;

    @FXML
    private Button openBtn;

    @FXML
    private TextField fileNameBox;

    @FXML
    private Label startTimeLabel;

    @FXML
    private Label endTimeLabel;

    @FXML
    private TableView<ThreadStatViewModel> threadListView;

    @FXML
    private TableColumn<ThreadStatViewModel, Boolean> showColumn;

    @FXML
    private TableColumn<ThreadStatViewModel, String> threadNameColumn;

    @FXML
    private TableView<ThreadStatViewModel> timelineView;

    @FXML
    private TableColumn<ThreadStatViewModel, List<ThreadStat>> timelineColumn;
    
    @FXML
    private SplitPane rangePane;
    
    @FXML
    private Button okBtn;
    
    private boolean boundScrollBar = false;
    
    private List<ThreadStat> threadStatList;
    
    private Map<Long, String> idMap;
    
    private ObjectProperty<LocalDateTime> rangeStart;
    
    private ObjectProperty<LocalDateTime> rangeEnd;

    /**
     * Update caption of label which represents time of selection.
     * 
     * @param target Label compornent to draw.
     * @param newValue Percentage of timeline. This value is between 0.0 and 1.0 .
     */
    private void updateRangeLabel(Label target, double newValue){
        if((threadStatList != null) && !threadStatList.isEmpty()){
            LocalDateTime start = threadStatList.get(0).getTime();
            LocalDateTime end = threadStatList.get(threadStatList.size() - 1).getTime();
            long diff = start.until(end, ChronoUnit.MILLIS);
            LocalDateTime newTime = start.plus((long)((double)diff * newValue), ChronoUnit.MILLIS);
            
            if(target == startTimeLabel){
                rangeStart.set(newTime);
            }
            else{
                rangeEnd.set(newTime);
            }
            
            LocalDateTimeConverter converter = new LocalDateTimeConverter();
            target.setText(converter.toString(newTime));
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        threadStatList = null;
        rangeStart = new SimpleObjectProperty<>();
        rangeEnd = new SimpleObjectProperty<>();
        
        rangePane.getDividers().get(0).positionProperty().addListener((b, o, n) -> updateRangeLabel(startTimeLabel, n.doubleValue()));
        rangePane.getDividers().get(1).positionProperty().addListener((b, o, n) -> updateRangeLabel(endTimeLabel, n.doubleValue()));

        showColumn.setCellValueFactory(new PropertyValueFactory<>("show"));
        showColumn.setCellFactory(CheckBoxTableCell.forTableColumn(showColumn));
        threadNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        timelineColumn.setCellValueFactory(new PropertyValueFactory<>("threadStats"));
        timelineColumn.setCellFactory(param -> new TimelineCell(rangeStart, rangeEnd));
        timelineColumn.prefWidthProperty().bind(timelineView.widthProperty().subtract(TIMELINE_PADDING));
        rangePane.getItems().forEach(n -> SplitPane.setResizableWithParent(n, false));
        timelineView.itemsProperty().bind(threadListView.itemsProperty());
    }
    
    /**
     * Event handler for open button.
     * @param event 
     */
    @FXML
    private void onOpenBtnClick(ActionEvent event){
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Choose HeapStats Thread Recorder file");
        dialog.setInitialDirectory(new File(HeapStatsUtils.getDefaultDirectory()));
        dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Thread Recorder file (*.htr)", "*.htr"),
                                            new FileChooser.ExtensionFilter("All files", "*.*"));
        File recorderFile = dialog.showOpenDialog(WindowController.getInstance().getOwner());
        
        if(recorderFile != null){
            HeapStatsUtils.setDefaultDirectory(recorderFile.getParent());
            fileNameBox.setText(recorderFile.getAbsolutePath());
            
            TaskAdapter<ThreadRecordParseTask> task = new TaskAdapter<>(new ThreadRecordParseTask(recorderFile));
            super.bindTask(task);
            task.setOnSucceeded(evt -> {
                ThreadRecordParseTask parser = task.getTask();
                idMap = parser.getIdMap();
                threadStatList = parser.getThreadStatList();
                rangePane.getDividers().get(0).setPosition(0.0d);
                rangePane.getDividers().get(1).setPosition(1.0d);
                
                rangePane.setDisable(false);
                okBtn.setDisable(false);
                
                // FIX ME! Can we redraw more lightly?
                timelineColumn.prefWidthProperty().addListener((b, o, n) -> onOkBtnClick(null));
            });
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
    }

    /**
     * Event handler for OK button.
     * @param event 
     */
    @FXML
    private void onOkBtnClick(ActionEvent event){
        
        if (boundScrollBar != true) {
            bindScroll();
            boundScrollBar = true;
        }
        
        Map<Long, List<ThreadStat>> statById = threadStatList.stream()
                                                             .filter(s -> s.getTime().isAfter(rangeStart.get()) && s.getTime().isBefore(rangeEnd.get()))
                                                             .collect(Collectors.groupingBy(ThreadStat::getId));
        ObservableList<ThreadStatViewModel> threadStats = FXCollections.observableArrayList(idMap.keySet().stream()
                                .sorted()
                                .map(k -> new ThreadStatViewModel(k, idMap.get(k), rangeStart.get(), rangeEnd.get(), statById.get(k)))
                                .collect(Collectors.toList()));
        threadListView.setItems(threadStats);
    }
    
    @Override
    public String getPluginName() {
        return "Thread Recorder";
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
    public EventHandler<Event> getOnPluginTabSelected() {
        return null;
    }

    @Override
    public Runnable getOnCloseRequest() {
        return null;
    }

    public TableView<ThreadStatViewModel> getTimelineView() {
        return timelineView;
    }

    private void bindScroll() {
        ScrollBar leftScrollBar = getVerticalScrollBar(threadListView);
        ScrollBar rightScrollBar = getVerticalScrollBar(timelineView);
        leftScrollBar.valueProperty().bindBidirectional(rightScrollBar.valueProperty());
    }

    private ScrollBar getVerticalScrollBar(TableView<?> tableView) {
        Set<Node> nodes = tableView.lookupAll(".scroll-bar");
        for (Node node : nodes) {
            if (node instanceof ScrollBar) {
                ScrollBar scrollBar = (ScrollBar) node;
                if (scrollBar.getOrientation() == Orientation.VERTICAL) {
                    return scrollBar;
                }
            }
        }
        throw new IllegalStateException("Not found ScrollBar.");
    }

    private void updateTime(LocalDateTime time, boolean start) {
        if (timelineView.getItems().size() > 0) {
            List<ThreadStatViewModel> updatedModels = timelineView.getItems().stream()
                    .map(model -> {
                        if (start) {
                            model.setStartTime(time);
                        } else {
                            model.setEndTime(time);
                        }
                        return model;
                    }).collect(Collectors.toList());
            ObservableList<ThreadStatViewModel> newViewModels = FXCollections.observableArrayList(updatedModels);
            threadListView.getItems().clear();
            threadListView.setItems(newViewModels);
            timelineView.getItems().clear();
            timelineView.setItems(newViewModels);
        }
    }

}
