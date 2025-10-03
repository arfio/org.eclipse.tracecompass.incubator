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

package org.eclipse.tracecompass.incubator.concurrentcallstack.core;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStack;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanCalledFunctionFactory;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Concurrent CallStack
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public class SpanCallStack extends CallStack {

    private final ITmfStateSystem fStateSystem;
    private final List<Integer> fQuarks;

    public SpanCallStack(ITmfStateSystem ss, List<@NonNull Integer> quarks, ICallStackElement element, IHostIdProvider hostIdProvider) {
        super(ss, quarks, element, hostIdProvider, null);
        fStateSystem = ss;
        fQuarks = quarks;

    }

    @Override
    public @Nullable ICalledFunction getNextFunction(long time, int depth, @Nullable ICalledFunction parent, IHostModel model, long start, long end) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException("CallStack depth " + depth + " is too large"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        long endTime = (parent == null ? fStateSystem.getCurrentEndTime() : parent.getEnd());
        if (time > endTime || time >= end) {
            return null;
        }
        try {
            ITmfStateInterval interval = fStateSystem.querySingleState(time, fQuarks.get(depth - 1));
            while ((interval.getStateValue().isNull() || interval.getEndTime() < start) && interval.getEndTime() + 1 < endTime) {
                interval = fStateSystem.querySingleState(interval.getEndTime() + 1, fQuarks.get(depth - 1));
            }

            if (!interval.getStateValue().isNull() && interval.getStartTime() < end) {
                return SpanCalledFunctionFactory.create(Math.max(start, interval.getStartTime()), Math.min(end, interval.getEndTime() + 1), interval.getValue(), getSymbolKeyAt(interval.getStartTime()), getThreadId(interval.getStartTime()), parent,
                        model);
            }

        } catch (StateSystemDisposedException e) {

        }
        return null;
    }
}
