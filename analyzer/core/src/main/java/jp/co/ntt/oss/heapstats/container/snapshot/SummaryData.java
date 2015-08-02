/*
 * Copyright (C) 2014-2015 Yasumasa Suenaga
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

package jp.co.ntt.oss.heapstats.container.snapshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Summary data class.<br/>
 * This class holds process summary information.
 * It shows at process summary table.
 * @author Yasu
 */
public class SummaryData {
    
    public static class SummaryDataEntry{
        
        private String category;
        
        private String value;
        
        public SummaryDataEntry(String category, String value){
            this.category = category;
            this.value = value;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
        
    }
    
    private final int count;
    
    private final long fullCount;
    
    private final long yngCount;
    
    private final long latestHeapUsage;
    
    private final long latestMetaspaceUsage;
    
    private final long maxGCTime;
    
    private final long maxSnapshotSize;
    
    private final long maxEntryCount;
    
    /**
     * Constructor of SummaryData.
     * @param headers List of SnapShot headers which should be summarized.
     */
    public SummaryData(List<SnapShotHeader> headers){
        count = headers.size();
        
        SnapShotHeader start = headers.get(0);
        SnapShotHeader end = headers.get(count - 1);
        
        fullCount = end.getFullCount() - start.getFullCount();
        yngCount = end.getYngCount() - start.getYngCount();
        
        latestHeapUsage = end.getNewHeap() + end.getOldHeap();
        latestMetaspaceUsage = end.getMetaspaceUsage();
        
        MaxSummaryStatistics statistics = headers.parallelStream()
                                                 .collect(MaxSummaryStatistics::new,
                                                         MaxSummaryStatistics::accept,
                                                         MaxSummaryStatistics::combine);
        maxGCTime = statistics.getMaxGCTime();
        maxSnapshotSize = statistics.getMaxSnapshotSize();
        maxEntryCount = statistics.getMaxEntryCount();
    }

    /**
     * Get count of SnapShot headers.
     * @return SnapShot count
     */
    public int getCount() {
        return count;
    }

    /**
     * Get count of FullGC.
     * @return FullGC count
     */
    public long getFullCount() {
        return fullCount;
    }

    /**
     * Get count of YoungGC.
     * @return YoungGC count
     */
    public long getYngCount() {
        return yngCount;
    }

    /**
     * Get Java heap usage in last SnapShot.
     * @return Java heap usage
     */
    public long getLatestHeapUsage() {
        return latestHeapUsage;
    }

    /**
     * Get metaspace usage in last SnapShot.
     * @return Metaspace usage
     */
    public long getLatestMetaspaceUsage() {
        return latestMetaspaceUsage;
    }

    /**
     * Get maximum value of GC time.
     * @return Maximum value of GC time
     */
    public long getMaxGCTime() {
        return maxGCTime;
    }

    /**
     * Get maximum value of SnapShot size.
     * @return Maximum value of SnapShot size
     */
    public long getMaxSnapshotSize() {
        return maxSnapshotSize;
    }

    /**
     * Get maximum value of entries in SnapShot.
     * @return Maximum value of SnapShot entries
     */
    public long getMaxEntryCount() {
        return maxEntryCount;
    }
    
    private class MaxSummaryStatistics{
        
        private final AtomicLong maxGCTime;
        
        private final AtomicLong maxSnapshotSize;
        
        private final AtomicLong maxEntryCount;
        
        public MaxSummaryStatistics(){
            maxGCTime = new AtomicLong();
            maxSnapshotSize = new AtomicLong();
            maxEntryCount = new AtomicLong();
        }
        
        public void accept(SnapShotHeader header){
            maxGCTime.accumulateAndGet(header.getGcTime(), Math::max);
            maxSnapshotSize.accumulateAndGet(header.getSnapShotSize(), Math::max);
            maxEntryCount.accumulateAndGet(header.getNumEntries(), Math::max);
        }
        
        public void combine(MaxSummaryStatistics other){
            maxGCTime.accumulateAndGet(other.maxGCTime.get(), Math::max);
            maxSnapshotSize.accumulateAndGet(other.maxSnapshotSize.get(), Math::max);
            maxEntryCount.accumulateAndGet(other.maxEntryCount.get(), Math::max);
        }
        
        public long getMaxGCTime(){
            return maxGCTime.get();
        }
        
        public long getMaxSnapshotSize(){
            return maxSnapshotSize.get();
        }
        
        public long getMaxEntryCount(){
            return maxEntryCount.get();
        }
        
    }

}
