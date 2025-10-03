package org.eclipse.tracecompass.incubator.internal.concurrentexecutioncomparison.ui;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Composite;

public class ExecutionComparisonView extends AbstractMultipleDensityView {

    /** The view's ID */
    public static final @NonNull String id = "org.eclipse.tracecompass.incubator.internal.concurrentexecutioncomparison.ui.execComparison"; //$NON-NLS-1$

    /**
     * Constructs a new density view.
     */
    public ExecutionComparisonView() {
        super(id);
    }


    @Override
    protected ExecutionComparisonDurationViewer createDurationSegmentStoreDensityViewer(Composite parent) {
        return new ExecutionComparisonDurationViewer(Objects.requireNonNull(parent));
    }

    @Override
    protected ExecutionComparisonSelfTimeViewer createSelfTimeSegmentStoreDensityViewer(Composite parent) {
        return new ExecutionComparisonSelfTimeViewer(Objects.requireNonNull(parent));
    }








}