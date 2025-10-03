/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.spanmetrics.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisEventBasedModule;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.incubator.spanmetrics.core.KernelMetrics.StateName;
import org.eclipse.tracecompass.incubator.spanmetrics.core.KernelMetrics.SyscallName;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory.SegmentStoreType;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableList;

/**
 * The analysis module extract and evaluate the performance metrics related to
 * each span of the specific thread The current version is just evaluating the
 * kernel metrics, but in the future we are going to extend it to more metrics
 * including ust one.
 *
 * @author Maryam Ekhlasi
 */
public class SpanMetricsAnalysis extends AbstractSegmentStoreAnalysisEventBasedModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.spanmetrics"; //$NON-NLS-1$
    private static final int VERSION = 1;
    Map<String, KernelMetrics> fkernelMap;
    long endTime1;
    long duration;
    long startTime1;

    @SuppressWarnings("null")
    private static final Collection<ISegmentAspect> BASE_ASPECTS = ImmutableList.of(SpanAspect.INSTANCE, OperationAspect.INSTANCE, TraceAspect.INSTANCE, ThreadAspect.INSTANCE,
            new SpanState(StateName.WAITBLOCKED),
            new SpanState(StateName.RUN),
            new SpanState(StateName.RUNSYSTEMCALL),
            new SpanState(StateName.INTERRUPTED),
            new SpanState(StateName.WAITCPU),
            new SpanState(StateName.WAITUNKNOWN),
            new SpanState(StateName.WAITFORK),
            new SpanState(StateName.UNKNOWN),

            new SyscallState(SyscallName.write),
            new SyscallState(SyscallName.futex),
            new SyscallState(SyscallName.unknown),
            new SyscallState(SyscallName.getrlimit),
            new SyscallState(SyscallName.select),
            new SyscallState(SyscallName.sendto),
            new SyscallState(SyscallName.sigaltstack),
            new SyscallState(SyscallName.getresgid),
            new SyscallState(SyscallName.newfstatat),
            new SyscallState(SyscallName.bind),
            new SyscallState(SyscallName.set_tid_address),
            new SyscallState(SyscallName.clock_gettime),
            new SyscallState(SyscallName.brk),
            new SyscallState(SyscallName.rt_sigprocmask),
            new SyscallState(SyscallName.mkdir),
            new SyscallState(SyscallName.getegid),
            new SyscallState(SyscallName.pipe2),
            new SyscallState(SyscallName.inotify_add_watch),
            new SyscallState(SyscallName.pselect6),
            new SyscallState(SyscallName.sync_file_range),
            new SyscallState(SyscallName.setgid),
            new SyscallState(SyscallName.execve),
            new SyscallState(SyscallName.getgid),
            new SyscallState(SyscallName.socketpair),
            new SyscallState(SyscallName.timerfd_create),
            new SyscallState(SyscallName.recvfrom),
            new SyscallState(SyscallName.rt_sigtimedwait),
            new SyscallState(SyscallName.epoll_pwait),
            new SyscallState(SyscallName.accept),
            new SyscallState(SyscallName.setsid),
            new SyscallState(SyscallName.getsid),
            new SyscallState(SyscallName.setresgid),
            new SyscallState(SyscallName.getpgid),
            new SyscallState(SyscallName.nanosleep),
            new SyscallState(SyscallName.dup2),
            new SyscallState(SyscallName.openat),
            new SyscallState(SyscallName.flock),
            new SyscallState(SyscallName.umask),
            new SyscallState(SyscallName.access),
            new SyscallState(SyscallName.fcntl),
            new SyscallState(SyscallName.setpriority),
            new SyscallState(SyscallName.getsockopt),
            new SyscallState(SyscallName.syslog),
            new SyscallState(SyscallName.setsockopt),
            new SyscallState(SyscallName.recvmsg),
            new SyscallState(SyscallName.epoll_create1),
            new SyscallState(SyscallName.mknod),
            new SyscallState(SyscallName.epoll_create),
            new SyscallState(SyscallName.getpgrp),
            new SyscallState(SyscallName.getgroups),
            new SyscallState(SyscallName.epoll_wait),
            new SyscallState(SyscallName.close),
            new SyscallState(SyscallName.fdatasync),
            new SyscallState(SyscallName.connect),
            new SyscallState(SyscallName.newfstat),
            new SyscallState(SyscallName.pwrite64),
            new SyscallState(SyscallName.rt_sigaction),
            new SyscallState(SyscallName.fchmod),
            new SyscallState(SyscallName.kill),
            new SyscallState(SyscallName.rt_sigpending),
            new SyscallState(SyscallName.getpeername),
            new SyscallState(SyscallName.ppoll),
            new SyscallState(SyscallName.munmap),
            new SyscallState(SyscallName.getpriority),
            new SyscallState(SyscallName.rename),
            new SyscallState(SyscallName.getresuid),
            new SyscallState(SyscallName.ftruncate),
            new SyscallState(SyscallName.sched_getaffinity),
            new SyscallState(SyscallName.get_mempolicy),
            new SyscallState(SyscallName.prctl),
            new SyscallState(SyscallName.poll),
            new SyscallState(SyscallName.fstatfs),
            new SyscallState(SyscallName.listen),
            new SyscallState(SyscallName.setgroups),
            new SyscallState(SyscallName.sysinfo),
            new SyscallState(SyscallName.timer_create),
            new SyscallState(SyscallName.pipe),
            new SyscallState(SyscallName.unlinkat),
            new SyscallState(SyscallName.tgkill),
            new SyscallState(SyscallName.read),
            new SyscallState(SyscallName.getrusage),
            new SyscallState(SyscallName.getsockname),
            new SyscallState(SyscallName.setresuid),
            new SyscallState(SyscallName.sched_yield),
            new SyscallState(SyscallName.faccessat),
            new SyscallState(SyscallName.newuname),
            new SyscallState(SyscallName.renameat),
            new SyscallState(SyscallName.chdir),
            new SyscallState(SyscallName.geteuid),
            new SyscallState(SyscallName.setuid),
            new SyscallState(SyscallName.getuid),
            new SyscallState(SyscallName.mprotect),
            new SyscallState(SyscallName.pread64),
            new SyscallState(SyscallName.timerfd_settime),
            new SyscallState(SyscallName.shutdown),
            new SyscallState(SyscallName.epoll_ctl),
            new SyscallState(SyscallName.prlimit64),
            new SyscallState(SyscallName.lseek),
            new SyscallState(SyscallName.readlink),
            new SyscallState(SyscallName.timer_settime),
            new SyscallState(SyscallName.eventfd2),
            new SyscallState(SyscallName.ioctl),
            new SyscallState(SyscallName.madvise),
            new SyscallState(SyscallName.fadvise64),
            new SyscallState(SyscallName.fsync),
            new SyscallState(SyscallName.getcwd),
            new SyscallState(SyscallName.times),
            new SyscallState(SyscallName.getdents64),
            new SyscallState(SyscallName.alarm),
            new SyscallState(SyscallName.chmod),
            new SyscallState(SyscallName.accept4),
            new SyscallState(SyscallName.getppid),
            new SyscallState(SyscallName.wait4),
            new SyscallState(SyscallName.splice),
            new SyscallState(SyscallName.fchown),
            new SyscallState(SyscallName.setpgid),
            new SyscallState(SyscallName.statfs),
            new SyscallState(SyscallName.fallocate),
            new SyscallState(SyscallName.clock_nanosleep),
            new SyscallState(SyscallName.newlstat),
            new SyscallState(SyscallName.gettid),
            new SyscallState(SyscallName.mkdirat),
            new SyscallState(SyscallName.unlink),
            new SyscallState(SyscallName.setitimer),
            new SyscallState(SyscallName.clone),
            new SyscallState(SyscallName.readlinkat),
            new SyscallState(SyscallName.setrlimit),
            new SyscallState(SyscallName.sendmsg),
            new SyscallState(SyscallName.utimensat),
            new SyscallState(SyscallName.socket),
            new SyscallState(SyscallName.set_robust_list),
            new SyscallState(SyscallName.mmap),
            new SyscallState(SyscallName.getpid),
            new SyscallState(SyscallName.newstat),
            new SyscallState(SyscallName.dup),
            new SyscallState(SyscallName.writev));

    private KernelAnalysisModule fModule;

    /**
     * Constructor
     */
    public SpanMetricsAnalysis() {
        fkernelMap = new HashMap<>();

    }

    /**
     * @param threadId
     *            ThreadId of the current span
     * @param start
     *            Start of the span time
     * @param end
     *            End time of the span time
     * @return Kernel Metrics
     */
    @SuppressWarnings("null")
    public KernelMetrics getMetrics(String threadId, long start, long end) {
        KernelMetrics kernelMetrics = new KernelMetrics();

        try {
            ITmfTrace trace = getTrace();
            KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
            fModule = module;
            ITmfStateSystem ss = fModule.getStateSystem();
            fModule.waitForCompletion();
            if (ss == null) {
                return null;
            }

            @SuppressWarnings("restriction")
            int threadParentQuark = ss.getQuarkAbsolute(Attributes.THREADS, threadId);

            for (ITmfStateInterval interval : ss.query2D(List.of(threadParentQuark), start, end)) {
                kernelMetrics.setStateDurationTime(interval.getValueInt(), interval.getStartTime(), interval.getEndTime());
            }
            kernelMetrics.fsyscallMap = getInterruptionMetrics(threadId, start, end);

        } catch (Exception ex) {
            // Assume that the attribute does not exist yet,
        }
        return kernelMetrics;
    }
//    private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    /**
     * @param threadId
     *            ThreadId of the current span
     * @param start
     *            Start of the span time
     * @param end
     *            End time of the span time
     * @return Kernel Metrics
     */
    @SuppressWarnings("null")
    public Map<String, Long> getInterruptionMetrics(String threadId, long start, long end) {
        KernelMetrics kernelMetrics = new KernelMetrics();

        try {
            List<@NonNull ITimeGraphState> list = new ArrayList<>();
            ITmfTrace trace = getTrace();
            KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
            fModule = module;
            ITmfStateSystem ss = fModule.getStateSystem();
            fModule.waitForCompletion();
            if (ss == null) {
                return null;
            }

            int syscallQuark = ss.optQuarkAbsolute(Attributes.THREADS, String.valueOf(threadId), Attributes.SYSTEM_CALL);
            if (syscallQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
//                Object syscallName = ss.querySingleState(start, syscallQuark).getValue();
                Iterable<@NonNull ITmfStateInterval> intervals = ss.query2D(Collections.singletonList(syscallQuark), start, end);
                for (ITmfStateInterval interval: intervals) {
                    if (interval.getValue() != null) {
                        list.add(new TimeGraphState(interval.getStartTime(), interval.getEndTime() - interval.getStartTime(), 1, interval.getValueString()));
                        kernelMetrics.setSysCallDurationTime(interval.getValueString(), interval.getStartTime(), interval.getEndTime());
                    }
                }
//                if (syscallName instanceof String) {
//                    list.add(new TimeGraphState(start, duration, String.valueOf(syscallName), STYLE_MAP.computeIfAbsent(LinuxStyle.SYSCALL.getLabel(), style -> new OutputElementStyle(style))));
//                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return kernelMetrics.fsyscallMap;
    }



    /*
     * Getting the ID of the class
     */
    @Override
    public String getId() {
        return ID;
    }

    @SuppressWarnings("null")
    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    /*
     * Getting the version of the class
     */
    @Override
    protected int getVersion() {
        return VERSION;
    }

    @Override
    protected @NonNull SegmentStoreType getSegmentStoreType() {
        return SegmentStoreType.OnDisk;
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        return Collections.singletonList(TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID));
    }

    @Override
    protected @NonNull AbstractSegmentStoreAnalysisRequest createAnalysisRequest(@NonNull ISegmentStore<@NonNull ISegment> segmentStore, @NonNull IProgressMonitor monitor) {
        return new SpanAnalysisRequest(segmentStore, monitor);
    }

    @SuppressWarnings("null")
    @Override
    protected @NonNull IHTIntervalReader<@NonNull ISegment> getSegmentReader() {
        return Span.READER;
    }

    /*
     * Filling spans' HashMaps
     */
    private class SpanAnalysisRequest extends AbstractSegmentStoreAnalysisRequest {
        private final Map<String, Span.InitialInfo> fOngoingSpan = new HashMap<>();
        private final IProgressMonitor fMonitor;

        public SpanAnalysisRequest(ISegmentStore<@NonNull ISegment> sSegment, IProgressMonitor monitor) {
            super(sSegment);
            fMonitor = monitor;
        }

        /*
         * Iterating trace events from start to end and fetch parameter's value
         * from start and end of spans.
         */
        @Override
        public void handleData(final ITmfEvent event) {
            startTime1 = System.nanoTime();
            super.handleData(event);

            ILttngUstEventLayout layout = ILttngUstEventLayout.DEFAULT_LAYOUT;
            final String eventType = "jaeger_ust"; //$NON-NLS-1$

            String eventTypeValue = event.getType().getName();

            if (event.getType().getName().contains(eventType)) {
                final String operationName = "op_name"; //$NON-NLS-1$
                final String parentId = "parent_span_id"; //$NON-NLS-1$
                final String traceId = "trace_id_low"; //$NON-NLS-1$
                final String spanId = "span_id"; //$NON-NLS-1$
                final String startSpan = "jaeger_ust:start_span"; //$NON-NLS-1$
                final String endSpan = "jaeger_ust:end_span"; //$NON-NLS-1$
                long startTime = event.getTimestamp().toNanos();

                if (eventTypeValue.contains(startSpan) || eventTypeValue.contains(endSpan)) {

                    String spanID = String.valueOf(event.getContent().getField(spanId).getValue());

                    if (eventTypeValue.equals(startSpan)) {

                        if (!fOngoingSpan.containsKey(spanID)) {

                            String opName = (String) event.getContent().getField(operationName).getValue();
                            String pId = String.valueOf(event.getContent().getField(parentId).getValue());
                            String trId = String.valueOf(event.getContent().getField(traceId).getValue());
                            String treadid = event.getContent().getFieldValue(String.class, layout.contextVtid());

                            Span.InitialInfo spanInfo = new Span.InitialInfo(startTime, opName, treadid, trId, spanID, pId);
                            fOngoingSpan.put(spanID, spanInfo);
                        }
                    } else if (eventTypeValue.equals(endSpan)) {

                        if (fOngoingSpan.containsKey(spanID)) {

                            Span.InitialInfo info = fOngoingSpan.remove(spanID);

                            if (info == null) {
                                return;
                            }
                            long endTime = event.getTimestamp().toNanos();

                            Span spanW = new Span(info, endTime);

                            //spanW.setMetrics(getInterruptionMetrics(spanW.getTid(), spanW.getStart(), endTime));
                            spanW.setMetrics(getMetrics(spanW.getTid(), spanW.getStart(), endTime));

                            getSegmentStore().add(spanW);

                        }
                    }

                }
            }
            endTime1 = System.nanoTime();
            duration = duration+(endTime1 - startTime1);
            //System.out.println("Execution time: " + duration + " nanoseconds");
        }

        @Override
        public void handleCompleted() {
            fOngoingSpan.clear();
            super.handleCompleted();
        }

        @Override
        public void handleCancel() {
            fMonitor.setCanceled(true);
            super.handleCancel();
        }
    }

    /**
     * @param value
     *            extract the parameters within the event's context separated by
     *            comma
     * @return Map
     */
    protected Map<String, String> MessageHashMapExtractor(String value) {
        // split the string to creat key-value pairs
        String[] keyValuePairs = value.split(","); //$NON-NLS-1$

        Map<String, String> map = new HashMap<>();
        // iterate over the pairs
        for (String pair : keyValuePairs) {
            String[] entry = pair.split("="); //$NON-NLS-1$
            map.put(entry[0].trim(), entry[1].trim());
        }
        return map;
    }

    /*
     * Define the SpanId column of the segment store table
     */
    private static final class SpanAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SpanAspect();

        private SpanAspect() {
            // Do nothing
        }

        @SuppressWarnings("null")
        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectName_Sid);
        }

        @Override
        public String getName() {
            return "SpanId"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof Span && segment2 instanceof Span) {
                    int res = ((Span) segment1).getName().compareToIgnoreCase(((Span) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof Span) {
                return ((Span) segment).getSid();
            }
            return null;
        }
    }

    /*
     * Define the operationName column of the segment store table
     */
    private static final class OperationAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new OperationAspect();

        private OperationAspect() {
            // Do nothing
        }

        @SuppressWarnings("null")
        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectName_OperationName);
        }

        @Override
        public String getName() {
            return "OperationName"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof Span && segment2 instanceof Span) {
                    int res = ((Span) segment1).getName().compareToIgnoreCase(((Span) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof Span) {
                return ((Span) segment).getName();
            }
            return null;
        }
    }

    /*
     * Define the TraceID column of the segment store table
     */
    private static final class TraceAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new TraceAspect();

        private TraceAspect() {
            // Do nothing
        }

        @SuppressWarnings("null")
        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_Traceid);
        }

        @Override
        public String getName() {
            return "TraceID"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof Span && segment2 instanceof Span) {
                    int res = ((Span) segment1).getName().compareToIgnoreCase(((Span) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof Span) {
                return ((Span) segment).getTraceid();
            }
            return null;
        }
    }

    /*
     * Define the Thread column of the segment store table
     */
    private static final class ThreadAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new ThreadAspect();

        private ThreadAspect() {
            // Do nothing
        }

        @SuppressWarnings("null")
        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_Threadid);
        }

        @Override
        public String getName() {
            return "ThreadID"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof Span && segment2 instanceof Span) {
                    int res = ((Span) segment1).getName().compareToIgnoreCase(((Span) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof Span) {
                return ((Span) segment).getTid();
            }
            return null;
        }
    }

    /*
     * Define the rest of the metrics columns of the segment store table
     */
    private static final class SpanState implements ISegmentAspect {
        private KernelMetrics.StateName fstateName;

        private SpanState(KernelMetrics.StateName stateName) {
            this.fstateName = stateName;
        }

        @SuppressWarnings("null")
        @Override
        public String getHelpText() {
            return fstateName.toString();
        }

        @SuppressWarnings("null")
        @Override
        public String getName() {
            return fstateName.toString();
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof Span && segment2 instanceof Span) {
                    int res = ((Span) segment1).getName().compareToIgnoreCase(((Span) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof Span) {
                return Long.toString(((Span) segment).getMetrics().getStateValue(this.fstateName));
            }
            return null;
        }
    }


    /*
     * Define the rest of the metrics columns of the segment store table
     */


    private static final class SyscallState implements ISegmentAspect {
        private KernelMetrics.SyscallName fsyscallName;

        private SyscallState(KernelMetrics.SyscallName syscallName) {
            this.fsyscallName = syscallName;
        }

        @SuppressWarnings("null")
        @Override
        public String getHelpText() {
            return fsyscallName.toString();
        }


        @SuppressWarnings("null")
        @Override
        public String getName() {
            return fsyscallName.toString();
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof Span && segment2 instanceof Span) {
                    int res = ((Span) segment1).getName().compareToIgnoreCase(((Span) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof Span) {
                //return Long.toString(new KernelMetrics().getSysCallValue(this.fsyscallName));
                return Long.toString(((Span) segment).getMetrics().getSysCallValue(this.fsyscallName));
            }
            return null;
        }
    }

    /** IMetricfactory */
    protected interface IMetricfactory {

    }
}
