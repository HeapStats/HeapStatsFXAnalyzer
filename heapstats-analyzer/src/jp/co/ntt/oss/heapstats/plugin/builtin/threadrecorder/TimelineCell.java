package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import jp.co.ntt.oss.heapstats.container.threadrecord.ThreadStat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Table cell for thread timeline.
 */
public class TimelineCell extends TableCell<ThreadStatViewModel, List<ThreadStat>> {

    private static final double RECT_HEIGHT = 16;

    private static final String CSS_CLASS_PREFIX = "rect-";

    private final HBox container;
    
    private final ThreadRecorderController controller;

    public TimelineCell(ThreadRecorderController controller) {
        this.controller = controller;
        container = new HBox(0);
        container.setAlignment(Pos.CENTER_LEFT);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(List<ThreadStat> item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
            updateToEmptyCell();
        } else {
            ThreadStatViewModel viewModel = getTableView().getItems().get(getIndex());
            LocalDateTime startTime = viewModel.getStartTime();
            LocalDateTime endTime = viewModel.getEndTime();
            if (startTime.isAfter(endTime)) {
                updateToEmptyCell();
            } else {
                drawTimeline(startTime, item, endTime);
            }
        }
    }

    private void drawTimeline(LocalDateTime startTime, List<ThreadStat> item, LocalDateTime endTime) {
        container.getChildren().clear();

        LocalDateTime prevTime = startTime;
        ThreadStat.ThreadEvent prevEvent = item.get(0).getEvent();
        List<Rectangle> rects = new ArrayList<>();
        for (int i = 0; i < item.size(); i++) {
            ThreadStat threadStat = item.get(i);
            LocalDateTime currentTime = threadStat.getTime();
            boolean end = currentTime.isAfter(endTime);
            currentTime = end ? endTime : currentTime;
            if (i == 0 && threadStat.getEvent() == ThreadStat.ThreadEvent.ThreadStart) {
                rects.add(createThreadRect(prevTime, currentTime, ThreadStat.ThreadEvent.Unused));
            } else {
                rects.add(createThreadRect(prevTime, currentTime, prevEvent));
            }
            if (end) {
                break;
            }
            
            prevTime = currentTime;
            prevEvent = threadStat.getEvent();
            
            if (i == item.size() - 1 && threadStat.getEvent() != ThreadStat.ThreadEvent.ThreadEnd) {
                rects.add(createThreadRect(prevTime, endTime, prevEvent));
            }
        }
        container.getChildren().addAll(rects);
        setGraphic(container);
    }

    private void updateToEmptyCell() {
        setText(null);
        setGraphic(null);
    }

    private Rectangle createThreadRect(LocalDateTime startTime, LocalDateTime endTime,
                                       ThreadStat.ThreadEvent prevEvent) {
        long range = controller.getRangeStart().until(controller.getRangeEnd(), ChronoUnit.MILLIS);
        long timeDiff = startTime.until(endTime, ChronoUnit.MILLIS);
        double width = (this.getTableView().getWidth() / (double)range) * timeDiff;
        Rectangle rectangle = new Rectangle(width, RECT_HEIGHT);
        String styleClass = CSS_CLASS_PREFIX + prevEvent.name().toLowerCase();
        rectangle.getStyleClass().add(styleClass);
        return rectangle;
    }

}
