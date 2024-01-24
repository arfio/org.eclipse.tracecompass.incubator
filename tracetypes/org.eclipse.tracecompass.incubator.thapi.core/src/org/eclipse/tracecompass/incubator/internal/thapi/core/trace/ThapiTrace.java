package org.eclipse.tracecompass.incubator.internal.thapi.core.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxPidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTrace;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.incubator.internal.thapi.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfEventFieldAspect;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

/**
 *
 */
public class ThapiTrace extends CtfTmfTrace implements IGpuTrace {

    public ThapiTrace() {
        super(new ThapiEventFactory());
    }

    public static TmfEventFieldAspect fNameAspect = new TmfEventFieldAspect("Name", "name", ITmfEvent::getContent); //$NON-NLS-1$ //$NON-NLS-2$

    public static LinuxPidAspect fVpidAspect = new LinuxPidAspect() {

        @Override
        public @Nullable Integer resolve(@NonNull ITmfEvent event) {
            Long fieldValue = event.getContent().getFieldValue(Long.class, "context._vpid");
            if (fieldValue != null) {
                return fieldValue.intValue();
            }
            return null;
        }
    };
    public static LinuxTidAspect fVtidAspect = new LinuxTidAspect() {

        @Override
        public @Nullable Integer resolve(@NonNull ITmfEvent event) {
            Long fieldValue = event.getContent().getFieldValue(Long.class, "context._vtid");
            if (fieldValue != null) {
                return fieldValue.intValue();
            }
            return null;
        }
    };
    public static TmfEventFieldAspect fDurAspect = new TmfEventFieldAspect("Duration", "dur", ITmfEvent::getContent); //$NON-NLS-1$ //$NON-NLS-2$
    public static TmfEventFieldAspect fBackendAspect = new TmfEventFieldAspect("Backend", "context._backend", ITmfEvent::getContent); //$NON-NLS-1$ //$NON-NLS-2$

    @Override
    public IStatus validate(IProject project, String path) {
        IStatus retVal = super.validate(project, path);
        if (retVal instanceof CtfTraceValidationStatus) {

            CtfTraceValidationStatus status = (CtfTraceValidationStatus) retVal;
            Map<String, String> env = status.getEnvironment();
            if ("\"thapi\"".equals(env.get("trace_domain"))) { //$NON-NLS-1$ //$NON-NLS-2$
                return new CtfTraceValidationStatus(status.getConfidence() + 1, path, status.getEnvironment());
            }
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No host event"); //$NON-NLS-1$
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        Iterable<ITmfEventAspect<?>> oldAspects = super.getEventAspects();
        List<ITmfEventAspect<?>> aspects = new ArrayList<>();
        for (ITmfEventAspect<?> aspect : oldAspects) {
            aspects.add(aspect);
        }
        aspects.add(fNameAspect);
        aspects.add(fDurAspect);
        aspects.add(fVpidAspect);
        aspects.add(fVtidAspect);
        aspects.add(fBackendAspect);
        return aspects;
    }

    @Override
    public @NonNull IGpuTraceEventLayout getGpuTraceEventLayout() {
        return ThapiTraceLayout.getInstance();
    }
}
