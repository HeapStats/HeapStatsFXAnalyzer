/*
 * Copyright (C) 2015 Yasumasa Suenaga
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
package jp.co.ntt.oss.heapstats.plugin.builtin.threadrecorder;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import jp.co.ntt.oss.heapstats.container.threadrecord.ThreadStat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thread Status Data Model for presentation.
 * 
 * @author Yasumasa Suenaga
 */
public class ThreadStatViewModel {
    
    private final BooleanProperty show;
    
    private long id;
    
    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private final ReadOnlyObjectWrapper<List<ThreadStat>> threadStats;

    public ThreadStatViewModel(long id, String name, LocalDateTime startTime, LocalDateTime endTime,
            List<ThreadStat> threadStats) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.threadStats = new ReadOnlyObjectWrapper<>(threadStats);
        this.show = new SimpleBooleanProperty(true);
    }
    
    public BooleanProperty showProperty(){
        return show;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<ThreadStat> getThreadStats() {
        return threadStats.get();
    }

    public ReadOnlyObjectProperty<List<ThreadStat>> threadStatsProperty() {
        return threadStats.getReadOnlyProperty();
    }

}
