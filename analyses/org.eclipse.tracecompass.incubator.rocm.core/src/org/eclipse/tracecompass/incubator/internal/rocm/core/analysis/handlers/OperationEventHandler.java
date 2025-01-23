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

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.EdgeStateValue;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmCallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Handles GPU operations from HIP calls.
 *
 * It uses state system information from the ApiEventHandler to access the
 * operation name
 *
 * @author Arnaud Fiorini
 */
public class OperationEventHandler implements IRocmEventHandler {

    private static final String QUEUES = "Queues"; //$NON-NLS-1$
    private static final String ROCM_AGENT = "ROCm Agent "; //$NON-NLS-1$
    private static final String QUEUE = "Queue "; //$NON-NLS-1$
//    private static final String UNKNOWN = "Unknown Operation"; //$NON-NLS-1$

    private final ApiEventHandler fApiHandler;

    public OperationEventHandler(ApiEventHandler apiHandler) {
        fApiHandler = apiHandler;
    }

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) {
        Long timestamp = event.getTimestamp().toNanos();
        boolean isHipOperationBegin = event.getName().equals(layout.getHipOperationBegin());
        boolean isHipOperationEnd = event.getName().equals(layout.getHipOperationEnd());
        if (isHipOperationBegin || isHipOperationEnd) {
            /*if (isHipOperationBegin) {
                try {
                    operationName = event.getContent().getFieldValue(String.class, layout.fieldOperationName());
                    String operationNameCorrelation = getCorrespondingHipCall(event, ssb, layout);
                    operationName = operationName == "" ? operationNameCorrelation : operationName;
                } catch (AttributeNotFoundException e) {
                    Activator.getInstance().logError(e.getMessage());
                }
            }*/
            // Create or find call stack quark
            Integer agentId = event.getContent().getFieldValue(Integer.class, layout.fieldAgentId());
            Integer queueId = event.getContent().getFieldValue(Integer.class, layout.fieldQueueId());
            Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
            if (agentId == null || queueId == null || correlationId == null) {
                return;
            }
            int rootQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.ROOT, QUEUES);
            int agentQuark = ssb.getQuarkRelativeAndAdd(rootQuark, ROCM_AGENT + agentId.toString());
            int queueQuark = ssb.getQuarkRelativeAndAdd(agentQuark, QUEUE + queueId.toString());
            if (ssb.queryOngoing(queueQuark) == null) {
                ssb.modifyAttribute(event.getTimestamp().getValue(), getGpuQueueId(agentId, queueId), queueQuark);
//                ssb.modifyAttribute(event.getTimestamp().getValue(), getGpuQueueId(agentId, queueId), agentQuark);
            }
            int callStackQuark = ssb.getQuarkRelativeAndAdd(queueQuark, InstrumentedCallStackAnalysis.CALL_STACK);

            // Add the operation to the queue if we are treating a begin event
            if (isHipOperationBegin) {
                int depth = 1;
                int subQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(depth));
                // While there is already activity on the quark
                while (!ssb.queryOngoingState(subQuark).isNull()) {
                    depth += 1;
                    subQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(depth));
                }
                // Register event name in the call stack
                ITmfEvent srcEvent = fApiHandler.getEventFromCorrelationCache(correlationId);
                if (srcEvent != null) {
                    Integer tid = srcEvent.getContent().getFieldValue(Integer.class, layout.fieldThreadId());
                    addArrows(ssb, (tid != null ? tid : 0), srcEvent.getTimestamp().getValue(), event, getGpuQueueId(agentId, queueId));
                } else {
                    Activator.getInstance().logWarning("Not found correlation for id: " + correlationId);
                }
                String operationName = getOperationNameFromSrcEvent(event, srcEvent, layout);
                ssb.modifyAttribute(timestamp, operationName + ":" + correlationId.toString(), subQuark);
                // Set call stack depth
                ssb.modifyAttribute(timestamp, depth, callStackQuark);
                // Set correlation id
                int correlationIdQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.CORRELATION_ID);
                ssb.modifyAttribute(timestamp, correlationId, correlationIdQuark);
                // Else if we have an end event, move all operations after the
                // one we received up.
            } else {
                int depth = 1;
                int maxDepth = ssb.queryOngoingState(callStackQuark).unboxInt();
                int subQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(depth));
                try {
                    // While there is already activity on the quark, go through
                    // each level
                    while (ssb.queryOngoingState(ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.CORRELATION_ID)).unboxLong() != correlationId) {
                        depth += 1;
                        subQuark = ssb.getQuarkRelative(callStackQuark, String.valueOf(depth));
                    }

                    int previousQuark = -1;
                    while (!ssb.queryOngoingState(subQuark).isNull()) {
                        if (depth >= maxDepth) {
                            ssb.modifyAttribute(timestamp, null, subQuark);
                            ssb.modifyAttribute(timestamp, depth - 1, callStackQuark);
                            break;
                        }
                        // Getting previous quark
                        previousQuark = subQuark;
                        // Getting next level quark
                        depth += 1;
                        subQuark = ssb.getQuarkRelative(callStackQuark, String.valueOf(depth));
                        int nextCorrelationIdQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.CORRELATION_ID);
                        Long nextCorrelationId = ssb.queryOngoingState(nextCorrelationIdQuark).unboxLong();
                        // Move operation down 1 level (inserting null to force the creation of a new interval
                        ssb.modifyAttribute(timestamp, null, previousQuark);
                        ssb.modifyAttribute(timestamp, ssb.queryOngoingState(subQuark).unboxValue(), previousQuark);
                        ssb.modifyAttribute(timestamp, nextCorrelationId, ssb.getQuarkRelativeAndAdd(previousQuark, RocmCallStackStateProvider.CORRELATION_ID));
                    }
                } catch (AttributeNotFoundException e) {
                    Activator.getInstance().logError("The correlation id was not found on the queue: " + correlationId);
                }
            }
        }
    }

    private static String getOperationNameFromSrcEvent(ITmfEvent operationEvent, @Nullable ITmfEvent srcEvent, RocmEventLayout layout) {
        String operationName = operationEvent.getContent().getFieldValue(String.class, layout.fieldOperationName());
        if (operationName != null && operationName.equals("") && srcEvent != null) {
            if (layout.isMemcpyBegin(srcEvent.getName())) {
                operationName = srcEvent.getContent().getFieldValue(String.class, layout.fieldMemcpyKind());
            } else if (srcEvent.getName().equals(layout.hipLaunchKernelBegin())) {
                operationName = srcEvent.getContent().getFieldValue(String.class, layout.fieldKernelName());
            } else {
                operationName = srcEvent.getName().substring(0, srcEvent.getName().length() - layout.getHipBeginSuffix().length());
            }
        }
        return operationName;
    }
/*
    private static String getCorrespondingHipCall(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) throws AttributeNotFoundException {
        Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
        if (correlationId == null) {
            return UNKNOWN;
        }
        int operationsQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.HIP_OPERATION_QUEUES);
        long ts = event.getTimestamp().getValue();
        int depth = 1;
        int subQuark = ssb.getQuarkRelative(operationsQuark, String.valueOf(depth));
        // While there is already activity on the quark
        while (correlationId != ssb.queryOngoingState(subQuark).unboxLong()) {
            depth += 1;
            subQuark = ssb.getQuarkRelative(operationsQuark, String.valueOf(depth));
        }

        int tidQuark = ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.TID);
        int hipOperationTid = ssb.queryOngoingState(tidQuark).unboxInt();

        Integer agentId = event.getContent().getFieldValue(Integer.class, layout.fieldAgentId());
        Integer queueId = event.getContent().getFieldValue(Integer.class, layout.fieldQueueId());
        addArrows(ssb, hipOperationTid, ssb.getOngoingStartTime(subQuark), event, getGpuQueueId(agentId, queueId));

        int nameQuark = ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.NAME);
        String hipOperationName = ssb.queryOngoingState(nameQuark).unboxStr();

        // set back to null to remove element from the queue
        ssb.modifyAttribute(ts, null, tidQuark);
        ssb.modifyAttribute(ts, null, nameQuark);
        ssb.modifyAttribute(ts, null, subQuark);
        return hipOperationName;
    }
*/
    private static int getGpuQueueId(Integer agentId, Integer queueId) {
        return Objects.hash(agentId, queueId);
    }

    private static void addArrows(ITmfStateSystemBuilder ssb, int tid, long srcTime, ITmfEvent destEvent, int gpuQueueId) {
        // hostid source
        String hostId = destEvent.getTrace().getHostId();
        HostThread src = new HostThread(hostId, tid);
        // hostid destination
        HostThread dest = new HostThread(destEvent.getTrace().getHostId(), gpuQueueId);
        int edgeQuark = getAvailableEdgeQuark(ssb, srcTime);
        Object edgeStateValue = new EdgeStateValue(0, src, dest);
        ssb.modifyAttribute(srcTime, edgeStateValue, edgeQuark);
        ssb.modifyAttribute(destEvent.getTimestamp().getValue(), (Object) null, edgeQuark);
    }

    private static int getAvailableEdgeQuark(ITmfStateSystemBuilder ssb, Long startTime) {
        int edgeRoot = ssb.getQuarkAbsoluteAndAdd(RocmCallStackAnalysis.EDGES);
        List<Integer> subQuarks = ssb.getSubAttributes(edgeRoot, false);
        for (int quark : subQuarks) {
            long start = ssb.getOngoingStartTime(quark);
            Object value = ssb.queryOngoing(quark);
            if (value == null && start <= startTime) {
                return quark;
            }
        }
        return ssb.getQuarkRelativeAndAdd(edgeRoot, Integer.toString(subQuarks.size()));
    }
}
