/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

/**
 * FXML Controller class
 *
 * @author Yasu
 */
public class ThreadRecorderController extends PluginController implements Initializable {
    
    @FXML
    private Button openBtn;
    
    @FXML
    private TextField fileNameBox;
    
    @FXML
    private ComboBox<ThreadStat> startCombo;
    
    @FXML
    private ComboBox<ThreadStat> endCombo;
    
    @FXML
    private TableView<ThreadShowSwitch> threadListView;
    
    @FXML
    private TableColumn<ThreadShowSwitch, Boolean> showColumn;
    
    @FXML
    private TableColumn<ThreadShowSwitch, String> threadNameColumn;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        startCombo.setConverter(new ThreadStatConverter());
        endCombo.setConverter(new ThreadStatConverter());
        
        showColumn.setCellValueFactory(new PropertyValueFactory<>("show"));
        showColumn.setCellFactory(CheckBoxTableCell.forTableColumn(showColumn));
        threadNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    }
    
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
                                         
                                         ObservableList<ThreadStat> list = FXCollections.observableArrayList(parser.getThreadStatList());
                                         startCombo.setItems(list);
                                         endCombo.setItems(list);
                                         startCombo.getSelectionModel().selectFirst();
                                         endCombo.getSelectionModel().selectLast();
                                         
                                         Map<Long, String> idMap = parser.getIdMap();
                                         threadListView.setItems(FXCollections.observableArrayList(idMap.keySet().stream()
                                                                                                                 .sorted()
                                                                                                                 .map(k -> new ThreadShowSwitch(k, idMap.get(k)))
                                                                                                                 .collect(Collectors.toList())));
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
    
}
