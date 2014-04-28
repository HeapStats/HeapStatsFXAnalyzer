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

package jp.co.ntt.oss.heapstats.plugin.builtin.log.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.LogController;
import jp.co.ntt.oss.heapstats.utils.LocalDateTimeConverter;

/**
 * This class stores archive data.
 * 
 * @author Yasumasa Suenaga
 */
public class ArchiveData {
    
    private static final int INDEX_LOCAL_ADDR = 1;

    private static final int INDEX_FOREIGN_ADDR = 2;

    private static final int INDEX_STATE = 3;

    private static final int INDEX_QUEUE = 4;

    /** Represents the index value of the inode of the file socket endpoint. */
    private static final int INDEX_INODE = 9;

    private final LocalDateTime date;
    
    private final String archivePath;
    
    private File tmpPath;
    
    private List<AbstractMap.SimpleEntry<String, String>> envInfo;
    
    private List<String> tcp;
    
    private List<String> tcp6;
    
    private List<String> udp;
    
    private List<String> udp6;
    
    private List<String> sockOwner;
    
    private boolean parsed;
    
    /**
     * Constructor of ArchiveData.
     * Each fields is initialized from argument.
     * 
     * @param log LogData. This value must be included archive data.
     */
    public ArchiveData(LogData log){
        date = log.getDateTime();
        archivePath = log.getArchivePath();
        
        try {
            tmpPath = Files.createTempDirectory("heapstats_archive").toFile();
        } catch (IOException ex) {
            Logger.getLogger(ArchiveData.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        tmpPath.deleteOnExit();
        parsed = false;
    }
    
    /**
     * Build environment info from envInfo.txt .
     * 
     * @param archive HeapStats ZIP archive.
     * @param entry  ZipEntry of methodInfo.
     */
    private void buildEnvInfo(ZipFile archive, ZipEntry entry){
        
        try(InputStream in = archive.getInputStream(entry)){
            Properties prop = new Properties();
            prop.load(in);
            prop.computeIfPresent("CollectionDate", (k, v) -> (new LocalDateTimeConverter())
                                                                 .toString(LocalDateTime.ofInstant(Instant.ofEpochMilli(
                                                                         Long.parseLong((String)v)), ZoneId.systemDefault())));
            prop.computeIfPresent("LogTrigger", (k, v) -> {
                                                             switch(Integer.parseInt((String)v)){
                                                                 case 1:
                                                                     return "Resource Exhausted";
                                                                         
                                                                 case 2:
                                                                     return "Signal";
                                                                         
                                                                 case 3:
                                                                     return "Interval";
                                                                         
                                                                 case 4:
                                                                     return "Deadlock";
                                                                         
                                                                 default:
                                                                     return "Unknown";
                                                          }
                                                        });
                
            envInfo = new ArrayList<>();
            envInfo.add(new AbstractMap.SimpleEntry<>("archive", archivePath));
            prop.forEach((k, v) -> envInfo.add(new AbstractMap.SimpleEntry<>((String)k, (String)v)));
        }
        catch (IOException ex) {
            Logger.getLogger(ArchiveData.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    /**
     * Build String data from ZIP entry
     * 
     * @param archive HeapStats ZIP archive.
     * @param entry ZipEntry to be parsed.
     * @return String value from ZipEntry.
     */
    private List<String> buildStringData(ZipFile archive, ZipEntry entry){

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(archive.getInputStream(entry)))){
            return reader.lines()
                         .skip(1)
                         .map(s -> s.trim())
                         .collect(Collectors.toList());
        }
        catch (IOException ex) {
            Logger.getLogger(ArchiveData.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        return null;
    }
    
    /**
     * Deflating file in ZIP.
     * 
     * @param archive HeapStats ZIP archive.
     * @param entry ZipEntry to be deflated.
     */
    private void deflateFileData(ZipFile archive, ZipEntry entry){
        Path destPath = FileSystems.getDefault().getPath(tmpPath.getAbsolutePath(), entry.getName());
        
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(archive.getInputStream(entry)));
            BufferedWriter writer = Files.newBufferedWriter(destPath, StandardOpenOption.CREATE);){
            
            reader.lines()
                  .map(s -> s.replace('\0', ' '))
                  .forEach(s -> {
                                   try{
                                       writer.write(s);
                                       writer.newLine();
                                   }
                                   catch(IOException e){
                                       throw new UncheckedIOException(e);
                                   }
                                });
        }
        catch (IOException ex) {
            Logger.getLogger(ArchiveData.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    /**
     * Write IPv4 data.
     * 
     * @param data String data in procfs.
     * @param writer PrintWriter to write.
     */
    private void writeIPv4(String data, PrintWriter writer){
        StringJoiner joiner = (new StringJoiner("."))
                                 .add(Integer.valueOf(data.substring(6, 8), 16).toString())
                                 .add(Integer.valueOf(data.substring(4, 6), 16).toString())
                                 .add(Integer.valueOf(data.substring(2, 4), 16).toString())
                                 .add(Integer.valueOf(data.substring(0, 2), 16).toString());
        writer.print(joiner.toString());
    }
    
    /**
     * Write IPv6 data.
     * 
     * @param data String data in procfs.
     * @param writer PrintWrite to write.
     */
    private void writeIPv6(String data, PrintWriter writer){
        StringJoiner joiner = (new StringJoiner(":"))
                                 .add(Integer.valueOf(data.substring(6, 8) + data.substring(4, 6), 16).toString())
                                 .add(Integer.valueOf(data.substring(2, 4) + data.substring(0, 2), 16).toString())
                                 .add(Integer.valueOf(data.substring(14, 16) + data.substring(12, 14), 16).toString())
                                 .add(Integer.valueOf(data.substring(10, 12) + data.substring(8, 10), 16).toString())
                                 .add(Integer.valueOf(data.substring(22, 24) + data.substring(20, 22), 16).toString())
                                 .add(Integer.valueOf(data.substring(18, 20) + data.substring(16, 18), 16).toString())
                                 .add(Integer.valueOf(data.substring(26, 28) + data.substring(24, 26), 16).toString());

        writer.print(joiner.toString());
    }
    
    /**
     * Write socket data.
     * 
     * @param proto Protocol. tcp or udp.
     * @param data Socket owner data.
     * @param writer PrintWriter to write.
     * @param isIPv4  true if this arguments represent IPv4.
     */
    private void writeSockDataInternal(String proto, String[] data, PrintWriter writer, boolean isIPv4){
        writer.print(sockOwner.contains(data[INDEX_INODE]) ? "jvm\t " : "\t"); // owner
        writer.print(proto + "\t");
        
        String[] queueData = data[INDEX_QUEUE].split(":");
        writer.print(Integer.parseInt(queueData[1], 16)); // Recv-Q
        writer.print("\t");
        writer.print(Integer.parseInt(queueData[0], 16)); // Send-Q
        writer.print("\t");
        
        String[] localAddr = data[INDEX_LOCAL_ADDR].split(":"); // local address
        if(isIPv4){
            writeIPv4(localAddr[0], writer);
        }
        else{
            writeIPv6(localAddr[0], writer);
        }
        writer.print(':');
        writer.print(Integer.parseInt(localAddr[1], 16));
        writer.print("\t");
        
        String[] foreignAddr = data[INDEX_FOREIGN_ADDR].split(":"); // foreign address
        if(isIPv4){
            writeIPv4(foreignAddr[0], writer);
        }
        else{
            writeIPv6(foreignAddr[0], writer);
        }
        writer.print(':');
        writer.print(Integer.parseInt(foreignAddr[1], 16));
        writer.print("\t");
        
        switch(Integer.parseInt(data[INDEX_STATE], 16)){ // connection state
            case 1:
                writer.println("ESTABLISHED");
                break;
            case 2:
                writer.println("SYN_SENT");
                break;
            case 3:
                writer.println("SYN_RECV");
                break;
            case 4:
                writer.println("FIN_WAIT1");
                break;
            case 5:
                writer.println("FIN_WAIT2");
                break;
            case 6:
                writer.println("TIME_WAIT");
                break;
            case 7:
                writer.println("CLOSE");
                break;
            case 8:
                writer.println("CLOSE_WAIT");
                break;
            case 9:
                writer.println("LAST_ACK");
                break;
            case 10:
                writer.println("LISTEN");
                break;
            case 11:
                writer.println("CLOSING");
                break;
            default:
                writer.println("-");
                break;
        }
        
    }
    
    /**
     * Build socket data from archive data.
     * 
     * @throws IOException 
     */
    private void buildSockData() throws IOException{
        Path sockfile = FileSystems.getDefault().getPath(tmpPath.getAbsolutePath(), "socket");
        
        try(PrintWriter writer = new PrintWriter(Files.newOutputStream(sockfile, StandardOpenOption.CREATE))){
            writer.println("Owner\tProto\tRecv-Q\tSend-Q\tLocal Address\tForeign Address\tState");
        
            tcp.stream().map(s -> s.split("\\s+"))
                        .forEach(d -> writeSockDataInternal("tcp", d, writer, true));
            writer.println();

            tcp6.stream().map(s -> s.split("\\s+"))
                        .forEach(d -> writeSockDataInternal("tcp6", d, writer, false));
            writer.println();
            
            udp.stream().map(s -> s.split("\\s+"))
                        .forEach(d -> writeSockDataInternal("udp", d, writer, true));
            writer.println();

            udp6.stream().map(s -> s.split("\\s+"))
                        .forEach(d -> writeSockDataInternal("udp6", d, writer, false));
        }
        
    }
    
    /**
     * Parsing Archive data.
     */
    public void parseArchive(){
        
        if(parsed){
            return;
        }
        
        try(ZipFile archive = new ZipFile(archivePath)){
            archive.stream().forEach(d -> {
                                             switch(d.getName()){

                                                 case "envInfo.txt":
                                                     buildEnvInfo(archive, d);
                                                     break;
                                                     
                                                 case "tcp":
                                                     tcp = buildStringData(archive, d);
                                                     break;
                                                     
                                                 case "tcp6":
                                                     tcp6 = buildStringData(archive, d);
                                                     break;
                                                     
                                                 case "udp":
                                                     udp = buildStringData(archive, d);
                                                     break;
                                                     
                                                 case "udp6":
                                                     udp6 = buildStringData(archive, d);
                                                     break;
                                                     
                                                 case "sockowner":
                                                     sockOwner = buildStringData(archive, d);
                                                     break;
                                                     
                                                 default:
                                                     deflateFileData(archive, d);
                                                     break;
                                             }
                                          });
            
            buildSockData();
        }
        catch (IOException ex) {
            Logger.getLogger(LogController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        parsed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            tmpPath.delete();
        } finally {
            super.finalize();
        }
    }

    /**
     * Getter of this date this archive is created.
     * @return LocalDateTime this archive is created.
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Getter of envInfo.
     * 
     * @return envInfo in this archive.
     */
    public List<AbstractMap.SimpleEntry<String, String>> getEnvInfo() {
        return envInfo;
    }
    
    /**
     * Getter of file list in this archive.
     * This list includes all deflated files in archive.
     * 
     * @return file list in this archive.
     */
    public List<String> getFileList(){
        return Arrays.asList(tmpPath.list());
    }
    
    /**
     * Getter of file contents.
     * Contents is represented as String.
     * 
     * @param file File to be got.
     * @return Contents of file.
     * @throws IOException 
     */
    public String getFileContents(String file) throws IOException{
        Path filePath = FileSystems.getDefault().getPath(tmpPath.getAbsolutePath(), file);
        return Files.readAllLines(filePath).stream()
                                           .collect(StringBuilder::new, (r, s) -> r.append(s).append("\n"), StringBuilder::append)
                                           .toString();
    }
    
}
