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

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.ProcessNameAspect;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.ThreadNameAspect;
import org.eclipse.tracecompass.incubator.concurrentstatesystem.core.ConcurrentStateSystem;
import org.eclipse.tracecompass.incubator.concurrentstatesystem.core.SpanCustomValue;
import org.eclipse.tracecompass.incubator.internal.concurrentstatesystem.core.ConcurrentTransientState;
import org.eclipse.tracecompass.incubator.internal.concurrentstatesystem.core.ProcessQuark;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * The state provider for span traces that implement the default
 * {@link InstrumentedCallStackAnalysis} with a process grouping, using the
 * default values.
 *
 *
 * The attribute tree should have the following structure:
 *
 * <pre>
 * (root)
 *   +-- Processes
 *       +-- (PID 1000)
 *       |    |    +-- CallStack (stack-attribute)
 *       |    |         +-- 1
 *       |    |         +-- 2
 *       |    |        ...
 *       |    |         +-- n
 *       +-- (PID 2000)
 *                 +-- CallStack (stack-attribute)
 *                      +-- 1
 *                      +-- 2
 *                     ...
 *                      +-- n
 * </pre>
 *
 * where:
 * <ul>
 * <li>(PID n) is an attribute whose name is the display name of the process.
 * Optionally, its value can be an int representing the process id. Otherwise,
 * the attribute name can be set to the process id formatted as a string.</li>
 * <li>"CallStack" is a stack-attribute whose pushed values are either a string,
 * int or long representing the function name or address in the call stack. The
 * type of value used must be constant for a particular CallStack.</li>
 * </ul>
 *
 * @author Fateme Faraji Daneshgar
 */
public abstract class SpanCallStackStateProvider extends AbstractTmfStateProvider {

    /**
     * Thread attribute
     *
     * @since 2.0
     */
    public static final String PROCESSES = "Processes"; //$NON-NLS-1$

    /**
     * Unknown process ID
     *
     * @since 2.0
     */
    public static final int UNKNOWN_PID = -1;

    /**
     * Unknown name
     *
     * @since 2.0
     */
    public static final String UNKNOWN = "UNKNOWN"; //$NON-NLS-1$

    /** CallStack state system ID */
    private static final String ID = "org.eclipse.linuxtools.tmf.parallelcallstack"; //$NON-NLS-1$

    /**
     * Default constructor
     *
     * @param trace
     *            The trace for which we build this state system
     */
    public SpanCallStackStateProvider(ITmfTrace trace) {
        super(trace, ID);

    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        handleFunctionEntry(ss, event);
        handleFunctionExit(ss, event);
    }

    private void handleFunctionEntry(ITmfStateSystemBuilder ss, ITmfEvent event) {
        /* Check if the event is a function entry */
        Object functionEntryName = functionEntry(event);
        String parentName = null;
        if (functionEntryName instanceof SpanCustomValue) {
            parentName = ((SpanCustomValue) functionEntryName).getParentId();
        }
        if (functionEntryName != null) {
            long timestamp = event.getTimestamp().toNanos();

            String processName = getProcessName(event);
            int processId = getProcessId(event);
            if (processName == null) {
                processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
            }
            ConcurrentStateSystem css = (ConcurrentStateSystem) ss;

            ProcessQuark pq = css.getQuarkAbsoluteAndAddConcurrent(PROCESSES, processName);
            css.updateOngoingState(TmfStateValue.newValueInt(processId), 1, processName);

            ProcessQuark callStackQuark = css.getQuarkRelativeAndAdd(processName, pq.getAttrQuarck(), InstrumentedCallStackAnalysis.CALL_STACK);
            css.pushAttribute(timestamp, functionEntryName, callStackQuark.getAttrQuarck(), processName, parentName);
            return;
        }
    }

    private void handleFunctionExit(ITmfStateSystemBuilder ss, ITmfEvent event) {
        /* Check if the event is a function exit */
        Object functionExitState = functionExit(event);
        // FIXME: since
        if (functionExitState != null) {

            ConcurrentStateSystem css = (ConcurrentStateSystem) ss;
            long timestamp = event.getTimestamp().toNanos();
            String processName = getProcessName(event);
            if (processName == null) {
                int processId = getProcessId(event);
                processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
            }
            try {
                popObject(css, timestamp, functionExitState, processName);
            } catch (AttributeNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Restrict the return type for {@link ITmfStateProvider#getNewInstance}.
     *
     * @since 2.0
     */
    @Override
    public abstract SpanCallStackStateProvider getNewInstance();

    /**
     * Check if this event should be considered at all for function entry/exit
     * analysis. This check is only run once per event, before
     * {@link #functionEntry} and {@link #functionExit} (to avoid repeating
     * checks in those methods).
     *
     * @param event
     *            The event to check
     * @return If false, the event will be ignored by the state provider. If
     *         true processing will continue.
     */
    protected abstract boolean considerEvent(ITmfEvent event);

    /**
     * Check an event if it indicates a function entry.
     *
     * @param event
     *            An event to check for function entry
     * @return The object representing the function being entered, or null if
     *         not a function entry
     * @since 2.0
     */
    protected abstract @Nullable Object functionEntry(ITmfEvent event);

    /**
     * Check an event if it indicates a function exit.
     *
     * @param event
     *            An event to check for function exit
     * @return The object representing the function being exited, or
     *         TmfStateValue#nullValue() if the exited function is undefined, or
     *         null if not a function exit.
     * @since 2.0
     */
    protected abstract @Nullable Object functionExit(ITmfEvent event);

    /**
     * Return the process ID of a function entry event.
     * <p>
     * Use {@link #UNKNOWN_PID} if it is not known.
     *
     * @param event
     *            The event
     * @return The process ID
     * @since 2.0
     */
    protected abstract int getProcessId(ITmfEvent event);

    /**
     * Return the process name of a function entry event.
     *
     * @param event
     *            The event
     * @return The process name (as will be shown in the view) or null to use
     *         the process ID formatted as a string (or {@link #UNKNOWN})
     * @since 2.0
     */
    protected @Nullable String getProcessName(ITmfEvent event) {
        /* Override to provide a process name */
        Object resolved = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), ProcessNameAspect.class, event);
        return (resolved instanceof String) ? (String) resolved : null;
    }

    /**
     * Return the thread id of a function entry event.
     *
     * @param event
     *            The event
     * @return The thread id
     * @since 2.0
     */
    protected abstract long getThreadId(ITmfEvent event);

    /**
     * Return the thread name of a function entry or exit event.
     *
     * @param event
     *            The event
     * @return The thread name (as will be shown in the view) or null to use the
     *         thread id formatted as a string
     */
    protected @Nullable String getThreadName(ITmfEvent event) {
        /* Override to provide a thread name */
        Object resolved = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), ThreadNameAspect.class, event);
        return (resolved instanceof String) ? (String) resolved : null;
    }

    public Object popObject(ITmfStateSystemBuilder ss, long t, Object functionExitState, String processName) throws AttributeNotFoundException {

        ConcurrentStateSystem css = ((ConcurrentStateSystem) ss);
        int stackDepth;
        ConcurrentTransientState ts = css.getTransientState(processName);

        stackDepth = ts.getCallStack();

        if (stackDepth == 0) {
            /*
             * Trying to pop an empty stack. This often happens at the start of
             * traces, for example when we see a syscall_exit, without having
             * the corresponding syscall_entry in the trace. Just ignore
             * silently.
             */
            return null;
        }

        @Nullable
        Object attributeSV = null;

        int subAttributeQuark;
        int subTsQuark;
        subTsQuark = css.getStateSize(processName);
        while (subTsQuark > 2) {
            attributeSV = css.queryOngoing(subTsQuark, processName);
            if (attributeSV != null) {
                if (attributeSV instanceof SpanCustomValue) {
                    if (functionExitState.equals(((SpanCustomValue) attributeSV).getSpanId())) {
                        break;
                    }
                }
            }

            subTsQuark--;
        }
        subAttributeQuark = css.getAttributeQuarck(processName, subTsQuark);
        stackDepth = css.getStackDepth(processName, subTsQuark) - 1;
        /* Delete the sub-attribute that contained the user's state value */
        css.removeAttribute(t, subTsQuark, subAttributeQuark, processName);
        Integer nextSV;
        if (stackDepth == 0) {
            // /* Store a null state value */
            nextSV = null;
        } else {
            nextSV = stackDepth;
        }
        /* Update the state value of the stack-attribute */
        css.modifyAttribute(t, nextSV, 2, processName);

        return attributeSV;

    }
}
