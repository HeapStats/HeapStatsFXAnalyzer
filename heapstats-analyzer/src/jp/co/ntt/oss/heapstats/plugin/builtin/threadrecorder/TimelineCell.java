package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import jp.co.ntt.oss.heapstats.container.threadrecord.ThreadStat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Table cell for thread timeline.
 */
public class TimelineCell extends TableCell<ThreadStatViewModel, List<ThreadStat>> {
    
    private final LocalDateTime rangeStart;
    
    private final LocalDateTime rangeEnd;

    public TimelineCell(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER_LEFT);

        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
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
        TimelineGenerator generator = new TimelineGenerator(viewModel, getTableColumn().prefWidthProperty());
        HBox container = generator.createTimeline(rangeStart, rangeEnd);
        
        container.visibleProperty().bind(viewModel.showProperty());
        setGraphic(container);
    }

    private void updateToEmptyCell() {
        setText(null);
        setGraphic(null);
    }

}
