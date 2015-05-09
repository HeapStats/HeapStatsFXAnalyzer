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

package jp.co.ntt.oss.heapstats.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

/**
 * Utility class for HeapStats FX Analyzer.
 * @author Yasu
 */
public class HeapStatsUtils {
    
    /* Path of HeapStats home directory. */
    private static Path currentPath = null;
    
    /* Properties for HeapStats analyzer. */
    private static final Properties prop = new Properties();
    
    /* Resource bundle for HeapStats Analyzer. */
    private static ResourceBundle resource;
        
    public static Path getHeapStatsHomeDirectory(){
        
        if(currentPath == null){
            String appJar = Stream.of(System.getProperty("java.class.path").split(System.getProperty("path.separator")))
                                         .filter(s -> s.endsWith("HeapStatsFXAnalyzer.jar"))
                                         .findFirst()
                                         .get();
            currentPath = Paths.get(appJar).toAbsolutePath().getParent();
        }
        
        return currentPath;
    }
    
    /**
     * Load HeapStats property file.
     * @author Yasumasa Suenaga
     * @throws IOException - I/O error to parse property file.
     * @throws HeapStatsConfigException - Invalid value.
     */
    public static void load() throws IOException, HeapStatsConfigException{
        Path properties = Paths.get(getHeapStatsHomeDirectory().toString(), "heapstats.properties");
        
        try(InputStream in = Files.newInputStream(properties, StandardOpenOption.READ)){
            prop.load(in);
        }
        catch(NoSuchFileException e){
            // use default values.
        }
        
        /* Validate values in properties. */
        
        /* Language */
        String language = prop.getProperty("language");
        if(language == null){
            prop.setProperty("language", "en");
        }
        else if(!language.equals("en") && !language.equals("ja")){
            throw new HeapStatsConfigException("Invalid option: language=" + language);
        }

        /* RankLevel */
        String rankLevelStr = prop.getProperty("ranklevel");
        if(rankLevelStr == null){
            prop.setProperty("ranklevel", "5");
        }
        else{
            try{
                Integer.decode(rankLevelStr);
            }
            catch(NumberFormatException e){
                throw new HeapStatsConfigException("Invalid option: ranklevel=" + rankLevelStr, e);
            }
        }
        
        /* ClassRename */
        /* java.lang.Boolean#parseStinrg() does not throws Throwable.
         * If user sets invalid value, Boolean will treat "true" .
         * See javadoc of java.lang.Boolean .
         */
        if(prop.getProperty("replace") == null){
            prop.setProperty("replace", "false");
        }
        
        /* Default directory for dialogs. */
        if(prop.getProperty("defaultdir") == null){
            prop.setProperty("defaultdir", getHeapStatsHomeDirectory().toString());
        }
        
        /* Log file list to parse. */
        if(prop.getProperty("logfile") == null){
            prop.setProperty("logfile", "redhat-release,cmdline,status,smaps,limits");
        }
        
        /* Socket endpoint file to parse. */
        if(prop.getProperty("socketend") == null){
            prop.setProperty("socketend", "tcp,udp,tcp6,udp6");
        }

        /* Socket owner file to parse. */
        if(prop.getProperty("owner") == null){
            prop.setProperty("owner", "sockowner");
        }

        /* Background color to draw graphs. */
        String bgColorStr = prop.getProperty("bgcolor");
        if(bgColorStr == null){
            prop.setProperty("bgcolor", "white");
        }
        else{
            try{
                Color.web(bgColorStr);
            }
            catch(IllegalArgumentException e){
                throw new HeapStatsConfigException("Invalid option: bgcolor=" + bgColorStr, e);
            }
        }
        
        /* HeapStats plugins. */
        String pluginList = prop.getProperty("plugins");
        if(pluginList == null){
            prop.setProperty("plugins", "jp.co.ntt.oss.heapstats.plugin.builtin.log;jp.co.ntt.oss.heapstats.plugin.builtin.snapshot");
        }
        
        /* Load resource bundle */
        resource = ResourceBundle.getBundle("HeapStatsResources", new Locale(prop.getProperty("language")));
        
        /* Add shutdown hook for saving current settings. */
        Runnable savePropImpl = () -> {
                                         try(OutputStream out = Files.newOutputStream(properties, StandardOpenOption.TRUNCATE_EXISTING)){
                                            prop.store(out, null);
                                         }
                                         catch(IOException ex){
                                             Logger.getLogger(HeapStatsUtils.class.getName()).log(Level.SEVERE, null, ex);
                                         }
                                      };
        Runtime.getRuntime().addShutdownHook(new Thread(savePropImpl));
    }
    
    public static List<String> getPlugins(){
        String pluginList = prop.getProperty("plugins");
        
        if(pluginList == null){
            return null;
        }
        else{
            return Arrays.asList(prop.getProperty("plugins").split(";"))
                         .stream()
                         .map(s -> s.trim())
                         .collect(Collectors.toList());                                     
        }
        
    }
    
    public static boolean getReplaceClassName(){
        return Boolean.parseBoolean(prop.getProperty("replace"));
    }
    
    public static String getDefaultDirectory(){
        return prop.getProperty("defaultdir");
    }
    
    public static void setDefaultDirectory(String currentDir){
        
        if(currentDir != null){
            prop.setProperty("defaultdir", currentDir);
        }
        
    }
    
    public static int getRankLevel(){
        return Integer.parseInt(prop.getProperty("ranklevel"));
    }
    
    public static void setRankLevel(int rankLevel){
        prop.setProperty("ranklevel", Integer.toString(rankLevel));
    }
    
    public static String getChartBgColor(){
        return prop.getProperty("bgcolor");
    }
    
    public static String getLanguage(){
        return prop.getProperty("language");
    }
    
    public static ResourceBundle getResourceBundle(){
        return ResourceBundle.getBundle("HeapStatsResources", new Locale(getLanguage()));
    }
    
    /**
     * Convert stack trace to String.
     * @param e Throwable object to convert.
     * 
     * @return String result of e.printStackTrace()
     */
    public static String stackTarceToString(Throwable e){
        String result = null;
        
        try(StringWriter strWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(strWriter)){
            e.printStackTrace(printWriter);
            result = strWriter.toString();
        }
        catch(IOException ex) {
            Logger.getLogger(HeapStatsUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    /**
     * Show alert dialog.
     * 
     * @param e Throwable instance to show.
     */
    public static void showExceptionDialog(Throwable e){
        TextArea details = new TextArea(HeapStatsUtils.stackTarceToString(e));
        details.setEditable(false);
        
        Alert dialog = new Alert(Alert.AlertType.ERROR);
        dialog.setTitle("Error");
        dialog.setHeaderText(e.getLocalizedMessage());
        dialog.getDialogPane().setExpandableContent(details);
        dialog.showAndWait();
    }

}
