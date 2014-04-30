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

package jp.co.ntt.oss.heapstats.plugin;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * Base class for HeapStats FX Analyzer plugin.
 * 
 * @author Yasumasa Suenaga
 */
public abstract class PluginController implements Initializable{

    private Region veil;
    
    private ProgressIndicator progress;
    
    public abstract String getPluginName();
    
    public abstract EventHandler<Event> getOnPluginTabSelected();

    /**
     * Setter of veil region.
     * This region is used for veiling (e.g. showing progress)
     * 
     * @param veil 
     */
    public void setVeil(Region veil){
        this.veil = veil;
    }
    
    /**
     * Setter of progress indicator.
     * This region is used for veiling (e.g. showing progress)
     * 
     * @param progress
     */
    public void setProgress(ProgressIndicator progress){
        this.progress = progress;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
    
    /**
     * Task binder.
     * This method binds veil and progress indicator to task.
     * 
     * @param task Task to be binded.
     */
    public void bindTask(Task task){
        veil.visibleProperty().bind(task.runningProperty());
        progress.visibleProperty().bind(task.runningProperty());
        progress.progressProperty().bind(task.progressProperty());
    }
 
    /**
     * Comvenient method of chart.
     * This method addes value to chart as percent and add Tooltip.
     * 
     * @param series Series to be added.
     * @param xData X value.
     * @param yData Y value. This value must be percentage.
     */
    protected void addChartDataAsPercent(XYChart.Series<String, Double> series, String xData, Double yData){
        XYChart.Data<String, Double> data = new XYChart.Data<>(xData, yData);
        series.getData().add(data);

        String tip = String.format("%s: %s, %.02f %%", series.getName(), xData, yData);
        Tooltip.install(data.getNode(), new Tooltip(tip));
    }
    
    /**
     * Comvenient method of chart.
     * This method addes value to chart as long value and add Tooltip.
     * 
     * @param series Series to be added.
     * @param xData X value.
     * @param yData Y value.
     */
    protected void addChartDataLong(XYChart.Series<String, Long> series, String xData, Long yData, String unit){
        XYChart.Data<String, Long> data = new XYChart.Data<>(xData, yData);
        series.getData().add(data);

        String tip = String.format("%s: %s, %d %s", series.getName(), xData, yData, unit);
        Tooltip.install(data.getNode(), new Tooltip(tip));
    }

}
