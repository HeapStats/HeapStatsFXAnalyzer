package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import jp.co.ntt.oss.heapstats.container.threadrecord.ThreadStat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Tooltip;

/**
 * Table cell for thread timeline.
 */
public class TimelineCell extends TableCell<ThreadStatViewModel, List<ThreadStat>> {
    
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

    private static final double RECT_HEIGHT = 16;

    private static final String CSS_CLASS_PREFIX = "rect-";

    private final HBox container;
    
    private final ThreadRecorderController controller;
    
    private double scale;
    
    private TemporalUnit unit;

    public TimelineCell(ThreadRecorderController controller) {
        this.controller = controller;
        container = new HBox(0);
        container.setAlignment(Pos.CENTER_LEFT);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER_LEFT);

        long range;
        double windowWidth = controller.getTimelineView().getWidth();
        LocalDateTime rangeStart = controller.getRangeStart();
        LocalDateTime rangeEnd = controller.getRangeEnd().plusSeconds(1); // Add 1 sec to end time because we want to draw timeline until end time.
        unit = ChronoUnit.MILLIS;

        // Try to milli sec
        range = rangeStart.until(rangeEnd, unit);
        scale = windowWidth / (double)range;
        
        // Try to micro sec
        if(scale > 0.0d){
            unit = ChronoUnit.MICROS;
            range = rangeStart.until(rangeEnd, unit);
            scale = windowWidth / (double)range;
            
            // Try to nano sec
            if(scale > 0.0d){
                unit = ChronoUnit.NANOS;
                range = rangeStart.until(rangeEnd, unit);
                scale = windowWidth / (double)range;
            }
            
        }

    }

    @Override
    protected void updateItem(List<ThreadStat> item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
            updateToEmptyCell();
        } else {
            drawTimeline((ThreadStatViewModel)getTableRow().getItem());
        }
    }

    private void drawTimeline(ThreadStatViewModel viewModel) {
        container.getChildren().clear();

        LocalDateTime prevTime = viewModel.getStartTime();
        ThreadEvent prevEvent = ThreadEvent.Unused;
        long prevAdditionalData = 0;
        List<Rectangle> rects = new ArrayList<>();
        for (ThreadStat threadStat : viewModel.threadStatsProperty().get()) {
            
            switch(threadStat.getEvent()){
                
                case ThreadStart:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.Unused, 0));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case ThreadEnd:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    break;
                    
                case MonitorWait:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.MonitorWait;
                    prevAdditionalData = threadStat.getAdditionalData();
                    break;
                    
                case MonitorWaited:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.MonitorWait, prevAdditionalData));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case MonitorContendedEnter:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.MonitorContendedEnter;
                    break;
                    
                case MonitorContendedEntered:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.MonitorContendedEnter, prevAdditionalData));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case ThreadSleepStart:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.ThreadSleep;
                    break;
                    
                case ThreadSleepEnd:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.ThreadSleep, threadStat.getAdditionalData()));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case Park:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.Park;
                    prevAdditionalData = threadStat.getAdditionalData();
                    break;
                    
                case Unpark:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.Park, prevAdditionalData));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case FileWriteStart:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.FileWrite;
                    break;
                    
                case FileWriteEnd:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.FileWrite, threadStat.getAdditionalData()));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case FileReadStart:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.FileRead;
                    break;
                    
                case FileReadEnd:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.FileRead, threadStat.getAdditionalData()));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case SocketWriteStart:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.SocketWrite;
                    break;
                    
                case SocketWriteEnd:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.SocketWrite, threadStat.getAdditionalData()));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
                case SocketReadStart:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), prevEvent, prevAdditionalData));
                    prevEvent = ThreadEvent.SocketRead;
                    break;
                    
                case SocketReadEnd:
                    rects.add(createThreadRect(prevTime, threadStat.getTime(), ThreadEvent.SocketRead, threadStat.getAdditionalData()));
                    prevEvent = ThreadEvent.Run;
                    break;
                    
            }
            
            prevTime = threadStat.getTime();
        }
        
        container.visibleProperty().bind(viewModel.showProperty());
        container.getChildren().addAll(rects);
        setGraphic(container);
    }

    private void updateToEmptyCell() {
        setText(null);
        setGraphic(null);
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
        
        if((event != ThreadEvent.Unused) && (timeDiff > 0)){
            Tooltip.install(rectangle, new Tooltip(caption));
        }
        
        return rectangle;
    }

}
