package org.eclipse.tracecompass.incubator.internal.mpi.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

public class MpiCallStackAnalysis extends InstrumentedCallStackAnalysis {

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new MpiStateProvider(getTrace());
    }
}
