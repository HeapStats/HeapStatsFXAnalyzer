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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.ntt.oss.heapstats.container.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.ObjectData;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;
import jp.co.ntt.oss.heapstats.parser.ParserEventHandler;

/**
 *
 * @author Yasu
 */
public class SnapShotHandler implements ParserEventHandler{
    
    private final Map<SnapShotHeader, Map<Long, ObjectData>> snapShots;
    
    private Map<Long, ObjectData> currentSnapShotData;
    
    private ObjectData currentObjectData;
    
    public SnapShotHandler() {
        snapShots = new HashMap<>();
    }

    @Override
    public ParseResult onStart(long off) {
        /* Nothing to do */
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onNewSnapShot(SnapShotHeader header, String parent) {
        currentSnapShotData = new HashMap<>();
        snapShots.put(header, currentSnapShotData);
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onEntry(ObjectData data) {
        currentSnapShotData.put(data.getTag(), data);
        currentObjectData = data;
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onChildEntry(long parentClassTag, ChildObjectData child) {
        List<ChildObjectData> referenceList = currentObjectData.getReferenceList();

        if(referenceList == null){
            referenceList = new ArrayList<>();
            currentObjectData.setReferenceList(referenceList);
        }
        
        referenceList.add(child);

        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onFinish(long off) {
        currentSnapShotData.forEach((k, v) -> v.setLoaderName(currentSnapShotData));
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    public Map<SnapShotHeader, Map<Long, ObjectData>> getSnapShots() {
        return snapShots;
    }

}
