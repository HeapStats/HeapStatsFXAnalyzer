/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.ntt.oss.heapstats.plugin.builtin.jvmlive.jdp;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Yasu
 */
public class JdpTableKeyValue {
    
    private final StringProperty key;
    
    private final ObjectProperty value;
    
    public JdpTableKeyValue(String key, Object value){
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleObjectProperty(value);
    }
    
    public StringProperty keyProperty(){
        return key;
    }
    
    public ObjectProperty valueProperty(){
        return value;
    }
    
}
