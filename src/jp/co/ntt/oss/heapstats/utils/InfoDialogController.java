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

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * FXML Controller class for Infomation Dialog.
 *
 * @author Yasumasa Suenaga
 */
public class InfoDialogController extends DialogBaseController implements Initializable {
    
    @FXML
    private AnchorPane mainAnchor;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private Button okButton;
    
    @FXML
    private Accordion detailsAccordion;
    
    @FXML
    private TitledPane detailsPane;
    
    @FXML
    private TextArea detailsTextArea;
    
    private double titleBarHeight;
    
    private double minimumAccordionHeight;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
    
    /**
     * Event handler when user clickes "Details" accordion.
     * 
     * @param event MouseEvent of this event.
     */
    @FXML
    private void onDetailsPaneClicked(MouseEvent event){
        double baseHeight = detailsAccordion.getLayoutY() + titleBarHeight;
        double accordionHeight = detailsPane.isExpanded() ? 200.0d : minimumAccordionHeight;
        super.getStage().setHeight(baseHeight + accordionHeight);
    }
    
    /**
     * Setter of message which is shown in dialog.
     * 
     * @param message 
     */
    public void setMessage(String message){
        messageLabel.setText(message);
    }
    
    /**
     * Setter of detail message which is shown in dialog.
     * If details is null, visible property of Accordion compornent is set to
     * false.
     * 
     * @param details 
     */
    public void setDetails(String details){
        
        if(details == null){
            detailsAccordion.setVisible(false);
        }
        else{
            detailsTextArea.setText(details);
        }
        
    }
    
    /**
     * Re-layout dialogs.
     * This function calculates Y-position on each components.
     * Y-position is based on message label height.
     */
    public void layout(){
        okButton.setLayoutY(messageLabel.getLayoutY() + messageLabel.getHeight() + 10.0d);
        detailsAccordion.setLayoutY(okButton.getLayoutY() + okButton.getHeight() + 10.0d);
        
        Stage stage = super.getStage();
        titleBarHeight = stage.getHeight() - mainAnchor.getHeight();
        minimumAccordionHeight = detailsAccordion.getHeight();
        stage.setHeight(detailsAccordion.getLayoutY() + minimumAccordionHeight + titleBarHeight);
    }

}