package org.eclipse.tracecompass.incubator.internal.mpi.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class MpiStateProvider extends CallStackStateProvider {

    private static final String EVENT_PREFIX = "mpi:";
    private static final String ENTER = "enter";
    private static final String EXIT = "exit";

    public MpiStateProvider(ITmfTrace trace) {
        super(trace);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull CallStackStateProvider getNewInstance() {
        return new MpiStateProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        return event.getName().startsWith(EVENT_PREFIX);
    }

    @Override
    protected @Nullable ITmfStateValue functionEntry(@NonNull ITmfEvent event) {
        if (event.getName().contains(ENTER)) {
            return TmfStateValue.newValueString(event.getName().substring(EVENT_PREFIX.length() + ENTER.length() + 1));
        }
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        if (event.getName().contains(EXIT)) {
            return TmfStateValue.newValueString(event.getName().substring(EVENT_PREFIX.length() + EXIT.length() + 1));
        }
        return null;
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        String rankId = event.getContent().getFieldValue(String.class, "context.__app_MPI_rank");
        if (rankId == null || rankId.contains("none")) {
            return -1;
        }
        return Integer.parseInt(rankId.substring("int64=".length()));
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        return 0;
    }
}
