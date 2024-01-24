package org.eclipse.tracecompass.incubator.internal.thapi.core.trace;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

@org.eclipse.jdt.annotation.NonNullByDefault
public class ThapiTraceLayout implements IGpuTraceEventLayout {

    private static String LEVEL_ZERO_PREFIX = "ze"; //$NON-NLS-1$
    private static String CUDA_PREFIX = "cuda"; //$NON-NLS-1$
    private static String OPENCL_PREFIX = "cl"; //$NON-NLS-1$
    private static String TID = "context._vtid"; //$NON-NLS-1$
    private static String DURATION = "dur"; //$NON-NLS-1$

    private static @Nullable ThapiTraceLayout INSTANCE;
    private static List<IApiEventLayout> fApiLayouts = List.of(new ThapiApiEventLayout("CUDA"), new ThapiApiEventLayout("OpenCL"), //$NON-NLS-1$ //$NON-NLS-2$
            new ThapiApiEventLayout("LevelZero")); //$NON-NLS-1$


    /**
     * The instance of this event layout
     * <p>
     * This object is completely immutable, so no need to create additional
     * instances via the constructor.
     *
     * @return the instance
     */
    public static synchronized ThapiTraceLayout getInstance() {
        ThapiTraceLayout instance = INSTANCE;
        if (instance == null) {
            instance = new ThapiTraceLayout();
            INSTANCE = instance;
        }
        return instance;
    }

    @Override
    public Collection<IApiEventLayout> getApiLayouts() {
        return fApiLayouts;
    }

    @Override
    public IApiEventLayout getCorrespondingApiLayout(ITmfEvent event) {
        Object name = ThapiTrace.fNameAspect.resolve(event);
        if (name instanceof String) {
            if (((String) name).startsWith(CUDA_PREFIX)) {
                return fApiLayouts.get(0);
            } else if (((String) name).startsWith(OPENCL_PREFIX)) {
                return fApiLayouts.get(1);
            } else if (((String) name).startsWith(LEVEL_ZERO_PREFIX)) {
                return fApiLayouts.get(2);
            }
        }
        return new ThapiApiEventLayout("UNKNOWN"); //$NON-NLS-1$
    }

    @Override
    public boolean isMemcpyBegin(ITmfEvent event) {
        return false;
    }

    @Override
    public boolean isLaunchBegin(ITmfEvent event) {
        return false;
    }

    @Override
    public boolean isApiEvent(ITmfEvent event) {
        Object name = ThapiTrace.fNameAspect.resolve(event);
        if (name instanceof String) {
            return ((String) name).startsWith(CUDA_PREFIX) || ((String) name).startsWith(LEVEL_ZERO_PREFIX) || ((String) name).startsWith(OPENCL_PREFIX);
        }
        return false;
    }

    @Override
    public String fieldThreadId() {
        return TID;
    }

    @Override
    public String fieldDuration() {
        return DURATION;
    }
}
