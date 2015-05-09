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

package jp.co.ntt.oss.heapstats.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.SnapShotHeader;
import static java.util.stream.Collectors.*;

/**
 * CSV writer class for GC statistics and heap histogram.
 */
public class CSVDumpHeap extends ProgressRunnable{
    
    /** Save the file name. */
    private final File csvFile;
    
    /** SnapShot to dump */
    private final List<SnapShotHeader> snapShots;
    
    /** Filter. */
    private final Optional<Predicate<? super ObjectData>> filter;
    
    private final boolean needJavaStyle;

    /**
     * Constructor of CSVDumpHeap.
     *
     * @param csvFile File name of csv file to dump.
     * @param target List of SnapShot to dump.
     * @param filter Filter list to dump.
     * @param needJavaStyle
     */
    public CSVDumpHeap(File csvFile, List<SnapShotHeader> target, Predicate<? super ObjectData> filter, boolean needJavaStyle) {
        this.csvFile = csvFile;
        this.snapShots = target;
        this.filter = Optional.ofNullable(filter);
        this.needJavaStyle = needJavaStyle;
    }

    @Override
    public void run() {
        
        try(PrintWriter writer = new PrintWriter(csvFile)){
            /* Collect all class tags and names from target snapshots. */
            Map<Long, String> targetClasses = snapShots.stream()
                                                       .flatMap(s -> s.getSnapShot(needJavaStyle).entrySet().stream())
                                                       .filter(e -> filter.orElse(f -> true).test(e.getValue()))
                                                       .collect(toConcurrentMap(e -> e.getKey(), e -> e.getValue().getName()));
            
            /* Sorted SnapShot DateTime List */
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS");
            
            /* Write CSV header */
            StringJoiner headerJoiner = new StringJoiner(",");
            headerJoiner.add("Tag")
                        .add("Name");
            snapShots.stream()
                     .map(h -> formatter.format(h.getSnapShotDate()))
                     .forEachOrdered(s -> headerJoiner.add(String.format("%s_instances", s))
                                                      .add(String.format("%s_total size", s)));
            writer.println(headerJoiner.toString());
            
            /* Dump data */
            targetClasses.forEach((k, v) -> {
                                               StringJoiner joiner = new StringJoiner(",");
                                               joiner.add(String.format("0x%X", k))
                                                     .add(v);
                                               snapShots.forEach(h -> {
                                                                        Optional<ObjectData> objData = Optional.ofNullable(h.getSnapShot(needJavaStyle).get(k));
                                                                        joiner.add(objData.map(o -> o.getCount())
                                                                                          .orElse(0L)
                                                                                          .toString())
                                                                              .add(objData.map(o -> o.getTotalSize())
                                                                                          .orElse(0L)
                                                                                          .toString());
                                                                      });
                                               writer.println(joiner.toString());
                                            });
            
        } catch (FileNotFoundException ex) {
            throw new UncheckedIOException(ex);
        }
        
    }

}
