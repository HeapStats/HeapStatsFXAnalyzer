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

import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import jp.co.ntt.oss.heapstats.plugin.PluginController;
import jp.co.ntt.oss.heapstats.utils.DialogBaseController;

/**
 * FXML Controller of About dialog.
 *
 * @author Yasumasa Suenaga
 */
public class AboutDialogController extends DialogBaseController implements Initializable {
    
    @FXML
    private Accordion accordion;
    
    @FXML
    private TitledPane pluginPane;
    
    @FXML
    private TableView<Map.Entry<String, String>> pluginTable;
    
    @FXML
    private TableColumn<Map.Entry<String, String>, String> pluinTableNameColumn;

    @FXML
    private TableColumn<Map.Entry<String, String>, String> pluginTableLicenseColumn;
    
    @FXML
    private TableView<PluginController.LibraryLicense> libraryTable;
    
    @FXML
    private TableColumn<PluginController.LibraryLicense, String> libraryTablePluginColumn;

    @FXML
    private TableColumn<PluginController.LibraryLicense, String> libraryTableLibraryColumn;

    @FXML
    private TableColumn<PluginController.LibraryLicense, String> libraryTableLicenseColumn;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pluinTableNameColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        pluginTableLicenseColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        libraryTablePluginColumn.setCellValueFactory(new PropertyValueFactory<>("pluginName"));
        libraryTableLibraryColumn.setCellValueFactory(new PropertyValueFactory<>("libraryName"));
        libraryTableLicenseColumn.setCellValueFactory(new PropertyValueFactory<>("license"));
        
        accordion.setExpandedPane(pluginPane);

        /*
         * Set plugin info to pluginTable
         * Map.Entry which is implemented in HashMap, Hashtable does not work in TableView.
         * Thus I create array of AbstractMap.SimpleEntry .
         */
        List<AbstractMap.SimpleEntry<String, String>> plugins = new ArrayList<>();
        WindowController.getPluginList().forEach((k, v) -> plugins.add(new AbstractMap.SimpleEntry<>(k, v.getLicense())));
        pluginTable.getItems().addAll(plugins);
        
        /* Set library license to libraryTable */
        List<PluginController.LibraryLicense> libraryList = new ArrayList<>();
        WindowController.getPluginList().forEach((n, c) -> {
                                                              if(c.getLibraryLicense() != null){
                                                                c.getLibraryLicense().forEach((k, v) -> libraryList.add(new PluginController.LibraryLicense(n, k, v)));
                                                              }
                                                           });
        libraryTable.getItems().addAll(libraryList);
    }    
  
    /**
     * Event handler when user clickes "OK" button.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOKClick(ActionEvent event){
        super.close();
    }

}
