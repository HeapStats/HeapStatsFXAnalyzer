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

package jp.co.ntt.oss.heapstats.plugin.builtin.snapshot.model;

import java.time.LocalDateTime;
import jp.co.ntt.oss.heapstats.container.ObjectData;

/**
 *
 * @author yasuenag
 */
public class DiffData implements Comparable<DiffData>{
    
    private final LocalDateTime diffDate;
    
    private final String className;
    
    private final String classLoaderName;
    
    private final long instances;
    
    private final long totalSize;

    public DiffData(LocalDateTime diffDate, ObjectData prev, ObjectData current) {
        this.diffDate = diffDate;
        this.className = current.getName();
        this.classLoaderName = current.getLoaderName();
        this.instances = current.getCount() - prev.getCount();
        this.totalSize = current.getTotalSize() - prev.getCount();
    }

    public DiffData(LocalDateTime diffDate, String className, String classLoaderName, Long instances, long totalSize) {
        this.diffDate = diffDate;
        this.className = className;
        this.classLoaderName = classLoaderName;
        this.instances = instances;
        this.totalSize = totalSize;
    }

    public LocalDateTime getDiffDate() {
        return diffDate;
    }

    public String getClassName() {
        return className;
    }

    public String getClassLoaderName() {
        return classLoaderName;
    }

    public long getInstances() {
        return instances;
    }

    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public int compareTo(DiffData o) {
        return Long.compare(totalSize, o.totalSize);
    }
    
    
}
