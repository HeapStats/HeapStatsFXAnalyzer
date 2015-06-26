/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import jp.co.ntt.oss.heapstats.container.threadrecord.ThreadStat;

/**
 *
 * @author Yasu
 */
public class TimelineGenerator {
    
    private static final double RECT_HEIGHT = 16;

    private static final String CSS_CLASS_PREFIX = "rect-";

    public static enum ThreadEvent{
        Unused,
        Run,
        MonitorWait,
        MonitorContendedEnter,
        ThreadSleep,
        Park,
        FileWrite,
        FileRead,
        SocketWrite,
        SocketRead
    }

    private final ThreadStatViewModel viewModel;
    
    private final DoubleProperty prefWidth;
    
    private double range;
    
    private double scale;
    
    private TemporalUnit unit;

    public TimelineGenerator(ThreadStatViewModel viewModel, DoubleProperty prefWidth) {
        this.viewModel = viewModel;
        this.prefWidth = prefWidth;
    }
    
    private void decideDrawScale(LocalDateTime start, LocalDateTime end){
        double width = prefWidth.get();
        
        // Try to milli sec
        unit = ChronoUnit.MILLIS;
        range = start.until(end, unit);
        scale = width / (double)range;
        
        // Try to micro sec
        if(scale > 0.0d){
            unit = ChronoUnit.MICROS;
            range = start.until(end, unit);
            scale = width / (double)range;
            
            // Try to nano sec
            if(scale > 0.0d){
                unit = ChronoUnit.NANOS;
                range = start.until(end, unit);
                scale = width / (double)range;
            }
            
        }

    }
    
    public HBox createTimeline(){
        List<ThreadStat> threadStatList = viewModel.threadStatsProperty().get();
        
        if(threadStatList.isEmpty()){
            return new HBox();
        }
        
        LocalDateTime start = threadStatList.get(0).getTime();
        LocalDateTime end = threadStatList.get(threadStatList.size() - 1).getTime();

        return createTimeline(start, end);
    }
    
    public HBox createTimeline(LocalDateTime start, LocalDateTime end){
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.prefWidthProperty().bind(prefWidth);
        
        decideDrawScale(start, end);
        drawTimeline(start, container);
        
        return container;
    }
    
    private ThreadEvent convertToThreadEvent(ThreadStat.ThreadEvent event){
        switch (event) {
            case ThreadStart:
            case MonitorWaited:
            case MonitorContendedEntered:
            case ThreadSleepEnd:
            case Unpark:
            case FileWriteEnd:
            case FileReadEnd:
            case SocketWriteEnd:
            case SocketReadEnd:
                return ThreadEvent.Run;

            case ThreadEnd:
                return ThreadEvent.Unused;

            case MonitorWait:
                return ThreadEvent.MonitorWait;

            case MonitorContendedEnter:
                return ThreadEvent.MonitorContendedEnter;

            case ThreadSleepStart:
                return ThreadEvent.ThreadSleep;

            case Park:
                return ThreadEvent.Park;

            case FileWriteStart:
                return ThreadEvent.FileWrite;

            case FileReadStart:
                return ThreadEvent.FileRead;

            case SocketWriteStart:
                return ThreadEvent.SocketWrite;

            case SocketReadStart:
                return ThreadEvent.SocketRead;
                
            default:
                return ThreadEvent.Unused;
        }
    }
    
    private void drawTimeline(LocalDateTime start, HBox container) {
        List<Rectangle> rects = new ArrayList<>();
        int startIndex;
        LocalDateTime prevTime;
        ThreadEvent prevEvent;
        long prevAdditionalData;
        List<ThreadStat> threadStatList = viewModel.threadStatsProperty().get();
        
        if(threadStatList.isEmpty()){
            return;
        }
        else{
            ThreadStat threadStat = threadStatList.get(0);
            prevTime = threadStat.getTime();

            if(start.equals(prevTime)){
                startIndex = 1;
                prevTime = threadStat.getTime();
                prevEvent = convertToThreadEvent(threadStat.getEvent());
                prevAdditionalData = threadStat.getAdditionalData();
            }
            else{
                startIndex = 0;
                prevTime = start;
                prevEvent = ThreadEvent.Unused;
                prevAdditionalData = 0;
            }
        
        }
        
        for(int Cnt = startIndex; Cnt < threadStatList.size(); Cnt++){
            ThreadStat threadStat = threadStatList.get(Cnt);
            
            rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
            
            prevTime = threadStat.getTime();
            prevAdditionalData = threadStat.getAdditionalData();
            prevEvent = convertToThreadEvent(threadStat.getEvent());
        }
        
        container.getChildren().addAll(rects);
    }
        
    private Rectangle createThreadRect(LocalDateTime startTime, LocalDateTime endTime, ThreadEvent event, long additionalData) {
        long timeDiff = startTime.until(endTime, unit);        
        double width = scale * timeDiff;
        Rectangle rectangle = new Rectangle(width, RECT_HEIGHT);
        String styleClass = CSS_CLASS_PREFIX + event.name().toLowerCase();
        rectangle.getStyleClass().add(styleClass);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
        String caption = startTime.format(formatter) + " - " + endTime.format(formatter) + ": " + event.toString();
        switch(event){
            case MonitorWait:
            case ThreadSleep:
            case Park:
                if(additionalData > 0){
                    caption += " (" + Long.toString(additionalData) + " ms)";
                }
                break;
                
            case FileWrite:
            case FileRead:
            case SocketWrite:
            case SocketRead:
                caption += " (" + Long.toString(additionalData) + " bytes)";
                break;
        }
        
        if((event != ThreadEvent.Unused) && (width > 0.0d)){
            Tooltip.install(rectangle, new Tooltip(caption));
        }
        
        return rectangle;
    }
    
}
