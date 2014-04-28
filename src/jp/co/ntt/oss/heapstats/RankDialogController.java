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
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;

/**
 * This class is controller class of Rank dialog.
 * This class shows dialog for setting rank value.
 *
 * @author Yasumasa Suenaga
 */
public class RankDialogController implements Initializable {
    
    private Stage stage;
    
    @FXML
    private TextField rankText;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rankText.setText(Integer.toString(HeapStatsUtils.getRankLevel()));
    }    

    /**
     * Event handler when user clickes "OK" button.
     * This handler sets new rank value to system property.
     * 
     * @param event ActionEvent of this event.
     */
    @FXML
    private void onOKClick(ActionEvent event){
        HeapStatsUtils.setRankLevel(Integer.parseInt(rankText.getText()));
        stage.close();
    }

    /**
     * Setter method for Stage.
     * This value is used to set parent window for Ranking dialog.
     * 
     * @param stage Instance of main Stage.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

}
