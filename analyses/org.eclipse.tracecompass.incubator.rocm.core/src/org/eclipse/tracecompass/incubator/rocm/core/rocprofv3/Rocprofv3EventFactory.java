package org.eclipse.tracecompass.incubator.rocm.core.rocprofv3;

import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

public class Rocprofv3EventFactory extends CtfTmfEventFactory {

    @Override
    public CtfTmfEvent createEvent(CtfTmfTrace trace, IEventDefinition definition, String fileName) {
        final IEventDeclaration declaration = definition.getDeclaration();

        int sourceCPU = definition.getCPU();
        String reference = (fileName == null ? NO_STREAM : fileName);

        if(definition.get) {
            return new Rocprofv3Event(trace, ITmfContext.UNKNOWN_RANK, trace.getStartTime(), reference, sourceCPU, declaration, definition);
        }
    }

}
