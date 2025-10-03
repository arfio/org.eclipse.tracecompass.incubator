/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.incubator.concurrentcallstack.core.SpanCallStackAnalysis;
import org.eclipse.tracecompass.incubator.concurrentcallstack.core.SpanFlameStateProvider;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunctionStatistics;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.ImmutableList;

/**
 * it Generates a segment stors of execution metrics which will be used to fill
 * differential flame graph view
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public class ExecutionComparisonSegmentStoreAnalysis extends AbstractSegmentStoreAnalysisModule {

    /**
     * The ID of this analysis
     */
    public static final String ANALYSIS_ID = "org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.exeComparison"; //$NON-NLS-1$
    private CallGraph fCallGraph = null;

    private SpanCallStackAnalysis fIcsaModule;
    private long fDuration;
    private long fSelfTime;
    private long fstartTime;
    private long fendTime;

    private final @NonNull Collection<ISegmentAspect> BASE_ASPECTS = ImmutableList.of(
            DurationAspect.INSTANCE,
            SelfTimeAspect.INSTANCE);

    // Constructor

    public ExecutionComparisonSegmentStoreAnalysis() {

    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    @Override
    protected boolean buildAnalysisSegments(@NonNull ISegmentStore<@NonNull ISegment> segmentStore, @NonNull IProgressMonitor monitor) throws TmfAnalysisException {

        // it is used to filter unwanted processes in race relating to config ,
        // ...
        Map<String, List<ExecutionMetrics>> cgTypes = new HashMap<String, List<ExecutionMetrics>>();

        TmfTraceManager traceManager = TmfTraceManager.getInstance();
        ITmfTrace trace = traceManager.getActiveTrace();

        List<ITmfTrace> traces = new ArrayList<ITmfTrace>();

        if (trace instanceof TmfExperiment) {
            TmfExperiment exp = (TmfExperiment) trace;
            traces.addAll(exp.getTraces());
        } else {
            traces.add(trace);
        }

        for (ITmfTrace t : traces) {

            if ((trace instanceof IKernelTrace) == false) {
                fIcsaModule = new SpanCallStackAnalysis() {

                    @Override
                    protected ITmfStateProvider createStateProvider() {
                        return new SpanFlameStateProvider(Objects.requireNonNull(t));

                    }
                };

                fIcsaModule.setTrace(t);
                fstartTime = t.getStartTime().toNanos();
                fendTime = t.getEndTime().toNanos();

                fIcsaModule.schedule();
                boolean completed = fIcsaModule.waitForCompletion();
                if (completed) {
                    fCallGraph = fIcsaModule.getCallGraph();
                } else {
                    return false;
                }

                Collection<ICallStackElement> processes = fCallGraph.getElements();

                assertNotNull(processes);

                for (ICallStackElement process : processes) {
                    assertNotNull(process);
                    Object[] children = fCallGraph.getCallingContextTree(process).toArray();
                    SpanAggregatedCalledFunction firstFunction = (SpanAggregatedCalledFunction) children[0];
                    AggregatedCalledFunctionStatistics mainStatistics = firstFunction.getFunctionStatistics();

                    fDuration = (long) mainStatistics.getDurationStatistics().getMean();
                    fSelfTime = (long) mainStatistics.getSelfTimeStatistics().getMean();
                    ExecutionMetrics Em = new ExecutionMetrics(fstartTime, fendTime, fDuration, fSelfTime, firstFunction, process.getName());

                    if (cgTypes.get(firstFunction.getObject().toString()) == null) {
                        List<ExecutionMetrics> processList = new ArrayList<ExecutionMetrics>();
                        processList.add(Em);
                        cgTypes.put(firstFunction.getObject().toString(), processList);

                    } else {
                        cgTypes.get(firstFunction.getObject().toString()).add(Em);
                    }

                }
                fIcsaModule.dispose();

            }

        }
        /// find the biggest group, we assumed this is the main group of process
        /// to compare
        int size = 0;
        String mainFunc = null;

        for (String function : cgTypes.keySet()) {
            if (cgTypes.get(function).size() > size) {
                size = cgTypes.get(function).size();
                mainFunc = function;
            }
        }
        if (mainFunc != null) {
            for (ExecutionMetrics em : cgTypes.get(mainFunc)) {
                segmentStore.add(em);
            }
        }

        if (segmentStore.size() < 2) {
            throw new IllegalStateException("There are not enough processes to compare");
        }
        Boolean multiExec = true;
        if (segmentStore.size() == 2) {
            multiExec = false;
        }

        ExecutionMetrics firstEM = (ExecutionMetrics) (segmentStore.stream().findFirst().orElse(null));
        ExecutionMetrics secondEM = (ExecutionMetrics) (segmentStore.stream().skip(1).findFirst().orElse(null));

        SpanAggregatedCalledFunction processA = firstEM.getProcess();
        SpanAggregatedCalledFunction processB = secondEM.getProcess();

        TmfMultiComparisonSignal signal = new TmfMultiComparisonSignal(this, multiExec, processA, firstEM.getProcessName(), processB, secondEM.getProcessName());
        TmfSignalManager.dispatchSignal(signal);

        return true;

    }

    final static class DurationAspect implements ISegmentAspect {
        public final static ISegmentAspect INSTANCE = new DurationAspect();

        private DurationAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_Duration);
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SegmentAspectName_Duration);
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
                if (segment1 instanceof ExecutionMetrics && segment2 instanceof ExecutionMetrics) {
                    int res = Double.compare(((ExecutionMetrics) segment1).getDuration(), ((ExecutionMetrics) segment2).getDuration());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public Long resolve(ISegment segment) {
            if (segment instanceof ExecutionMetrics) {
                return ((ExecutionMetrics) segment).getDuration();
            }
            return -1l;
        }
    }

    final static class SelfTimeAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SelfTimeAspect();

        private SelfTimeAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SelfTime);
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SegmentAspectName_SelfTime);
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
                if (segment1 instanceof ExecutionMetrics && segment2 instanceof ExecutionMetrics) {
                    int res = Double.compare(((ExecutionMetrics) segment1).getSelfTime(), ((ExecutionMetrics) segment2).getSelfTime());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public Long resolve(ISegment segment) {
            if (segment instanceof ExecutionMetrics) {
                return ((ExecutionMetrics) segment).getSelfTime();
            }
            return -1l;
        }
    }

    @Override
    protected void canceling() {
        // TODO Auto-generated method stub

    }

}