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

import java.time.LocalDateTime;

/**
 * Container class for difference data (e.g. CPU usage, Safepoint time).
 * @author Yasumasa Suenaga
 */
public class DiffData {
    
    private final LocalDateTime dateTime;
    
    private double javaUserUsage;
    
    private double javaSysUsage;
    
    private double cpuUserUsage;
    
    private double cpuNiceUsage;
    
    private double cpuSysUsage;
    
    private double cpuIdleUsage;
    
    private double cpuIOWaitUsage;
    
    private double cpuIRQUsage;
    
    private double cpuSoftIRQUsage;
    
    private double cpuStealUsage;
    
    private double cpuGuestUsage;
    
    private long jvmSyncPark;
    
    private long jvmSafepointTime;
    
    private long jvmSafepoints;
    
    private final boolean minusData;
    
    /**
     * Constructor of DiffData.
     * Each fields is based on "current - prev" .
     * 
     * @param prev
     * @param current 
     */
    public DiffData(LogData prev, LogData current){
        dateTime = current.getDateTime();
        
        /* Java CPU usage */
        double javaUserTime = current.getJavaUserTime() - prev.getJavaUserTime();
        double javaSysTime = current.getJavaSysTime() - prev.getJavaSysTime();
        double javaCPUTotal = javaUserTime + javaSysTime;
        javaUserUsage = javaUserTime / javaCPUTotal * 100.0d;
        javaSysUsage = javaSysTime / javaCPUTotal * 100.0d;
        
        /* System CPU usage */
        double systemUserTime = current.getSystemUserTime() - prev.getSystemUserTime();
        double systemNiceTime = current.getSystemNiceTime() - prev.getSystemNiceTime();
        double systemSysTime = current.getSystemSysTime() - prev.getSystemSysTime();
        double systemIdleTime = current.getSystemIdleTime() - prev.getSystemIdleTime();
        double systemIOWaitTime = current.getSystemIOWaitTime() - prev.getSystemIOWaitTime();
        double systemIRQTime = current.getSystemIRQTime() - prev.getSystemIRQTime();
        double systemSoftIRQTime = current.getSystemSoftIRQTime() - prev.getSystemSoftIRQTime();
        double systemStealTime = current.getSystemStealTime() - prev.getSystemStealTime();
        double systemGuestTime = current.getSystemGuestTime() - prev.getSystemGuestTime();
        double systemCPUTotal = systemUserTime + systemNiceTime + systemSysTime +
                                systemIdleTime + systemIOWaitTime + systemIRQTime +
                                systemSoftIRQTime + systemStealTime + systemGuestTime;
        cpuUserUsage    = systemUserTime / systemCPUTotal * 100.0d;
        cpuNiceUsage    = systemNiceTime / systemCPUTotal * 100.0d;
        cpuSysUsage     = systemSysTime / systemCPUTotal * 100.0d;
        cpuIdleUsage    = systemIdleTime / systemCPUTotal * 100.0d;
        cpuIOWaitUsage  = systemIOWaitTime / systemCPUTotal * 100.0d;
        cpuIRQUsage     = systemIRQTime / systemCPUTotal * 100.0d;
        cpuSoftIRQUsage = systemSoftIRQTime / systemCPUTotal * 100.0d;
        cpuStealUsage   = systemStealTime / systemCPUTotal * 100.0d;
        cpuGuestUsage   = systemGuestTime / systemCPUTotal * 100.0d;
        
        /* JVM statistics */
        jvmSyncPark      = current.getJvmSyncPark() - prev.getJvmSyncPark();
        jvmSafepointTime = current.getJvmSafepointTime() - prev.getJvmSafepointTime();
        jvmSafepoints    = current.getJvmSafepoints() - prev.getJvmSafepoints();
        
        minusData = (systemUserTime < 0.0d) || (systemNiceTime < 0.0d) ||
                    (systemSysTime < 0.0d) || (systemIdleTime < 0.0d) ||
                    (systemIOWaitTime < 0.0d) || (systemIRQTime < 0.0d) ||
                    (systemSoftIRQTime < 0.0d) || (systemStealTime < 0.0d) ||
                    (systemGuestTime < 0.0d) ||
                    (jvmSyncPark < 0) || (jvmSafepointTime < 0) || (jvmSafepoints < 0);

        /*
         * If this data have minus entry, it is suspected reboot.
         * So I clear to all fields.
         */
        if(minusData){
            javaUserUsage   = 0.0d;
            javaSysUsage    = 0.0d;
            cpuUserUsage    = 0.0d;
            cpuNiceUsage    = 0.0d;
            cpuSysUsage     = 0.0d;
            cpuIdleUsage    = 0.0d;
            cpuIOWaitUsage  = 0.0d;
            cpuIRQUsage     = 0.0d;
            cpuSoftIRQUsage = 0.0d;
            cpuStealUsage   = 0.0d;
            cpuGuestUsage   = 0.0d;
            jvmSyncPark      = 0;
            jvmSafepointTime = 0;
            jvmSafepoints    = 0;
        }
        
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public double getJavaUserUsage() {
        return javaUserUsage;
    }

    public double getJavaSysUsage() {
        return javaSysUsage;
    }

    public double getCpuUserUsage() {
        return cpuUserUsage;
    }

    public double getCpuNiceUsage() {
        return cpuNiceUsage;
    }

    public double getCpuSysUsage() {
        return cpuSysUsage;
    }

    public double getCpuIdleUsage() {
        return cpuIdleUsage;
    }

    public double getCpuIOWaitUsage() {
        return cpuIOWaitUsage;
    }

    public double getCpuIRQUsage() {
        return cpuIRQUsage;
    }

    public double getCpuSoftIRQUsage() {
        return cpuSoftIRQUsage;
    }

    public double getCpuStealUsage() {
        return cpuStealUsage;
    }

    public double getCpuGuestUsage() {
        return cpuGuestUsage;
    }

    public long getJvmSyncPark() {
        return jvmSyncPark;
    }

    public long getJvmSafepointTime() {
        return jvmSafepointTime;
    }

    public long getJvmSafepoints() {
        return jvmSafepoints;
    }
    
    public double getCpuTotalUsage(){
        return cpuUserUsage + cpuNiceUsage + cpuSysUsage + cpuIOWaitUsage +
               cpuIRQUsage + cpuSoftIRQUsage + cpuStealUsage + cpuGuestUsage;
    }
    
    public static double getCPUTotalUsage(DiffData instance){
        return instance.getCpuTotalUsage();
    }
    
    public boolean hasMinusData(){
        return minusData;
    }
    
}
