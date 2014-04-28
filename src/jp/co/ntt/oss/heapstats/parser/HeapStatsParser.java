/*
 * HeapStatsParser.java
 * Created on 2011/10/13
 *
 * Copyright (C) 2011-2014 Nippon Telegraph and Telephone Corporation
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
 *
 */

package jp.co.ntt.oss.heapstats.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystems;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.ntt.oss.heapstats.container.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.ObjectData;
import jp.co.ntt.oss.heapstats.container.SnapShotHeader;
import jp.co.ntt.oss.heapstats.parser.ParserEventHandler.ParseResult;
import jp.co.ntt.oss.heapstats.utils.HeapStatsUtils;

/**
 * This class loads a file java heap information.
 */
public class HeapStatsParser {

    /**
     * Format version of the snapshot file. No information on the child class.
     */
    private static final int FILE_FORMAT_NO_CHILD = 49;

    /** Format version of the snapshot file. Have a child class information. */
    private static final int FILE_FORMAT_HAVE_CHILD = 60;

    /** Format version of the snapshot file. Have a metaspace information. */
    private static final int FILE_FORMAT_HAVE_CHILD_AND_METASPACE = 61;
    
    private final ByteBuffer longBuffer;
    
    private final ByteBuffer intBuffer;

    public HeapStatsParser() {
        longBuffer = ByteBuffer.allocate(8);
        intBuffer = ByteBuffer.allocate(4);
    }
    
    /**
     * Setter of byte order.
     * This value is used in parsing of SnapShot file.
     * 
     * @param order byte order of this SnapShot file.
     */
    private void setByteOrder(ByteOrder order){
        longBuffer.order(order);
        intBuffer.order(order);
    }
    
    /**
     * Reader of long (8 bytes) value from channel.
     * 
     * @param ch Channel to be read.
     * @return read value.
     * @throws IOException 
     */
    private long readLong(SeekableByteChannel ch) throws IOException{
        ch.read(longBuffer);
        longBuffer.flip();
        long ret = longBuffer.getLong();
        longBuffer.flip();
        
        return ret;
    }

    /**
     * Reader of int (4 bytes) value from channel.
     * 
     * @param ch Channel to be read.
     * @return read value.
     * @throws IOException 
     */
    private int readInt(SeekableByteChannel ch) throws IOException{
        ch.read(intBuffer);
        intBuffer.flip();
        int ret = intBuffer.getInt();
        intBuffer.flip();
        
        return ret;
    }
    
    /**
     * Parse SnapShot.
     * @param fname File name to be parsed.
     * @param startOfs Offset of to be parsed in SnapShot.
     * @param handler SnapShot handler.
     * @return true if parsing is succeeded.
     * @throws IOException 
     */
    public boolean parse(String fname, long startOfs, ParserEventHandler handler) throws IOException {
        SnapShotHeader header;

        try(FileInputStream stream = new FileInputStream(fname)) {
            stream.skip(startOfs);
            FileChannel ch = stream.getChannel();
            
            while (true) {
                long offset = ch.position();
                handler.onStart(offset);

                int ret = stream.read();

                if (ret == -1) {
                    // EOF
                    break;
                }
                else if(ret == FILE_FORMAT_NO_CHILD ||
                        ret == FILE_FORMAT_HAVE_CHILD ||
                        ret == FILE_FORMAT_HAVE_CHILD_AND_METASPACE){
                    // Heap
                    header = parseHeader(stream, ret);
                    header.setFileOffset(offset);
                    header.setSnapshotFile(FileSystems.getDefault().getPath(fname));

                    if(handler.onNewSnapShot(header, fname) != ParseResult.HEAPSTATS_PARSE_CONTINUE){
                        return false;
                    }
                    
                    if(parseElement(stream, header, handler, ret) == ParseResult.HEAPSTATS_PARSE_ABORT){
                        return false;
                    }

                }
                else{
                    StringBuilder errString = new StringBuilder();
                    errString.append("Unknown FileType! (");
                    errString.append(ret);
                    errString.append(")");
                    throw new IOException(errString.toString());
                }
                
                if(handler.onFinish(ch.position()) == ParseResult.HEAPSTATS_PARSE_ABORT){
                    return false;
                }
                
            }
            
        }

        return true;
    }

    /**
     * Output to a temporary file and then parse the data in the snapshot agent
     * output.
     *
     * @param fname the file java heap information.
     * @param handler the ParserEventHandler.
     * @return Return the Reading results file
     * @throws IOException If you load the file java heap information is not set
     *         ByteOrder agent if that occurs
     */
    public boolean parse(String fname, ParserEventHandler handler) throws IOException {
        return parse(fname, 0, handler);
    }

    /**
     * Extracting the header information of the snapshot.
     *
     * @param stream the file Java Heap Information.
     * @param format Snapshot format.
     * @return Return the SnapShotHeader
     * @throws IOException If other I / O error occurs.
     */
    protected SnapShotHeader parseHeader(final FileInputStream stream, final int format) throws IOException {
        SnapShotHeader header = new SnapShotHeader();
        int ret = stream.read();

        switch (ret) {
            
            case 'L':
                header.setByteOrderMark(ByteOrder.LITTLE_ENDIAN);
                break;
                
            case 'B':
                header.setByteOrderMark(ByteOrder.BIG_ENDIAN);
                break;
                
            default:
                StringBuilder errString = new StringBuilder();
                errString.append("Unknown ByteOrderMark! (");
                errString.append(ret);
                errString.append(")");
                throw new IOException(errString.toString());
                
        }

        FileChannel ch = stream.getChannel();
        setByteOrder(header.getByteOrderMark());

        // SnapShot Date
        header.setSnapShotDateAsLong(readLong(ch));
        // Entries
        header.setNumEntries(readLong(ch));

        // SnapShot Cause
        header.setCause(readInt(ch));

        // GC Cause
        int len = (int)readLong(ch);
        byte[] gcCause = new byte[len];
        if (stream.read(gcCause) != gcCause.length) {
            throw new IOException("Could not get the GC Cause.");
        }
        header.setGcCause(gcCause[0] == '\0' ? "-" : new String(gcCause));

        // Full GC Count
        header.setFullCount(readLong(ch));

        // Young GC Count
        header.setYngCount(readLong(ch));

        // GC Time
        header.setGcTime(readLong(ch));

        // New Heap Size
        header.setNewHeap(readLong(ch));

        // Old Heap Size
        header.setOldHeap(readLong(ch));

        // Total Heap Size
        header.setTotalCapacity(readLong(ch));

        if(format == FILE_FORMAT_HAVE_CHILD_AND_METASPACE){
          // Metaspace usage
          header.setMetaspaceUsage(readLong(ch));

          // Metaspace capacity
          header.setMetaspaceCapacity(readLong(ch));
        }

        return header;
    }

    /**
     * Stored in a temporary file to extract the object information.
     *
     * @param stream the file Java Heap Information.
     * @param header the SnapShot header
     * @param handler ParserEventHandler
     * @param format Format version of the snapshot file
     * @return Return the Parse result.
     * @throws IOException If some other I/O error occurs
     */
    protected ParseResult parseElement(final FileInputStream stream,
            final SnapShotHeader header, final ParserEventHandler handler,
            final int format) throws IOException {

        FileChannel ch = stream.getChannel();
        
        for (long i = 0; i < header.getNumEntries(); i++) {
            byte[] classNameInBytes;
            ParserEventHandler.ParseResult eventResult;
            ObjectData obj;
            obj = new ObjectData();

            // tag
            obj.setTag(readLong(ch));

            // class Name
            classNameInBytes = new byte[(int)readLong(ch)];
            if (stream.read(classNameInBytes) != classNameInBytes.length) {
                throw new IOException("Could not get the Class name.");
            }

            if (HeapStatsUtils.getReplaceClassName()) {
                String tmp = new String(classNameInBytes)
                                  .replaceAll("^L|(^\\[*)L|;$", "$1")
                                  .replaceAll("(^\\[*)B$", "$1byte")
                                  .replaceAll("(^\\[*)C$", "$1char")
                                  .replaceAll("(^\\[*)I$", "$1int")
                                  .replaceAll("(^\\[*)S$", "$1short")
                                  .replaceAll("(^\\[*)J$", "$1long")
                                  .replaceAll("(^\\[*)D$", "$1double")
                                  .replaceAll("(^\\[*)F$", "$1float")
                                  .replaceAll("(^\\[*)V$", "$1void")
                                  .replaceAll("(^\\[*)Z$", "$1boolean");

                Pattern pattern = Pattern.compile("^\\[(.*)");
                Matcher matcher = pattern.matcher(tmp);
                while (matcher.find()) {
                    tmp = matcher.replaceAll("$1 []");
                    matcher = pattern.matcher(tmp);
                }

                obj.setName(tmp.replaceAll("\\/", "."));
            }
            else {
                obj.setName(new String(classNameInBytes));
            }

            if (format >= FILE_FORMAT_HAVE_CHILD) {
                obj.setClassLoader(readLong(ch));
                obj.setClassLoaderTag(readLong(ch));
            }

            // instance
            obj.setCount(readLong(ch));
            // heap usage
            obj.setTotalSize(readLong(ch));

            eventResult = handler.onEntry(obj);

            if ((eventResult == ParseResult.HEAPSTATS_PARSE_CONTINUE) &&
                (format >= FILE_FORMAT_HAVE_CHILD)) {
                eventResult = parseChildClass(obj.getTag(), ch,
                                         header.getByteOrderMark(), handler);
            }

            if (eventResult != ParseResult.HEAPSTATS_PARSE_CONTINUE) {
                return eventResult;
            }
            
        }

        return ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    /**
     * Child class to extract information from a stream of snapshots.
     *
     * @param parentClassTag Tags parent class for uniquely identifying the
     *        parent class
     * @param ch FileChannel of Snapshot file.
     * @param byteOrder the byte order
     * @param handler ParserEventHandler
     * @return Return the Parse result.
     * @throws IOException If some other I/O error occurs
     */
    protected ParseResult parseChildClass(final long parentClassTag,
            final FileChannel ch, final ByteOrder byteOrder,
            final ParserEventHandler handler) throws IOException {

        while (true) {
            long childClassTag = readLong(ch);
            long instances = readLong(ch);
            long totalSize = readLong(ch);

            if (childClassTag == -1) {
                return ParseResult.HEAPSTATS_PARSE_CONTINUE;
            }

            ParseResult result = handler.onChildEntry(parentClassTag,
                    new ChildObjectData(childClassTag, instances, totalSize));

            if (result != ParseResult.HEAPSTATS_PARSE_CONTINUE) {
                return result;
            }
            
        }
        
    }
    
}
