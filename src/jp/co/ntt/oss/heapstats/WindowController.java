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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import jp.co.ntt.oss.heapstats.plugin.PluginClassLoader;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.LogController;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.SnapShotController;
import jp.co.ntt.oss.heapstats.utils.DialogHelper;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;

/**
 * Main window controller.
 * 
 * @author Yasumasa Suenaga
 */
public class WindowController implements Initializable {
    
    private static final Map<String, PluginController> pluginList;
        
    private Region veil;
    
    private ProgressIndicator progress;
    
    private PluginClassLoader pluginClassLoader;
    
    private Window owner;
    
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
        DialogHelper rankDialog = new DialogHelper("/jp/co/ntt/oss/heapstats/rankDialog.fxml", "Rank Level setting");
        rankDialog.show();
    }

    @FXML
    private void onAboutMenuClick(ActionEvent event){
        DialogHelper aboutDialog = new DialogHelper("/jp/co/ntt/oss/heapstats/aboutDialog.fxml", "about HeapStatsFXAnalyzer");
        aboutDialog.show();
    }

    private void addPlugin(String packageName){
        String lastPackageName = packageName.substring(packageName.lastIndexOf('.') + 1);
        packageName = packageName.replace('.', '/');
        String fxmlName = packageName + "/" + lastPackageName + ".fxml";
        FXMLLoader loader = new FXMLLoader(pluginClassLoader.getResource(fxmlName));
        Parent root;
        
        try {
            root = loader.load();
        }
        catch (IOException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        PluginController controller = (PluginController)loader.getController();
        controller.setOwner(owner);
        controller.setVeil(veil);
        controller.setProgress(progress);

        Tab tab = new Tab();
        tab.setText(controller.getPluginName());
        tab.setContent(root);
        tab.setOnSelectionChanged(controller.getOnPluginTabSelected());
        
        tabPane.getTabs().add(tab);
        
        pluginList.put(controller.getPluginName(), controller);
    }
    
    @FXML
    private void onGCAllClick(ActionEvent event) {
        SnapShotController snapShotController = (SnapShotController)WindowController.getPluginController("SnapShot Data");
        snapShotController.dumpGCStatisticsToCSV(false);
    }

    @FXML
    private void onGCSelectedClick(ActionEvent event) {
        SnapShotController snapShotController = (SnapShotController)WindowController.getPluginController("SnapShot Data");
        snapShotController.dumpGCStatisticsToCSV(true);
    }

    @FXML
    private void onHeapAllClick(ActionEvent event) {
        SnapShotController snapShotController = (SnapShotController)WindowController.getPluginController("SnapShot Data");
        snapShotController.dumpClassHistogramToCSV(false);
    }

    @FXML
    private void onHeapSelectedClick(ActionEvent event) {
        SnapShotController snapShotController = (SnapShotController)WindowController.getPluginController("SnapShot Data");
        snapShotController.dumpClassHistogramToCSV(true);
    }

    @FXML
    private void onSnapShotOpenClick(ActionEvent event) {
        Tab snapShotTab = tabPane.getTabs().stream()
                                 .filter(t -> t.getText().equals("SnapShot Data"))
                                 .findAny()
                                 .orElseThrow(() -> new IllegalStateException("SnapShot plugin must be loaded."));
        tabPane.getSelectionModel().select(snapShotTab);
        SnapShotController snapShotController = (SnapShotController)WindowController.getPluginController("SnapShot Data");
        
        snapShotController.onSnapshotFileClick(event);
    }

    @FXML
    private void onLogOpenClick(ActionEvent event) {
        Tab snapShotTab = tabPane.getTabs().stream()
                                 .filter(t -> t.getText().equals("Log Data"))
                                 .findAny()
                                 .orElseThrow(() -> new IllegalStateException("Log plugin must be loaded."));
        tabPane.getSelectionModel().select(snapShotTab);
        LogController logController = (LogController)WindowController.getPluginController("Log Data");
        
        logController.onLogFileClick(event);
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
    }

    /**
     * Load plugins which is defined in heapstats.properties.
     */
    public void loadPlugin(){
        String resourceName = "/" + this.getClass().getName().replace('.', '/') + ".class";
        String appJarString = this.getClass().getResource(resourceName).getPath();
        appJarString = appJarString.substring(0, appJarString.indexOf('!')).replaceFirst("file:", "");
        
        Path appJarPath;
        
        try{
            appJarPath = FileSystems.getDefault().getPath(appJarString);
        }
        catch(InvalidPathException e){
            if((appJarString.charAt(0) == '/') && (appJarString.length() > 2)){ // for Windows
                appJarPath = FileSystems.getDefault().getPath(appJarString.substring(1));
            }
            else{
                throw e;
            }
        }
        
        Path libPath = appJarPath.getParent().resolve("lib");
        List<URL> jarURLList = new ArrayList<>();
        
        try(DirectoryStream<Path> jarPaths = Files.newDirectoryStream(libPath, "*.jar")){
            jarPaths.forEach(p -> {
                                     try{
                                       jarURLList.add(p.toUri().toURL());
                                     }
                                     catch (MalformedURLException ex) {
                                       Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
                                     }
                                  });
        }
        catch(IOException ex) {
            Logger.getLogger(WindowController.class.getName()).log(Level.SEVERE, null, ex);
        }

        pluginClassLoader = new PluginClassLoader(jarURLList.toArray(new URL[0]));
        FXMLLoader.setDefaultClassLoader(pluginClassLoader);
            
        List<String> plugins = HeapStatsUtils.getPlugins();
        plugins.stream().forEach(s -> addPlugin(s));
    }

    /**
     * Get controller instance of plugin.
     * 
     * @param pluginName Plugin name which you want.
     * @return Controller of Plugin. If it does not exist, return null.
     */
    public static PluginController getPluginController(String pluginName){
        return pluginList.get(pluginName);
    }

    /**
     * Get loaded plugin list.
     * 
     * @return Loaded plugin list.
     */
    public static Map<String, PluginController> getPluginList() {
        return pluginList;
    }

    public Window getOwner() {
        return owner;
    }

    public void setOwner(Window owner) {
        this.owner = owner;
    }

}
