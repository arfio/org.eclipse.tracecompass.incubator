package org.eclipse.tracecompass.incubator.internal.thapi.core.trace;

import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

class ThapiEvent extends CtfTmfEvent {

    /**
     * Constructor, used by {@link ThapiEventFactory#createEvent}.
     *
     * Only subclasses should call this. It is imperative that the subclass also
     * has a constructor with the EXACT same parameter signature, because the
     * factory will look for a constructor with the same arguments.
     *
     * @param trace
     *            The trace to which this event belongs
     * @param rank
     *            The rank of the event
     * @param timestamp
     *            The timestamp
     * @param channel
     *            The CTF channel of this event
     * @param cpu
     *            The event's CPU
     * @param declaration
     *            The event declaration
     * @param eventDefinition
     *            The event definition
     */
    protected ThapiEvent(CtfTmfTrace trace,
            long rank,
            ITmfTimestamp timestamp,
            String channel,
            int cpu,
            IEventDeclaration declaration,
            IEventDefinition eventDefinition) {
        super(trace, rank, timestamp, channel, cpu, declaration, eventDefinition);
    }
}
