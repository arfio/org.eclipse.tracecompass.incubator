package org.eclipse.tracecompass.incubator.internal.thapi.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.thapi.core.trace.ThapiTrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 *
 */
public class ThapiCallstackAnalysis extends InstrumentedCallStackAnalysis {

    private static final @NonNull String ID = "org.eclipse.tracecompass.incubator.thapi.analysis.callstack"; //$NON-NLS-1$

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        if (trace instanceof ThapiTrace) {
            return new ThapiCallstackProvider(trace);
        }
        // placeholder for CTF UFTraces
        throw new IllegalStateException();
    }

}
