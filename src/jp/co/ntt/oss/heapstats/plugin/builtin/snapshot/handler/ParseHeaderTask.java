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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;
import jp.co.ntt.oss.heapstats.parser.HeapStatsParser;

/**
 *
 * @author Yasu
 */
public class ParseHeaderTask extends Task<Void>{
    
    private final List<String> files;
    
    private List<SnapShotHeader> snapShotList;

    public ParseHeaderTask(List<String> files) {
        this.files = files;
        snapShotList = null;
    }

    @Override
    protected Void call() throws Exception {
        SnapShotListHandler handler = new SnapShotListHandler();
        HeapStatsParser parser = new HeapStatsParser();
        
        files.stream().forEach(f -> {
                                      try{
                                          parser.parse(f, handler);
                                      }
                                      catch(IOException e){
                                          throw new UncheckedIOException(e);
                                      }
                                    });
        
        snapShotList = handler.getHeaders().parallelStream()
                                           .sorted(Comparator.naturalOrder())
                                           .collect(Collectors.toList());
        
        return null;
    }

    public List<SnapShotHeader> getSnapShotList() {
        return snapShotList;
    }
    
}
