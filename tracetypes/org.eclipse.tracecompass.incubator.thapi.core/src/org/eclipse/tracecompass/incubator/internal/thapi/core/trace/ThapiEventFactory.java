package org.eclipse.tracecompass.incubator.internal.thapi.core.trace;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

public class ThapiEventFactory extends CtfTmfEventFactory {

    private long fLastTs = 0;
    /**
     * Factory method to instantiate new CTF events.
     *
     * @param trace
     *            The trace to which the new event will belong
     * @param eventDef
     *            CTF EventDefinition object corresponding to this trace event
     * @param fileName
     *            The path to the trace file
     * @return The newly-built CtfTmfEvent
     * @since 2.0
     */
    @Override
    public CtfTmfEvent createEvent(CtfTmfTrace trace, IEventDefinition eventDef, @Nullable String fileName) {

        /* Prepare what to pass to CtfTmfEvent's constructor */
        final IEventDeclaration eventDecl = eventDef.getDeclaration();

        long ts = fLastTs;
        ICompositeDefinition streamContext = eventDef.getStreamContext();
        if (streamContext != null) {
            Definition definition = streamContext.getDefinition("_ts");
            if (definition instanceof IntegerDefinition) {
                IntegerDefinition integerDefinition = (IntegerDefinition) definition;
                ts = integerDefinition.getValue();
                if(ts==0) {
                    ts = fLastTs;
                }
                fLastTs = ts;
            }
        }
        ITmfTimestamp timestamp = trace.createTimestamp(trace.timestampCyclesToNanos(ts));

        int sourceCPU = eventDef.getCPU();

        String reference = (fileName == null ? NO_STREAM : fileName);

        /* Handle the special case of lost events */
        if (eventDecl.getName().equals(CTFStrings.LOST_EVENT_NAME)) {
            return createLostEvent(trace, eventDef, eventDecl, ts, timestamp, sourceCPU, reference);
        }

        /* Handle standard event types */
        return new ThapiEvent(trace,
                ITmfContext.UNKNOWN_RANK,
                timestamp,
                reference, // filename
                sourceCPU,
                eventDecl,
                eventDef);
    }
}
