package org.eclipse.tracecompass.incubator.rocm.core.rocprofv3;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

public class Rocprofv3Trace extends CtfTmfTrace {


    private final Queue<ITmfEvent> fFutureEvents = new PriorityQueue<>(Comparator.comparing(ITmfEvent::getTimestamp));
    private final Integer fLastMissingCorrelationId = 0;
    private final Integer fLastGpuOperationCorrelationId = 0;


    public Rocprofv3Trace() {
        super(new Rocprofv3EventFactory());
    }

    @Override
    public IStatus validate(IProject project, String path) {
        IStatus retVal = super.validate(project, path);
        if (retVal instanceof CtfTraceValidationStatus status) {
            return new CtfTraceValidationStatus(status.getConfidence() + 1, path, status.getEnvironment());
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No host event"); //$NON-NLS-1$
    }

    @Override
    public synchronized CtfTmfEvent getNext(final ITmfContext context) {
        CtfTmfEvent event = super.getNext(context);

        if (event.getContent().getFieldValue(Integer.class, "correlation_id")) {

        }
        fFutureEvents.add(event);


        return event;
    }

}
