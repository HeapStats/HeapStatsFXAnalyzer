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
import java.util.List;
import jp.co.ntt.oss.heapstats.container.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.ObjectData;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;
import jp.co.ntt.oss.heapstats.parser.ParserEventHandler;

/**
 *
 * @author Yasu
 */
public class SnapShotListHandler implements ParserEventHandler{
    
    private final List<SnapShotHeader> headers;
    
    private long instances;
    
    private SnapShotHeader currentHeader;

    public SnapShotListHandler() {
        this.headers = new ArrayList<>();
        this.instances = 0;
    }

    @Override
    public ParseResult onStart(long off) {
        this.instances = 0;
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onNewSnapShot(SnapShotHeader header, String parent) {
        currentHeader = header;
        headers.add(header);
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onEntry(ObjectData data) {
        instances += data.getCount();
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onChildEntry(long parentClassTag, ChildObjectData child) {
        /* Nothing to do */
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    @Override
    public ParseResult onFinish(long off) {
        currentHeader.setSnapShotSize(off - currentHeader.getFileOffset());
        currentHeader.setNumInstances(instances);
        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    public List<SnapShotHeader> getHeaders() {
        return headers;
    }
    
}
