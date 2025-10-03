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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;

/**
 * This class defines the required performance parameters for each span
 *
 * @author Maryam Ekhlasi
 *
 */
public class Span implements INamedSegment {

    private static final long serialVersionUID = 1234562136875329857L;
    /**
     * buffer for the sequences of the column in the segment store table
     */
    public static final IHTIntervalReader<@NonNull ISegment> READER = buffer -> new Span(buffer.getLong(), buffer.getLong(), buffer.getString(), buffer.getString(), buffer.getString(), buffer.getString(), buffer.getString());
    private final long fStartTime;
    private final long fEndTime;
    private final String fOperationName;
    private final String fTid;
    private final String fTraceid;
    private final String fSid;
    private final String fPid;
    private KernelMetrics fmetrics;

    /**
     * Constructor
     *
     * @param info
     *            object for the based variables
     * @param endTime
     *            of span
     */
    public Span(
            InitialInfo info,
            long endTime) {
        fStartTime = info.fStartTime;
        fOperationName = info.fOperationName;
        fEndTime = endTime;
        fTid = info.fTid;
        fTraceid = info.fTraceid;
        fSid = info.fSid;
        fPid = info.fPid;
    }

    private Span(long startTime, long endTime, String operationName, String tid, String traceid, String sid, String pid) {
        fStartTime = startTime;
        fEndTime = endTime;
        fOperationName = operationName;
        fTid = tid;
        fTraceid = traceid;
        fSid = sid;
        fPid = pid;
    }

    @Override
    public long getStart() {
        return fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

    /**
     * Get the name of the span
     *
     * @return Name
     */
    @SuppressWarnings("null")
    @Override
    public String getName() {
        return fOperationName;
    }

    /**
     * Get the thread ID for this span
     *
     * @return The ID of the thread
     */
    public String getTid() {
        return fTid;
    }

    /**
     * Get the trace ID for this span
     *
     * @return The ID of the trace
     */
    public String getTraceid() {
        return fTraceid;
    }

    /**
     * Get span ID
     *
     * @return ID of span
     */
    public String getSid() {
        return fSid;
    }

    /**
     * Get Parent ID
     *
     * @return ID of parent
     */
    public String getPid() {
        return fPid;
    }

    /**
     * Get kernel metrics parameters per span
     *
     * @return KernelMetrics parameters
     */
    public KernelMetrics getMetrics() {
        return fmetrics;
    }


    /**
     * @param metrics
     *            related to kernel
     */
    public void setMetrics(KernelMetrics metrics) {
        fmetrics = metrics;
    }

    @SuppressWarnings("null")
    @Override
    public int getSizeOnDisk() {
        return 8 * Long.BYTES + SafeByteBufferFactory.getStringSizeInBuffer(fOperationName) + 8 * Long.BYTES;
    }

    @SuppressWarnings("null")
    @Override
    public void writeSegment(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putLong(fStartTime);
        buffer.putLong(fEndTime);
        buffer.putString(fOperationName);
        buffer.putString(fTid);
        buffer.putString(fTraceid);
        buffer.putString(fPid);
        buffer.putString(fSid);
    }

    @Override
    public int compareTo(@NonNull ISegment o) {
        int ret = INamedSegment.super.compareTo(o);
        if (ret != 0) {
            return ret;
        }
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return "Start Time = " + getStart() + //$NON-NLS-1$
                "; End Time = " + getEnd() + //$NON-NLS-1$
                "; Duration = " + getLength() + //$NON-NLS-1$
                "; Operation Name = " + getName() + //$NON-NLS-1$
                "; Span ID = " + getSid() + //$NON-NLS-1$
                "; Thread ID = " + getTid() + //$NON-NLS-1$
                "; Trace ID = " + getTraceid() + //$NON-NLS-1$
                "; Parent ID = " + getPid(); //$NON-NLS-1$
    }

    /**
     * Initial information of the span
     */
    public static class InitialInfo {

        private long fStartTime;
        private String fOperationName;
        private String fTid;
        private String fTraceid;
        private String fSid;
        private String fPid;

        /**
         * @param startTime
         *            Start time of the span
         * @param operationName
         *            Name of the operation
         * @param tid
         *            The TID of the thread
         * @param traceid
         *            The traceid of each trace
         * @param sid
         *            Span Id
         * @param pid
         *            Parent Id
         */
        public InitialInfo(
                long startTime,
                String operationName,
                String tid,
                String traceid,
                String sid,
                String pid) {

            fStartTime = startTime;
            fOperationName = operationName.intern();
            fTid = tid;
            fTraceid = traceid;
            fSid = sid;
            fPid = pid;
        }
    }



}
