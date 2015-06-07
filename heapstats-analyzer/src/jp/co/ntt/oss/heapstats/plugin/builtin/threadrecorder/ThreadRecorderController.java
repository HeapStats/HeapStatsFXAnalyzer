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
import jp.co.ntt.oss.heapstats.utils.ThreadStatConverter;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 * FXML Controller class
 *
 * @author Yasu
 */
public class ThreadRecorderController extends PluginController implements Initializable {

    private static final int TIMELINE_PADDING = 8;

    private static final double DEFAULT_COLUMN_WIDTH = 525.0;

    @FXML
    private Button openBtn;

    @FXML
    private TextField fileNameBox;

    @FXML
    private ComboBox<ThreadStat> startCombo;

    @FXML
    private ComboBox<ThreadStat> endCombo;

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

    private boolean boundScrollBar = false;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        startCombo.setConverter(new ThreadStatConverter());
        startCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateTime(newValue.getTime(), true);
        });
        endCombo.setConverter(new ThreadStatConverter());
        endCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateTime(newValue.getTime(), false);
        });
        
        showColumn.setCellValueFactory(new PropertyValueFactory<>("show"));
        showColumn.setCellFactory(CheckBoxTableCell.forTableColumn(showColumn));
        threadNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        timelineColumn.setCellValueFactory(new PropertyValueFactory<>("threadStats"));
        timelineColumn.setCellFactory(param -> new TimelineCell());
    }
    
    @FXML
    private void onOpenBtnClick(ActionEvent event){
        if (boundScrollBar != true) {
            bindScroll();
            boundScrollBar = true;
        }
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

                ObservableList<ThreadStat> list = FXCollections.observableArrayList(parser.getThreadStatList());
                startCombo.setItems(list);
                endCombo.setItems(list);
                startCombo.getSelectionModel().selectFirst();
                endCombo.getSelectionModel().selectLast();

                Map<Long, List<ThreadStat>> statById = list.stream()
                        .collect(Collectors.groupingBy(ThreadStat::getId));

                Map<Long, String> idMap = parser.getIdMap();
                final LocalDateTime startTime = list.get(0).getTime();
                final LocalDateTime endTime = list.get(list.size() - 1).getTime();
                ObservableList<ThreadStatViewModel> threadStats = FXCollections.observableArrayList(idMap.keySet().stream()
                                .sorted()
                                .map(k -> new ThreadStatViewModel(k, idMap.get(k), startTime, endTime, statById.get(k)))
                                .collect(Collectors.toList()));
                adjustColumnWidth(startTime, endTime);
                threadListView.setItems(threadStats);
                timelineView.setItems(threadStats);
            });
            
            Thread parseThread = new Thread(task);
            parseThread.start();
        }
        
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
            LocalDateTime startTime = newViewModels.get(0).getStartTime();
            LocalDateTime endTime = newViewModels.get(0).getEndTime();
            adjustColumnWidth(startTime, endTime);
        }
    }

    private void adjustColumnWidth(LocalDateTime startTime, LocalDateTime endTime) {
        long timeDiff = startTime.until(endTime, ChronoUnit.MILLIS);
        double newWidth = timeDiff * TimelineCell.LENGTH_PER_MILLS + TIMELINE_PADDING;
        newWidth = (newWidth < DEFAULT_COLUMN_WIDTH) ? DEFAULT_COLUMN_WIDTH : newWidth;
        timelineColumn.setPrefWidth(newWidth);
    }

}
