/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.spanmetrics.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;



/**
 * This class is responsible for mapping the state values to the meaningful
 * state names.
 *
 * @author Maryam Ekhlasi
 */
public class KernelMetrics {

    Map<Integer, Long> fmetricsMap;
    EnumMap<StateName, Integer> fstateNames;
    EnumMap<SyscallName, Integer> fsyscallNames;
    Map<String, Long> fsyscallMap;



    /**
     * Enums related to each kernel state
     */
    public enum StateName {
        UNKNOWN, WAITFORK, WAITCPU, EXIT, ZOMBIE, WAITBLOCKED, RUN, NOTALIVE, RUNSYSTEMCALL, INTERRUPTED, WAITUNKNOWN
    }

    public enum SyscallName {
        getrlimit, select, sendto, sigaltstack, getresgid, newfstatat, bind, set_tid_address, clock_gettime, brk,
        rt_sigprocmask, write, mkdir, getegid, pipe2, inotify_add_watch, pselect6, sync_file_range, setgid, execve,
        getgid, socketpair, timerfd_create, recvfrom, rt_sigtimedwait, futex, epoll_pwait, accept, setsid, getsid,
        setresgid, getpgid, nanosleep, dup2, openat, flock, umask, access, fcntl, setpriority,
        getsockopt, syslog, setsockopt, unknown, recvmsg, epoll_create1, mknod, epoll_create, getpgrp, getgroups,
        epoll_wait, close, fdatasync, connect, newfstat, pwrite64, rt_sigaction, fchmod, kill, rt_sigpending,
        getpeername, ppoll, munmap, getpriority, rename, getresuid, ftruncate, sched_getaffinity, get_mempolicy, prctl,
        poll, fstatfs, listen, setgroups, sysinfo, timer_create, pipe, unlinkat, tgkill, read,
        getrusage, getsockname, setresuid, sched_yield, faccessat, newuname, renameat, chdir, geteuid, setuid,
        getuid, mprotect, pread64, timerfd_settime, shutdown, epoll_ctl, prlimit64, lseek, readlink, timer_settime,
        eventfd2, ioctl, madvise, fadvise64, fsync, getcwd, times, getdents64, alarm, chmod,
        accept4, getppid, wait4, splice, fchown, setpgid, statfs, fallocate, clock_nanosleep, newlstat,
        gettid, mkdirat, unlink, setitimer, clone, readlinkat, setrlimit, sendmsg, utimensat, socket,
        set_robust_list, mmap, getpid, newstat, dup, writev,
    }

    /**
     * Construction, initiating the maps
     */
    public KernelMetrics() {
        fmetricsMap = new HashMap<>();
        fstateNames = new EnumMap<>(StateName.class);
        fsyscallMap = new HashMap<>();

        fstateNames.put(StateName.UNKNOWN, 0);
        fstateNames.put(StateName.WAITBLOCKED, 1);
        fstateNames.put(StateName.RUN, 2);
        fstateNames.put(StateName.RUNSYSTEMCALL, 3);
        fstateNames.put(StateName.INTERRUPTED, 4);
        fstateNames.put(StateName.WAITCPU, 5);
        fstateNames.put(StateName.WAITUNKNOWN, 6);
        fstateNames.put(StateName.WAITFORK, 7);
    }

    /**
     * Evaluate the duration time of each kernel state
     *
     * @param stateValue
     *            the value of each state
     * @param startTime
     *            of the span
     * @param endTime
     *            of the span
     */

    public void setStateDurationTime(int stateValue, long startTime, long endTime) {

        long value = this.fmetricsMap.computeIfAbsent(stateValue, k -> 0L);
        value += endTime - startTime;
        this.fmetricsMap.put(stateValue, value);
    }


    public void setSysCallDurationTime(String stateValue, long startTime, long endTime){

        if (this.fsyscallMap.containsKey(stateValue)) {
            // Key exists, update the value
            Long oldValue = fsyscallMap.get(stateValue);
            if (oldValue != null) {
                // Key exists, update the value
                this.fsyscallMap.put(stateValue, oldValue + (endTime - startTime));
            } else {
                // Key doesn't exist or value is null, add new key-value pair
                this.fsyscallMap.put(stateValue, endTime - startTime);
            }
        } else {
            // Key doesn't exist, add new key-value pair
            this.fsyscallMap.put(stateValue, endTime - startTime);
        }
        System.out.println(stateValue);
    }

    public long getStateValue(StateName statename) {

        Integer stateID = fstateNames.get(statename);
        return this.fmetricsMap.getOrDefault(stateID, 0L);
    }

    /**
     * Extract the duration time
     *
     * @param statename
     *            based on its value
     * @return the duration time of each state
     */
    /*public long getSysCallValue(SyscallName syscallName) {

        if (this.fsyscallMap == null || !this.fsyscallMap.containsKey(syscallName.toString())) {
            return 0L;
        }
        return this.fsyscallMap.get(syscallName.toString());
    } */
    public long getSysCallValue(SyscallName syscallName) {
        if (this.fsyscallMap == null || syscallName == null || !this.fsyscallMap.containsKey(syscallName.toString())) {
            return 0L;
        }
        Long value = this.fsyscallMap.get(syscallName.toString());
        return value != null ? value : 0L;
    }


    /**
     * Extract metrics from hashmap
     *
     * @return fmetricsMap
     */
    public Map<Integer, Long> getMetrics() {
        return Collections.unmodifiableMap(this.fmetricsMap);
    }
}