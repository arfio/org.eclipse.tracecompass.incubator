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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.concurrentstatesystem.core.SpanCustomValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.MultiAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public class SpanFlameStateProvider extends SpanCallStackStateProvider {

    private final ITmfEventAspect<Integer> fTidAspect;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public SpanFlameStateProvider(ITmfTrace trace) {
        super(trace);
        fTidAspect = (ITmfEventAspect<Integer>) MultiAspect.<Integer> create(TmfTraceUtils.getEventAspects(trace, LinuxTidAspect.class), LinuxTidAspect.class);
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public @NonNull SpanCallStackStateProvider getNewInstance() {
        return new SpanFlameStateProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(ITmfEvent event) {
        return true;
    }

    @Override
    protected @Nullable Object functionEntry(ITmfEvent event) {
        String funcNameContext = "funcName"; //$NON-NLS-1$
        String operationName = "span_id"; //$NON-NLS-1$
        String parentName = "parent_span_id"; //$NON-NLS-1$
        String serviceName = "op_name";

        String startSpan = "jaeger_ust:start_span"; //$NON-NLS-1$

        ITmfEventField content = event.getContent();
        ITmfEventField msg = content.getField("msg");

        if (msg != null) {
            content = msg;
            funcNameContext = "funcName"; //$NON-NLS-1$
            operationName = "operation_name"; //$NON-NLS-1$
            startSpan = "start_span"; //$NON-NLS-1$

            String funcName = (String) event.getContent().getField(funcNameContext).getValue();

            if (funcName.contains(startSpan)) {

                Map<String, String> map = MessageHashMapExtractor(content);

                return map.get(operationName);
            }
            return null;
        }

        startSpan = "jaeger_ust:start_span"; //$NON-NLS-1$
        if (event.getName().equals(startSpan)) {

            return new SpanCustomValue(content.getFieldValue(String.class, operationName), content.getFieldValue(String.class, parentName), content.getFieldValue(String.class, serviceName));

        }

        return null;
    }

    protected Map<String, String> MessageHashMapExtractor(ITmfEventField value) {
        // split the string to creat key-value pairs
        Map<String, String> map = new HashMap<>();
        // iterate over the pairs
        for (ITmfEventField field : value.getFields()) {
            Objects.requireNonNull(field);
            map.put(field.getName(), field.getValue().toString().trim());
        }
        if (map.isEmpty()) {
            String valueString = (String) Objects.requireNonNull(value.getValue());
            String[] values = valueString.split(",");
            for (String tuple : values) {
                String[] parts = tuple.split("=");
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }

    @Override
    protected @Nullable Object functionExit(ITmfEvent event) {
        String funcNameContext = "funcName"; //$NON-NLS-1$
        String operationName = "span_id"; //$NON-NLS-1$
        String reportSpan = "jaeger_ust:end_span"; //$NON-NLS-1$

        ITmfEventField content = event.getContent();
        ITmfEventField msg = content.getField("msg");
        if (msg != null) {
            content = msg;
            funcNameContext = "funcName"; //$NON-NLS-1$
            operationName = "operation_name"; //$NON-NLS-1$
            reportSpan = "report_span"; //$NON-NLS-1$
        } else {

            if (event.getName().equals(reportSpan)) {
                operationName = "span_id"; //$NON-NLS-1$
                return content.getFieldValue(String.class, operationName);
            }

            return null;
        }

        String funcName = (String) event.getContent().getField(funcNameContext).getValue();
        if (funcName.contains(reportSpan)) {

            Map<String, String> map = MessageHashMapExtractor(content);

            return map.get(operationName);
        }
        return null;
    }

    @Override
    protected @Nullable String getProcessName(ITmfEvent event) {
        Long fieldValue = event.getContent().getFieldValue(Long.class, "trace_id");
        if (fieldValue == null) {
            fieldValue = event.getContent().getFieldValue(Long.class, "trace_id_low");
        }

        return fieldValue == null ? "eduroam" : Long.toHexString(fieldValue);
    }

    @Override
    protected int getProcessId(ITmfEvent event) {
        Long resolve = event.getContent().getFieldValue(Long.class, "trace_id");
        return resolve == null ? -1 : resolve.intValue();
    }

    @Override
    protected long getThreadId(ITmfEvent event) {
        Integer resolve = fTidAspect.resolve(event);
        return resolve == null ? -1 : resolve.longValue();
    }
}
