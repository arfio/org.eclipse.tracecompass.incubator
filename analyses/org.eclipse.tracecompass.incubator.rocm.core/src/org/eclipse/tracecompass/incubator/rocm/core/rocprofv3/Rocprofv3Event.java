package org.eclipse.tracecompass.incubator.rocm.core.rocprofv3;

import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

public class Rocprofv3Event extends CtfTmfEvent {

    public Rocprofv3Event() {
        super();
    }

    public Rocprofv3Event(CtfTmfTrace trace, long rank, ITmfTimestamp timestamp, String channel, int cpu, IEventDeclaration declaration, IEventDefinition definition) {
        super(trace, rank, timestamp, channel, cpu, declaration, definition);
    }

}
