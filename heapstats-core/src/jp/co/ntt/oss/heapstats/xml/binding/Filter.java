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

package jp.co.ntt.oss.heapstats.xml.binding;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Filter class in XML filter.
 * 
 * @author Yasumasa Suenaga
 */
public class Filter {
    
    @XmlAttribute(name="name")
    private String name;
    
    private BooleanProperty hide;
    
    @XmlElement(name="classes")
    private Classes classes;

    public Filter() {
        hide = new SimpleBooleanProperty(false);
    }

    public String getName() {
        return name;
    }

    public boolean isHide() {
        return hide.get();
    }

    public Classes getClasses() {
        return classes;
    }

    @XmlElement(name="hide")
    public void setHide(boolean visible) {
        this.hide.set(visible);
    }

    public BooleanProperty hideProperty() {
        return hide;
    }

}
