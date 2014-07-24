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

package jp.co.ntt.oss.heapstats.utils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * Helper class for Dialog.
 * This class can control showing and closing dialog.
 * 
 * @author Yasumasa Suenaga
 */
public class DialogHelper {
    
    private final String title;
    
    private final FXMLLoader loader;
    
    private Stage dialog;
    
    private DialogBaseController controller;
    
    private EventHandler<WindowEvent> onShown;

    /**
     * Constructor of DialogHelper.
     * 
     * @param fxmlName FXML name that want to control. It must be full path of resource.
     *                  This value is used through ClassLoader#getResource() .
     * @param title Title of this dialog.
     */
    public DialogHelper(String fxmlName, String title){
        this.title = title;
        this.dialog = null;
        this.onShown = null;

        loader = new FXMLLoader(getClass().getResource(fxmlName), HeapStatsUtils.getResourceBundle());
        
        try {
            loader.load();
            controller = loader.getController();
        }
        catch (IOException ex) {
            Logger.getLogger(DialogHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * Show dialog.
     * Dialog will be created with fxmlName field.
     */
    public void show(){
        Parent root = loader.getRoot();        
        Scene scene = new Scene(root);
        
        dialog = new Stage(StageStyle.UTILITY);
        controller.setStage(dialog);
        
        if(onShown != null){
            dialog.setOnShown(onShown);
        }
        
        dialog.setScene(scene);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);
        dialog.setTitle(title);
        dialog.showAndWait();
    }
    
    /**
     * Close this dialog.
     */
    public void close(){
        dialog.close();
    }

    /**
     * Get Stage instance of this dialog.
     * 
     * @return Stage of this dialog.
     */
    public Stage getDialog() {
        return dialog;
    }

    /**
     * Get Controller class of this dialog.
     * 
     * @return Controller of dialog.
     */
    public DialogBaseController getController() {
        return controller;
    }
    
    /**
     * Set onShown event to this stage.
     * 
     * @param value onShown event handler.
     */
    public void setOnShown(EventHandler<WindowEvent> value){
        onShown = value;
    }
    
}
