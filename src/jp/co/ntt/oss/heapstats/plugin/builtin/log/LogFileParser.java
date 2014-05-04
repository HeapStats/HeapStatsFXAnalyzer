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

package jp.co.ntt.oss.heapstats.plugin.builtin.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.model.DiffData;
import jp.co.ntt.oss.heapstats.plugin.builtin.log.model.LogData;

/**
 * HeapStats log file (CSV) parser.
 * @author Yasu
 */
public class LogFileParser extends Task<Void>{
    
    private final List<LogData> logEntries;
    
    private final List<DiffData> diffEntries;
    
    private final List<File> fileList;
    
    /**
     * Constructor of LogFileParser.
     * 
     * @param fileList List of log to be parsed.
     */
    public LogFileParser(List<File> fileList){
        logEntries = new ArrayList<>();
        diffEntries = new ArrayList<>();
        this.fileList = fileList;
    }
    
    /**
     * This method addes log value from CSV.
     * 
     * @param csvLine CSV data to be added. This value must be 1-raw (1-record).
     * @param logdir Log directory. This value is used to store log value.
     */
    private void addEntry(String csvLine, String logdir){
        LogData element = new LogData();
        
        try{
            element.parseFromCSV(csvLine, logdir);
        }
        catch(IllegalArgumentException ex){
            Logger.getLogger(LogFileParser.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        logEntries.add(element);
    }
    
    /**
     * Parse log file.
     * 
     * @param logfile Log to be parsedd.
     */
    protected void parse(String logfile){
        String logdir = FileSystems.getDefault()
                                   .getPath(logfile)
                                   .getParent()
                                   .toString();
        
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(logfile))){
            reader.lines().forEach(s -> addEntry(s, logdir));
        }
        catch (IOException ex){
            Logger.getLogger(LogFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    @Override
    protected Void call() throws Exception {
        
        /* Parse log files */
        fileList.stream()
                .map(File::getAbsolutePath)
                .forEach(f -> parse(f));
        
        /* Sort log files order by date&time */
        logEntries.sort(Comparator.naturalOrder());

        /*
         * Calculate diff data
         * Difference data needs 2 elements at least.
         *
         *  1. Skip top element in logEntries.
         *  2. Pass top element in logEntries to reduce() as identity value.
         *  3. Calculate difference data in reduce().
         *  4. Return current element in logEntries. That value passes next
         *     calculation in reduce().
         */
        logEntries.stream()
                  .skip(1)
                  .reduce(logEntries.get(0), (x, y) -> {
                                                          diffEntries.add(new DiffData(x, y));
                                                          return y;
                                                       });
        
        super.succeeded();
        return null;
    }

    /**
     * Returns log entries of resulting on this task.
     * @return results of this task.
     */
    public List<LogData> getLogEntries() {
        return logEntries;
    }

    /**
     * Returns diff entries of resulting on this task.
     * 
     * @return results of this task.
     */
    public List<DiffData> getDiffEntries() {
        return diffEntries;
    }
    
}
