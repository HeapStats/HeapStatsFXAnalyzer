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

package jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.handler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
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
    
    private final Map<SnapShotHeader, Map<Long, ObjectData>> snapShots;

    private final Map<LocalDateTime, List<ObjectData>> topNList;

    private final List<DiffData> lastDiffList;
    
    private final int rankLevel;
    
    private final boolean includeOthers;

    public DiffTask(Map<SnapShotHeader, Map<Long, ObjectData>> snapShots, int rankLevel, boolean includeOthers) {
        this.snapShots = snapShots;
        this.topNList = new HashMap<>();
        this.lastDiffList = new ArrayList<>();
        this.rankLevel = rankLevel;
        this.includeOthers = includeOthers;
    }
    
    /**
     * Build TopN data from givien snapshot header.
     * 
     * @param progress Progress counter.
     * @param header SnapShot header to build.
     * @param maxItrs Max iterations. This value uses updateProgress().
     */
    private void buildTopNData(LongAdder progress, SnapShotHeader header, long maxItrs){
        Map<Long, ObjectData> current = snapShots.get(header);
        List<ObjectData> buf = current.values().parallelStream()
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
        progress.increment();
        updateProgress(progress.longValue(), maxItrs);
    }

    @Override
    protected Void call() throws Exception {

        /* Calculate top N data */
        LongAdder cnt = new LongAdder();
        List<SnapShotHeader> keyList = snapShots.keySet().parallelStream()
                                                         .sorted(Comparator.naturalOrder())
                                                         .collect(Collectors.toList());
        long maxItrs = keyList.size() - 1;
        keyList.stream()
               .forEachOrdered(h -> buildTopNData(cnt, h, maxItrs));
        
       List<Long> rankedTagList = topNList.values().stream()
                                                   .flatMap(c -> c.stream())
                                                   .mapToLong(o -> o.getTag())
                                                   .filter(t -> t != 0L)
                                                   .distinct()
                                                   .boxed()
                                                   .collect(Collectors.toList());
        
        /* Calculate summarize diff */
        Map<Long, ObjectData> start = snapShots.get(keyList.get(0));
        SnapShotHeader endHeader = keyList.get(keyList.size() - 1);
        Map<Long, ObjectData> end = snapShots.get(endHeader);
        start.forEach((k, v) -> end.putIfAbsent(k, new ObjectData(k, v.getName(), v.getClassLoader(), v.getClassLoaderTag(), 0, 0, v.getLoaderName(), null)));
        end.forEach((k, v) -> lastDiffList.add(new DiffData(endHeader.getSnapShotDate(), start.get(k), v, rankedTagList.contains(v.getTag()))));
        
        return null;
    }

    public Map<LocalDateTime, List<ObjectData>> getTopNList() {
        return topNList;
    }

    public List<DiffData> getLastDiffList() {
        return lastDiffList;
    }
    
}
