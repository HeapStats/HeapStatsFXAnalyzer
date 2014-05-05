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

package jp.co.ntt.oss.heapstats.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * ClassLoader class for loading HeapStatsFXAnalyzer plugin and related libraries.
 * 
 * @author Yasumasa Suenaga
 */
public class PluginClassLoader extends URLClassLoader{

    public PluginClassLoader(URL[] urls) {
        super(urls);
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException{
        
        try{
            return findClass(name);
        }
        catch(ClassNotFoundException e){
            return super.loadClass(name);
        }
        
    }
    
}
