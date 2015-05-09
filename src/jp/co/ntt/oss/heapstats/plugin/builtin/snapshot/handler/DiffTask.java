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

package jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.handler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import jp.co.ntt.oss.heapstats.container.ObjectData;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;
import jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.model.DiffData;

/**
 *
 * @author yasuenag
 */
public class DiffTask extends Task<Void>{
    
    private final List<SnapShotHeader> snapShots;

    private final Map<LocalDateTime, List<ObjectData>> topNList;

    private final List<DiffData> lastDiffList;
    
    private final int rankLevel;
    
    private final boolean includeOthers;
    
    private final Optional<Predicate<? super ObjectData>> filter;

    public DiffTask(List<SnapShotHeader> snapShots, int rankLevel, boolean includeOthers, Predicate<? super ObjectData> filter) {
        this.snapShots = snapShots;
        this.topNList = new HashMap<>();
        this.lastDiffList = new ArrayList<>();
        this.rankLevel = rankLevel;
        this.includeOthers = includeOthers;
        this.filter = Optional.ofNullable(filter);
    }
        
    /**
     * Build TopN data from givien snapshot header.
     * 
     * @param header SnapShot header to build.
     * @param counter Counter for progress indicator.
     */
    private void buildTopNData(SnapShotHeader header, AtomicLong counter){
        List<ObjectData> buf = header.getSnapShot()
                                     .values()
                                     .parallelStream()
                                     .filter(filter.orElse(o -> true))
                                     .sorted(Comparator.reverseOrder())
                                     .limit(rankLevel)
                                     .collect(Collectors.toList());
        
        if(includeOthers){
            ObjectData other = new ObjectData();
            other.setName("Others");
            other.setTotalSize(header.getNewHeap() + header.getOldHeap() - buf.stream()
                                                                              .mapToLong(d -> d.getTotalSize())
                                                                              .sum());
            buf.add(other);
        }
        
        topNList.put(header.getSnapShotDate(), buf);
        updateProgress(counter.incrementAndGet(), snapShots.size());
    }

    @Override
    protected Void call() throws Exception {
        AtomicLong counter = new AtomicLong();
        
        /* Calculate top N data */
        snapShots.stream()
                 .forEachOrdered(h -> buildTopNData(h, counter));
        
        List<Long> rankedTagList = topNList.values().stream()
                                                    .flatMap(c -> c.stream())
                                                    .mapToLong(o -> o.getTag())
                                                    .filter(t -> t != 0L)
                                                    .distinct()
                                                    .boxed()
                                                    .collect(Collectors.toList());
        
        /* Calculate summarize diff */
        SnapShotHeader startHeader = snapShots.get(0);
        SnapShotHeader endHeader = snapShots.get(snapShots.size() - 1);
        
        Map<Long, ObjectData> start = startHeader.getSnapShot();
        Map<Long, ObjectData> end = endHeader.getSnapShot();
        start.forEach((k, v) -> end.putIfAbsent(k, new ObjectData(k, v.getName(), v.getClassLoader(), v.getClassLoaderTag(), 0, 0, v.getLoaderName(), null)));
        
        if(filter.isPresent()){
            end.forEach((k, v) -> {
                                     if(filter.get().test(v)){
                                         lastDiffList.add(new DiffData(endHeader.getSnapShotDate(), start.get(k), v, rankedTagList.contains(v.getTag())));
                                     }
                                  });
        }
        else{
            end.forEach((k, v) -> lastDiffList.add(new DiffData(endHeader.getSnapShotDate(), start.get(k), v, rankedTagList.contains(v.getTag()))));
        }
        
        return null;
    }

    public Map<LocalDateTime, List<ObjectData>> getTopNList() {
        return topNList;
    }

    public List<DiffData> getLastDiffList() {
        return lastDiffList;
    }
    
}
