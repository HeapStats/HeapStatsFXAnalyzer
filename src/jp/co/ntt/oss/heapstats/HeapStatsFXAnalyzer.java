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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;
import jp.co.ntt.oss.heapstats.utils.InfoDialog;

/**
 * Main class of HeapStats FX Analyzer.
 * This class provides entry point of HeapStats FX Analyzer.
 * 
 * @author Yasumasa Suenaga
 */
public class HeapStatsFXAnalyzer extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Platform.runLater(() -> (new InfoDialog("Error", e.getLocalizedMessage(), HeapStatsUtils.stackTarceToString(e))).show()));
        HeapStatsUtils.load();
        FXMLLoader mainWindowLoader = new FXMLLoader(getClass().getResource("window.fxml"));
        
        Parent root = mainWindowLoader.load();
        Scene scene = new Scene(root);
        WindowController controller = (WindowController)mainWindowLoader.getController();
        controller.setOwner(stage);
        
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Main method of HeapStats analyzer.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
