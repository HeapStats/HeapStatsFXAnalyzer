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

package jp.co.ntt.oss.heapstats.csv;

import java.io.File;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import jp.co.ntt.oss.heapstats.container.ObjectData;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;

/**
 * CSV writer class for GC statistics and heap histogram.
 */
public class CSVDumpHeapTask extends Task<Void>{
    
    /** Save the file name. */
    private final File csvFile;
    
    /** SnapShot to dump */
    private final Map<SnapShotHeader, Map<Long, ObjectData>> snapShots;
    
    /** Filter set. */
    private final Set<String> filter;

    /**
     * Constructor of CSVDumpHeap.
     *
     * @param csvFile File name of csv file to dump.
     * @param target List of SnapShot to dump.
     * @param filter Filter list to dump.
     */
    public CSVDumpHeapTask(File csvFile, Map<SnapShotHeader, Map<Long, ObjectData>> target, Set<String> filter) {
        this.csvFile = csvFile;
        this.snapShots = target;
        this.filter = filter;
    }

    @Override
    protected Void call() throws Exception {
        
        try(PrintWriter writer = new PrintWriter(csvFile)){
            
            /* Collect all class tags and names from target snapshots. */
            Map<Long, String> targetClasses = new ConcurrentHashMap<>();
            if((filter == null) || filter.isEmpty()){
                snapShots.values().parallelStream()
                                  .forEach(m -> m.forEach((k, v) -> targetClasses.put(k, v.getName())));
            }
            else{
                snapShots.values().parallelStream()
                                  .forEach(m -> m.forEach((k, v) -> {
                                                                       if(filter.contains(v.getName())){
                                                                         targetClasses.put(k, v.getName());
                                                                       }
                                                                    }));
            }
            
            /* Sorted SnapShot DateTime List */
            List<SnapShotHeader> headers = snapShots.keySet().stream()
                                                             .sorted()
                                                             .collect(Collectors.toList());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS");
            
            /* Write CSV header */
            StringJoiner headerJoiner = new StringJoiner(",");
            headerJoiner.add("Tag")
                        .add("Name");
            headers.stream()
                   .map(h -> formatter.format(h.getSnapShotDate()))
                   .forEachOrdered(s -> headerJoiner.add(String.format("%s_instances", s))
                                                    .add(String.format("%s_total size", s)));
            writer.println(headerJoiner.toString());
            
            /* Dump data */
            targetClasses.forEach((k, v) -> {
                                               StringJoiner joiner = new StringJoiner(",");
                                               joiner.add(String.format("0x%X", k))
                                                     .add(v);
                                               headers.forEach(h -> {
                                                                       Optional<ObjectData> objData = Optional.ofNullable(snapShots.get(h).get(k));
                                                                       joiner.add(objData.map(o -> o.getCount())
                                                                                         .orElse(0L)
                                                                                         .toString())
                                                                             .add(objData.map(o -> o.getTotalSize())
                                                                                         .orElse(0L)
                                                                                         .toString());
                                                                    });
                                               writer.println(joiner.toString());
                                            });
            
        }
        
        return null;        
    }

}
