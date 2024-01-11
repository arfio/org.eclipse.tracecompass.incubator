package org.eclipse.tracecompass.incubator.internal.thapi.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.thapi.core.trace.ThapiTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 *
 */
public class ThapiCallstackProvider extends CallStackStateProvider {

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public ThapiCallstackProvider(ITmfTrace trace) {
        super(trace);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull CallStackStateProvider getNewInstance() {
        return new ThapiCallstackProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        return event.getName().contains("host");
    }

    @Override
    protected @Nullable Object functionEntry(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        long timestamp = event.getTimestamp().toNanos();
        if (ss == null ) {
            return null;
        }
        int processId = getProcessId(event);
        String processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);
        String threadName = getThreadName(event);
        long threadId = getThreadId(event);
        if (threadName == null) {
            threadName = Long.toString(threadId);
        }
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);
        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        addFutureEvent(timestamp + getDuration(event), event, callStackQuark, FutureEventType.POP);

        return ThapiTrace.fNameAspect.resolve(event);
    }

    @Override
    protected @Nullable Object functionExit(@NonNull ITmfEvent event) {
        return null;
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        Object value = ThapiTrace.fVpidAspect.resolve(event);
        if (value instanceof Integer) {
                return (int) value;
        }
        return -1;
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Object value = ThapiTrace.fVtidAspect.resolve(event);
        if (value instanceof Long) {
                return (long) value;
        }
        return -1;
    }

    protected long getDuration(@NonNull ITmfEvent event) {
        Object resolve = ThapiTrace.fDurAspect.resolve(event);
        if (resolve instanceof Long) {
            return (long) resolve;
        }
        return -1;
    }

}
