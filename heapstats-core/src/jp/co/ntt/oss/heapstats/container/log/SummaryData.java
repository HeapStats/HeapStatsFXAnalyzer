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

package jp.co.ntt.oss.heapstats.container.log;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Summary data class.<br/>
 * This class holds process summary information.
 * It shows at process summary table.
 * @author Yasumasa Suenaga
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
    
    private final double averageCPUUsage;
    
    private final double maxCPUUsage;
    
    private final double averageVSZ;
    
    private final double maxVSZ;
    
    private final double averageRSS;
    
    private final double maxRSS;
    
    private final double averageLiveThreads;
    
    private final long maxLiveThreads;
    
    /**
     * Constructor of SummaryData.
     * @param logData Log data list to be summarized.
     * @param diffData Difference data list to be summarized.
     */
    public SummaryData(List<LogData> logData, List<DiffData> diffData){
        DoubleSummaryStatistics cpuUsage = diffData.parallelStream()
                                                   .mapToDouble(d -> d.getCpuTotalUsage())
                                                   .summaryStatistics();
        averageCPUUsage = cpuUsage.getAverage();
        maxCPUUsage = cpuUsage.getMax();
        
        DiffSummaryStatistics diffSummary = logData.parallelStream()
                                                   .collect(DiffSummaryStatistics::new,
                                                            DiffSummaryStatistics::accept,
                                                            DiffSummaryStatistics::combine);
        
        averageVSZ = diffSummary.getAverageVSZ() / 1024.0d / 1024.0d; // in MB
        maxVSZ = diffSummary.getMaxVSZ() / 1024.0d / 1024.0d; // in MB
        
        averageRSS = diffSummary.getAverageRSS() / 1024.0d / 1024.0d; // in MB
        maxRSS = diffSummary.getMaxRSS() / 1024.0d / 1024.0d; // in MB

        averageLiveThreads = diffSummary.getAverageLiveThreads();
        maxLiveThreads = diffSummary.getMaxLiveThreads();
    }

    /**
     * Get average CPU usage of system.
     * @return Average of CPU usage
     */
    public double getAverageCPUUsage() {
        return averageCPUUsage;
    }

    /**
     * Get maximum CPU usage of system.
     * @return Maximum value of CPU usage
     */
    public double getMaxCPUUsage() {
        return maxCPUUsage;
    }

    /**
     * Get average virtual memoly size (VSZ) of java process.
     * @return Average of VSZ
     */
    public double getAverageVSZ() {
        return averageVSZ;
    }

    /**
     * Get maximum virtual memory size (VSZ) of java process.
     * @return Maximum value of VSZ
     */
    public double getMaxVSZ() {
        return maxVSZ;
    }

    /**
     * Get average resident set size (RSS) of java process.
     * @return Average of RSS
     */
    public double getAverageRSS() {
        return averageRSS;
    }

    /**
     * Get maximum resident set size (RSS) of java process.
     * @return Maximum value of RSS
     */
    public double getMaxRSS() {
        return maxRSS;
    }

    /**
     * Get average live Java thread count.
     * @return Average of Java live thread
     */
    public double getAverageLiveThreads() {
        return averageLiveThreads;
    }

    /**
     * Get maximum live Java thread count.
     * @return Maximum value of Java live thread
     */
    public long getMaxLiveThreads() {
        return maxLiveThreads;
    }
    
    /**
     * Statistics class for SnapSHot diff calculation.
     */
    private class DiffSummaryStatistics{
        
        private final LongAdder count;
        
        private final LongAdder vsz;
        
        private final AtomicLong maxVSZ;
        
        private final LongAdder rss;
        
        private final AtomicLong maxRSS;
        
        private final LongAdder liveThreads;
        
        private final AtomicLong maxLiveThreads;
        
        public DiffSummaryStatistics(){
            count = new LongAdder();

            vsz = new LongAdder();
            maxVSZ = new AtomicLong();
            
            rss = new LongAdder();
            maxRSS = new AtomicLong();
            
            liveThreads = new LongAdder();
            maxLiveThreads = new AtomicLong();
        }
        
        public void accept(LogData logData){
            count.increment();
            
            vsz.add(logData.getJavaVSSize());
            maxVSZ.accumulateAndGet(logData.getJavaVSSize(), Math::max);
            
            rss.add(logData.getJavaRSSize());
            maxRSS.accumulateAndGet(logData.getJavaRSSize(), Math::max);
            
            liveThreads.add(logData.getJvmLiveThreads());
            maxLiveThreads.accumulateAndGet(logData.getJvmLiveThreads(), Math::max);
        }
        
        public void combine(DiffSummaryStatistics other){
            count.add(other.count.intValue());
            
            vsz.add(other.vsz.longValue());
            maxVSZ.accumulateAndGet(other.maxVSZ.get(), Math::max);
            
            rss.add(other.rss.longValue());
            maxRSS.accumulateAndGet(other.maxRSS.get(), Math::max);
            
            liveThreads.add(other.liveThreads.longValue());
            maxLiveThreads.accumulateAndGet(other.maxLiveThreads.get(), Math::max);
        }
        
        public double getAverageVSZ(){
            return vsz.doubleValue() / count.doubleValue();
        }
        
        public long getMaxVSZ(){
            return maxVSZ.get();
        }
        
        public double getAverageRSS(){
            return rss.doubleValue() / count.doubleValue();
        }
        
        public long getMaxRSS(){
            return maxRSS.get();
        }
        
        public double getAverageLiveThreads(){
            return liveThreads.doubleValue() / count.doubleValue();
        }
        
        public long getMaxLiveThreads(){
            return maxLiveThreads.get();
        }
        
    }
    
}
