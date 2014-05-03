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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                                                               String stackTrace;
                                                               
                                                               try(StringWriter strWriter = new StringWriter();
                                                                   PrintWriter printWriter = new PrintWriter(strWriter);){
                                                                 e.printStackTrace(printWriter);
                                                                 stackTrace = strWriter.toString();
                                                               }
                                                               catch(IOException ioe){
                                                                   throw new UncheckedIOException(ioe);
                                                               }
                                                                   
                                                               Platform.runLater(() -> (new InfoDialog("Error", e.toString(), stackTrace)).show());
                                                            });
        HeapStatsUtils.load();
        FXMLLoader mainWindowLoader = new FXMLLoader(getClass().getResource("window.fxml"));
        
        Parent root = mainWindowLoader.load();
        Scene scene = new Scene(root);
        
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
