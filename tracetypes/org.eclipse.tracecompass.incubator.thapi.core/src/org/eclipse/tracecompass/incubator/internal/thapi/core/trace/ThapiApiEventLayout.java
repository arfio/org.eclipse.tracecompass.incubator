package org.eclipse.tracecompass.incubator.internal.thapi.core.trace;

import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout.IApiEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class ThapiApiEventLayout implements IApiEventLayout {

    private String fApiName;

    public ThapiApiEventLayout(String apiName) {
        fApiName = apiName;
    }

    @Override
    public boolean isBeginEvent() {
        return true;
    }

    @Override
    public String getEventName(ITmfEvent event) {
        Object name = ThapiTrace.fNameAspect.resolve(event);
        if (name instanceof String) {
            return (String) name;
        }
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getApiName() {
        return fApiName;
    }
}
