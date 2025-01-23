/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmCallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Handler for All APIs (HSA and HIP).
 *
 * This handler handles all API calls that are written to call stacks and are
 * also used to provide other information for the GPU operations.
 *
 * @author Arnaud Fiorini
 */
public class ApiEventHandler implements IRocmEventHandler {

    private static final String HIP = "HIP"; //$NON-NLS-1$
    private static final String HSA = "HSA"; //$NON-NLS-1$
    private static final Integer MAX_ENTRIES = 2000;
    private static Long fSmallestCorrelationId = 0L;
    private final Map<Long, ITmfEvent> fCorrelationCache = new HashMap<>(MAX_ENTRIES+1, .75F) {
        private static final long serialVersionUID = 6811157191976376628L;

        // This method is called just after a new entry has been added
        @SuppressWarnings("null")
        public ITmfEvent put(Long key, ITmfEvent value) {
            while (size() > MAX_ENTRIES) {
                super.remove(fSmallestCorrelationId++);
            }
            return super.put(key, value);
        }
    };

    private static void provideThreadIdIfNotProvided(ITmfEvent event, ITmfStateSystemBuilder ssb, int quark, RocmEventLayout layout) {
        if (ssb.queryOngoing(quark) == null) {
            Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldThreadId());
            ssb.modifyAttribute(event.getTimestamp().getValue(), tid, quark);
        }
    }

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) {
        Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldThreadId());
        if (tid == null) {
            return;
        }
        int rootQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.ROOT, CallStackStateProvider.PROCESSES);
        int processQuark = ssb.getQuarkRelativeAndAdd(rootQuark, tid.toString());
//        addEventToOperationQueue(event, ssb, layout);
        boolean isEndEvent = false;

        int callStackQuark = ITmfStateSystem.INVALID_ATTRIBUTE;
        if (event.getName().startsWith(layout.getHipPrefix())) {
            int apiQuark = ssb.getQuarkRelativeAndAdd(processQuark, HIP);
            callStackQuark = ssb.getQuarkRelativeAndAdd(apiQuark, InstrumentedCallStackAnalysis.CALL_STACK);
            isEndEvent = event.getName().endsWith(layout.getHipEndSuffix());
            if (!isEndEvent) {
                addEventToCorrelationCache(event, layout);
            }
            provideThreadIdIfNotProvided(event, ssb, apiQuark, layout);

        } else if (event.getName().startsWith(layout.getHsaPrefix())) {
            if (event.getName().equals(layout.getHsaHandleType())) {
                return;
            }
            int apiQuark = ssb.getQuarkRelativeAndAdd(processQuark, HSA);
            callStackQuark = ssb.getQuarkRelativeAndAdd(apiQuark, InstrumentedCallStackAnalysis.CALL_STACK);
            isEndEvent = event.getName().endsWith(layout.getHsaEndSuffix());
            provideThreadIdIfNotProvided(event, ssb, apiQuark, layout);
        }
        if (isEndEvent) {
            ssb.popAttribute(event.getTimestamp().getValue(), callStackQuark);
            return;
        }
        // Trimming the begin out of the event name
        String eventName = event.getName().startsWith(layout.getHipPrefix()) ? event.getName().substring(0, event.getName().length() - layout.getHipBeginSuffix().length())
                : event.getName().substring(0, event.getName().length() - layout.getHsaBeginSuffix().length());
        ssb.pushAttribute(event.getTimestamp().getValue(), eventName, callStackQuark);
    }

    private void addEventToCorrelationCache(ITmfEvent event, RocmEventLayout layout) {
        if (layout.isLinkedToOperation(event.getName())) {
            Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
            if (correlationId == null) {
                return;
            }
            fCorrelationCache.put(correlationId, event);
        }
    }

    public @Nullable ITmfEvent getEventFromCorrelationCache(Long correlationId) {
        return fCorrelationCache.remove(correlationId);
    }

/*
    private static boolean isLinkedToGpuOperationStartEvent(ITmfEvent event, RocmEventLayout layout) {
        return event.getName().equals(layout.hipStreamSynchronizeBegin()) || event.getName().equals(layout.hipStreamWaitEventBegin());
    }

    private static boolean isLinkedToGpuOperationEndEvent(ITmfEvent event, RocmEventLayout layout) {
        return event.getName().equals(layout.hipStreamSynchronizeEnd()) || event.getName().equals(layout.hipStreamWaitEventEnd());
    }

    private static void addEventToOperationQueue(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) {
        Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
        if (correlationId == null) {
            return;
        }
        int operationsQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.HIP_OPERATION_QUEUES);
        long ts = event.getTimestamp().getValue();

        if (layout.isMemcpyBegin(event.getName()) || (event.getName().equals(layout.hipLaunchKernelBegin()) && ((RocmCtfPluginTrace) event.getTrace()).isContainingKernelGpuActivity())
                || isLinkedToGpuOperationStartEvent(event, layout)) {
            int depth = 1;
            int subQuark = ssb.getQuarkRelativeAndAdd(operationsQuark, String.valueOf(depth));
            // While there is already activity on the quark
            while (!ssb.queryOngoingState(subQuark).isNull()) {
                depth += 1;
                subQuark = ssb.getQuarkRelativeAndAdd(operationsQuark, String.valueOf(depth));
            }
            // Register event tid in the call stack
            int tidQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.TID);
            ssb.modifyAttribute(ts, event.getContent().getFieldValue(Integer.class, layout.fieldThreadId()), tidQuark);
            // Register event name in the call stack
            ssb.modifyAttribute(ts, correlationId, subQuark);
            int nameQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.NAME);
            if (layout.isMemcpyBegin(event.getName())) {
                ssb.modifyAttribute(ts, event.getContent().getFieldValue(String.class, layout.fieldMemcpyKind()), nameQuark);
            } else if (event.getName().equals(layout.hipLaunchKernelBegin())) {
                ssb.modifyAttribute(ts, event.getContent().getFieldValue(String.class, layout.fieldKernelName()), nameQuark);
            } else if (event.getName().equals(layout.hipStreamSynchronizeBegin())) {
                ssb.modifyAttribute(ts, event.getName().substring(0, event.getName().length() - layout.getHipBeginSuffix().length()), nameQuark);
            }
        }
        if (isLinkedToGpuOperationEndEvent(event, layout)) {
            int depth = 1;
            int subQuark;
            try {
                subQuark = ssb.getQuarkRelative(operationsQuark, String.valueOf(depth));
                // While there is already activity on the quark
                while (correlationId != ssb.queryOngoingState(subQuark).unboxLong()) {
                    depth += 1;
                    subQuark = ssb.optQuarkRelative(operationsQuark, String.valueOf(depth));
                    if (subQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                        return;
                    }
                }
                ssb.modifyAttribute(ts, null, subQuark);
                int nameQuark = ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.NAME);
                ssb.modifyAttribute(ts, null, nameQuark);
                int tidQuark = ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.TID);
                ssb.modifyAttribute(ts, null, tidQuark);
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            }
        }
    }*/
}
