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

package jp.co.ntt.oss.heapstats;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;

/**
 * Main window controller.
 * 
 * @author Yasumasa Suenaga
 */
public class WindowController implements Initializable {
    
    private Window ownerWindow;
    
    private static final Map<String, PluginController> pluginList;
        
    private Region veil;
    
    private ProgressIndicator progress;
    
    @FXML
    private StackPane stackPane;
    
    @FXML
    private TabPane tabPane;
    
    static{
        pluginList = new ConcurrentHashMap<>();
    }
    
    @FXML
    private void onExitClick(ActionEvent event) {
        Platform.exit();
    }
    
    @FXML
    private void onRankLevelClick(ActionEvent event){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("rankDialog.fxml"));
        
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        Parent root = loader.getRoot();
        
        RankDialogController controller = loader.getController();
        
        Scene scene = new Scene(root);
        Stage dialog = new Stage(StageStyle.UTILITY);
        controller.setStage(dialog);
        dialog.setScene(scene);
        dialog.initOwner(ownerWindow);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);
        dialog.setTitle("Rank Level setting");
        dialog.showAndWait();
    }

    private void addPlugin(String packageName){
        String lastPackageName = packageName.substring(packageName.lastIndexOf('.') + 1);
        packageName = packageName.replace('.', '/');
        String fxmlName = "/" + packageName + "/" + lastPackageName + ".fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
        Parent root;
        
        try {
            root = loader.load();
        }
        catch (IOException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        PluginController controller = (PluginController)loader.getController();
        controller.setVeil(veil);
        controller.setProgress(progress);

        Tab tab = new Tab();
        tab.setText(controller.getPluginName());
        tab.setContent(root);
        tab.setOnSelectionChanged(controller.getOnPluginTabSelected());
        
        tabPane.getTabs().add(tab);
        
        pluginList.put(controller.getPluginName(), controller);
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        veil = new Region();
        veil.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2)");
        veil.setVisible(false);
        
        progress = new ProgressIndicator();
        progress.setMaxSize(200.0d, 200.0d);
        progress.setVisible(false);
        
        stackPane.getChildren().add(veil);
        stackPane.getChildren().add(progress);

        List<String> plugins = HeapStatsUtils.getPlugins();
        plugins.stream().forEach(s -> addPlugin(s));
    }    

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }
    
    public static PluginController getPluginController(String pluginName){
        return pluginList.get(pluginName);
    }

}
