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

package jp.co.ntt.oss.heapstats.plugin.reftree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jp.co.ntt.oss.heapstats.container.snapshot.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;

/**
 * This class tracks object references.
 * 
 * @author Yasumasa Suenaga
 */
public class ReferenceTracker {
    
    private final Map<Long, ObjectData> snapShot;
    
    private final long startTag;

    public ReferenceTracker(Map<Long, ObjectData> snapShot, long startTag) {
        this.snapShot = snapShot;
        this.startTag = startTag;
    }
    
    /**
     * This method tracks object references which direction is parent.
     * 
     * @param sortBySize Sort order. If this parameter is true, return list is
     *                    sorted by total size. Others, return list is sorted by
     *                    instance count.
     * @return List of parents.
     */
    public List<ChildObjectData> getParents(boolean sortBySize){
        /* Pick up parents which have reference to target object */
        Stream<ObjectData> parentStream = snapShot.values().parallelStream()
                                                           .filter(o -> o.getReferenceList() != null)
                                                           .filter(o -> o.getReferenceList().stream()
                                                                                            .anyMatch(c -> (c.getTag() == startTag)));

        /* This comparator is reverse order. */
        Comparator<ChildObjectData> comparator = sortBySize ? (x, y) -> Long.compare(y.getTotalSize(), x.getTotalSize())
                                                            : (x, y) -> Long.compare(y.getInstances(), x.getInstances());
        
        return parentStream.map(o -> {
                                        ChildObjectData target = o.getReferenceList().stream()
                                                                                     .filter(c -> (c.getTag() == startTag))
                                                                                     .findAny()
                                                                                     .get();
                                        return new ChildObjectData(o.getTag(), target.getInstances(), target.getTotalSize());
                                     })
                           .sorted(comparator)
                           .collect(Collectors.toList());
    }
    
    /**
     * This method tracks object references which direction is child.
     * 
     * @param sortBySize Sort order. If this parameter is true, return list is
     *                    sorted by total size. Others, return list is sorted by
     *                    instance count.
     * @return List of children.
     */
    public List<ChildObjectData> getChildren(boolean sortBySize){
        List<ChildObjectData> children = snapShot.get(startTag).getReferenceList();
        
        if(children == null){
            return new ArrayList<>();
        }
        
        /* This comparator is reverse order. */
        Comparator<ChildObjectData> comparator = sortBySize ? (x, y) -> Long.compare(y.getTotalSize(), x.getTotalSize())
                                                            : (x, y) -> Long.compare(y.getInstances(), x.getInstances());
        
        return children.stream()
                       .sorted(comparator)
                       .collect(Collectors.toList());
    }
    
}
