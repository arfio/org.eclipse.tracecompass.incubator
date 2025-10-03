package org.eclipse.tracecompass.incubator.internal.concurrentexecutioncomparison.ui;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.ExecutionComparisonSegmentStoreAnalysis;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.ExecutionMetrics;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Displays the segment store provider data in a density chart for execution
 * comparison.
 *
 * @author Matthew Khouzam
 * @author Marc-Andre Laperle
 *
 * @since 4.1
 */
public class ExecutionComparisonDurationViewer extends AbstractMultipleSegmentStoreDensityViewer {

    public ExecutionComparisonDurationViewer(@NonNull Composite parent) {
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
        return Em.getDuration();
    }

    @Override
    protected Comparator getComparator() {
        return (Objects.requireNonNull(Comparator.comparingLong(ExecutionMetrics::getDuration)));

    }

    @Override
    protected String getTimeAxisLable() {
        return Messages.ExecutionComparisonDurationViewer_TimeAxisLabel;
    }

    // @Override
    // @TmfSignalHandler
    // public void traceSelected(TmfTraceSelectedSignal signal) {
    // loadTrace(TmfTraceManager.getInstance().getActiveTrace());
    // }

 /*   public void updateModel(Iterable<? extends ISegment> data) {

        //Iterator iter = fTraces.iterator();
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        String name = trace.getName();
        System.out.println(name + "       ooomadam      " + Iterables.size(data));
        Iterable<ISegment> data2 = (Iterable<ISegment>) data;
        Display.getDefault().asyncExec(() -> updateDisplay(name, data2));

    }*/

}