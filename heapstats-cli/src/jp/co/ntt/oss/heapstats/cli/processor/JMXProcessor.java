/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.ntt.oss.heapstats.cli.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;
import javax.management.MalformedObjectNameException;
import jp.co.ntt.oss.heapstats.cli.Options;
import jp.co.ntt.oss.heapstats.jmx.JMXHelper;

/**
 *
 * @author Yasu
 */
public class JMXProcessor implements CliProcessor{
    
    private final Options options;

    public JMXProcessor(Options options) {
        this.options = options;
    }
    
    @Override
    public void process() {
        try(JMXHelper jmx = new JMXHelper(options.getJmxURL())){
            switch(options.getMode()){
                case JMX_GET_VERSION:
                    System.out.println(jmx.getMbean().getHeapStatsVersion());
                    break;
                case JMX_GET_SNAPSHOT:
                    jmx.getSnapShot(options.getFile().get(0));
                    System.out.println("SnapShot saved to " + options.getFile().get(0));
                    break;
                case JMX_GET_LOG:
                    jmx.getResourceLog(options.getFile().get(0));
                    System.out.println("Resource log saved to " + options.getFile().get(0));
                    break;
                case JMX_GET_CONFIG:
                    System.out.println(jmx.getMbean().getConfiguration(options.getConfigKey()));
                    break;
                case JMX_CHANGE_CONFIG:
                    System.out.print(options.getConfigKey() + ": " + jmx.getMbean().getConfiguration(options.getConfigKey()) + " -> ");
                    jmx.changeConfigurationThroughString(options.getConfigKey(), options.getNewConfigValue());
                    System.out.println(jmx.getMbean().getConfiguration(options.getConfigKey()));
                    break;
                case JMX_INVOKE_SNAPSHOT:
                    jmx.getMbean().invokeSnapShotCollection();
                    break;
                case JMX_INVOKE_LOG:
                    jmx.getMbean().invokeLogCollection();
                    break;
                case JMX_INVOKE_ALL_LOG:
                    jmx.getMbean().invokeAllLogCollection();
                    break;
            }
            
        }
        catch(IOException ex){
            throw new UncheckedIOException(ex);
        } catch (MalformedObjectNameException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
