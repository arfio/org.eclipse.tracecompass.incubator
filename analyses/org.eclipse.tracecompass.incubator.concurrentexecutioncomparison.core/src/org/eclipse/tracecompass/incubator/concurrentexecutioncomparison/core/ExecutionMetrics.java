/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAggregatedCalledFunction;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * The required information we need for differential flame graph view
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public final class ExecutionMetrics implements ISegment {

    private static final long serialVersionUID = 1554494342105208730L;

    /**
     * The subset of information that is available from a callgraph .
     */
    private final long fStartTime;
    private final long fEndTime;
    private long Duration;
    private long SelfTime;
    private SpanAggregatedCalledFunction element;
    private String processName;
    private int hashcode;

    public ExecutionMetrics(long StartTime, long endTime, long duration, long selftime, SpanAggregatedCalledFunction process, String processName) {

        this.Duration = duration;
        this.SelfTime = selftime;
        this.fStartTime = StartTime;
        this.fEndTime = endTime;
        this.element = process;
        this.processName = processName;
        this.hashcode = Objects.hash(Duration + SelfTime + fStartTime + fEndTime);

    }

    public long getDuration() {
        return this.Duration;
    }

    public long getSelfTime() {
        return this.SelfTime;
    }

    public String getProcessName() {
        return this.processName;
    }

    public SpanAggregatedCalledFunction getProcess() {
        return this.element;
    }

    @Override
    public int compareTo(@NonNull ISegment o) {
        int ret = ISegment.super.compareTo(o);
        if (ret != 0) {
            return ret;
        }
        return toString().compareTo(o.toString());

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutionMetrics that = (ExecutionMetrics) o;
        return this.Duration == that.getDuration();
    }

    @Override
    public String toString() {
        return "Process = " + getProcessName() + //$NON-NLS-1$
                "; Duration = " + getDuration() + //$NON-NLS-1$
                "; SelfTime = " + getSelfTime();//$NON-NLS-1$

    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    @Override
    public long getStart() {
        return fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

}
