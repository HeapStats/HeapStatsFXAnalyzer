/*
 * Copyright (C) 2015 Yasumasa Suenaga
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
    
    private final ObjectProperty<Object> value;
    
    public JdpTableKeyValue(String key, Object value){
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleObjectProperty<>(value);
    }
    
    public StringProperty keyProperty(){
        return key;
    }
    
    public ObjectProperty<Object> valueProperty(){
        return value;
    }
    
}
