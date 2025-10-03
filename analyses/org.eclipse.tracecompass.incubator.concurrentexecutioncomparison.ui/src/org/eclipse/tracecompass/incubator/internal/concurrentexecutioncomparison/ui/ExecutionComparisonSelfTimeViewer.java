package org.eclipse.tracecompass.incubator.internal.concurrentexecutioncomparison.ui;


import java.util.Comparator;
//import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.ExecutionComparisonSegmentStoreAnalysis;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.ExecutionMetrics;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
//import org.eclipse.tracecompass.incubator.cuncurrentexecutioncomparision.core.ExecutionMetrics;
//import org.eclipse.tracecompass.incubator.internal.executioncomparision.core.ExecutionComparisionSegmentStoreAnalysis;
//import org.eclipse.tracecompass.incubator.internal.executioncomparision.core.ExecutionMetrics;
//import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;



/**
 * Displays the segment store provider data in a density chart for execution comparision.
 *
 */

public class ExecutionComparisonSelfTimeViewer extends AbstractMultipleSegmentStoreDensityViewer {

    public ExecutionComparisonSelfTimeViewer(@NonNull Composite parent) {
        super(parent);
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(ITmfTrace trace) {
        return TmfTraceUtils.getAnalysisModuleOfClass(trace, ExecutionComparisonSegmentStoreAnalysis.class,
                ExecutionComparisonSegmentStoreAnalysis.ANALYSIS_ID);

    }

    @Override
    protected long getSegmentLength(ISegment segment) {
        ExecutionMetrics Em = (ExecutionMetrics) segment;
        return Em.getSelfTime();
    }
    @Override
    protected Comparator getComparator(){
        return (Objects.requireNonNull(Comparator.comparingLong(ExecutionMetrics::getSelfTime)));

   }
    @Override
    protected String getTimeAxisLable() {
        return Messages.ExecutionComparisonSelfTimeViewer_TimeAxisLabel;
    }




}