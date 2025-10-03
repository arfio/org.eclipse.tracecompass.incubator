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

package org.eclipse.tracecompass.incubator.internal.concurrentexecutioncomparison.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density2.ISegmentStoreDensityViewerDataListener;
import org.eclipse.tracecompass.incubator.concurrentcallstack.core.SpanFlameAnalysis;
import org.eclipse.tracecompass.incubator.concurrentcallstack.core.SpanFlameStateProvider;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.DifferentialCallGraphAnalysis;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.ExecutionMetrics;
import org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core.TmfMultiComparisonSignal;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAggregatedCalledFunction;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Differential Falme Graph View
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public abstract class AbstractMultipleDensityView extends DifferentialFlameGraphView {

    private static final int[] DEFAULT_WEIGHTS = new int[] { 6, 4 };
    private static final int[] DEFAULT_WEIGHTSFilteringV = new int[] { 495, 10, 495 };
    private static final int[] DEFAULT_WEIGHTSFilteringH = new int[] { 1, 3, 3, 3 };
    private static final int[] DEFAULT_WEIGHTSTwoReq = new int[] { 0, 10 };

    /**
     * Default zoom range
     *
     * @since 4.1
     */
    public static final Pair<Double, Double> DEFAULT_RANGE = new Pair<>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

    private @Nullable ExecutionComparisonDurationViewer fDurationDensityViewerGroupA;
    private @Nullable ExecutionComparisonSelfTimeViewer fSelfTimeDensityViewerGroupA;

    private @Nullable ExecutionComparisonDurationViewer fDurationDensityViewerGroupB;
    private @Nullable ExecutionComparisonSelfTimeViewer fSelfTimeDensityViewerGroupB;

    private Map<String, SpanAggregatedCalledFunction> fGroupAProcesses = new HashMap<String, SpanAggregatedCalledFunction>();

    private Map<String, SpanAggregatedCalledFunction> fGroupBProcesses = new HashMap<String, SpanAggregatedCalledFunction>();

    private Map<String, SpanAggregatedCalledFunction> fGroupAProcessesBackup;
    private Map<String, SpanAggregatedCalledFunction> fGroupBProcessesBackup;

    SashForm fsashForm = null;
    Table fRequestsA = null;
    Table fRequestsB = null;
    SpanAggregatedCalledFunction fmergedGroupACG;
    SpanAggregatedCalledFunction fmergedGroupBCG;

    /**
     * Constructs a segment store density view
     *
     * @param viewName
     *            the name of the view
     */
    public AbstractMultipleDensityView(String viewName) {
        super(viewName);
    }

    /**
     * Used to keep the density chart in sync with Duration chart. And build
     * differential flame graph based on selected data in Group A
     */
    private final class DurationDataChangedListenerA implements ISegmentStoreDensityViewerDataListener {

        private void updateDensityModel(@Nullable Iterable<? extends ISegment> data) {
            final Display display = Display.getDefault();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    fRequestsA.removeAll();
                    for (ISegment em : data) {
                        TableItem item = new TableItem(fRequestsA, SWT.NONE);
                        item.setText(((ExecutionMetrics) em).toString());
                        item.setChecked(true);
                    }
                }
            });

            fGroupAProcesses = getProcesses(data);
            fGroupAProcessesBackup = getProcesses(data);
            AbstractMultipleSegmentStoreDensityViewer viewer = fSelfTimeDensityViewerGroupA;

            if (viewer != null && data != null) {
                viewer.updateModel(data);
            }

            buildDifferetialFlameGraph();

        }

        @Override
        public void viewDataChanged(@Nullable Iterable<? extends ISegment> data) {
            updateDensityModel(data);
        }

        @Override
        public void selectedDataChanged(@Nullable Iterable<? extends ISegment> data) {
            updateDensityModel(data);

        }

    }

    /**
     * Used to keep the density chart in sync with Duration chart. And build
     * differential flame graph based on selected data in Group B
     */

    private final class DurationDataChangedListenerB implements ISegmentStoreDensityViewerDataListener {

        private void updateDensityModel(@Nullable Iterable<? extends ISegment> data) {
            final Display display = Display.getDefault();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    fRequestsB.removeAll();
                    for (ISegment em : data) {
                        TableItem item = new TableItem(fRequestsB, SWT.NONE);
                        item.setText(((ExecutionMetrics) em).toString());
                        item.setChecked(true);
                    }
                }
            });

            fGroupBProcesses = getProcesses(data);
            fGroupBProcessesBackup = getProcesses(data);

            AbstractMultipleSegmentStoreDensityViewer viewer = fSelfTimeDensityViewerGroupB;
            if (viewer != null && data != null) {
                viewer.updateModel(data);
            }

            buildDifferetialFlameGraph();

        }

        @Override
        public void viewDataChanged(@Nullable Iterable<? extends ISegment> data) {
            // updateDensityModel(data);
        }

        @Override
        public void selectedDataChanged(@Nullable Iterable<? extends ISegment> data) {
            updateDensityModel(data);

        }

    }

    /**
     * Used to keep the duration chart in sync with selftime chart. And build
     * differential flame graph based on selected data in Group A
     */
    private final class SelfTimeDataChangedListenerA implements ISegmentStoreDensityViewerDataListener {

        private void updateDensityModel(@Nullable Iterable<? extends ISegment> data) {
            final Display display = Display.getDefault();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    fRequestsA.removeAll();
                    for (ISegment em : data) {
                        TableItem item = new TableItem(fRequestsA, SWT.NONE);
                        item.setText(((ExecutionMetrics) em).toString());
                        item.setChecked(true);
                    }
                }
            });

            fGroupAProcesses = getProcesses(data);
            fGroupAProcessesBackup = getProcesses(data);

            AbstractMultipleSegmentStoreDensityViewer viewer = fDurationDensityViewerGroupA;
            if (viewer != null && data != null) {
                viewer.updateModel(data);
            }

            buildDifferetialFlameGraph();
        }

        @Override
        public void viewDataChanged(@Nullable Iterable<? extends ISegment> data) {
            // updateDensityModel(data);
        }

        @Override
        public void selectedDataChanged(@Nullable Iterable<? extends ISegment> data) {
            updateDensityModel(data);

        }

    }

    /**
     * Used to keep the duration chart in sync with selftime chart. And build
     * differential flame graph based on selected data in Group B
     */

    private final class SelfTimeDataChangedListenerB implements ISegmentStoreDensityViewerDataListener {

        private void updateDensityModel(@Nullable Iterable<? extends ISegment> data) {
            final Display display = Display.getDefault();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    fRequestsB.removeAll();
                    for (ISegment em : data) {
                        TableItem item = new TableItem(fRequestsB, SWT.NONE);
                        item.setText(((ExecutionMetrics) em).toString());
                        item.setChecked(true);
                    }
                }
            });

            fGroupBProcesses = getProcesses(data);
            fGroupBProcessesBackup = getProcesses(data);

            AbstractMultipleSegmentStoreDensityViewer viewer = fDurationDensityViewerGroupB;
            if (viewer != null && data != null) {
                viewer.updateModel(data);
            }

            buildDifferetialFlameGraph();
        }

        @Override
        public void viewDataChanged(@Nullable Iterable<? extends ISegment> data) {
            // updateDensityModel(data);
        }

        @Override
        public void selectedDataChanged(@Nullable Iterable<? extends ISegment> data) {
            updateDensityModel(data);

        }

    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        TmfSignalManager.register(this);
        final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        fsashForm = sashForm;
        SashForm sashFormFiltering = new SashForm(sashForm, SWT.HORIZONTAL);

        SashForm sashFormGroupA = new SashForm(sashFormFiltering, SWT.VERTICAL);

        SashForm distance = new SashForm(sashFormFiltering, SWT.NONE);
        distance.pack();

        SashForm sashFormGroupB = new SashForm(sashFormFiltering, SWT.VERTICAL);

        Text labelGroupA = new Text(sashFormGroupA, SWT.BORDER | SWT.CENTER);
        labelGroupA.setText("GroupA");
        sashFormGroupA.setLayout(new FillLayout());

        Text labelGroupB = new Text(sashFormGroupB, SWT.BORDER | SWT.CENTER);
        labelGroupB.setText("GroupB");

        fDurationDensityViewerGroupA = createDurationSegmentStoreDensityViewer(sashFormGroupA);
        fDurationDensityViewerGroupB = createDurationSegmentStoreDensityViewer(sashFormGroupB);

        fSelfTimeDensityViewerGroupA = createSelfTimeSegmentStoreDensityViewer(sashFormGroupA);
        fSelfTimeDensityViewerGroupB = createSelfTimeSegmentStoreDensityViewer(sashFormGroupB);

        Table requestsA = new Table(sashFormGroupA, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        requestsA.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.detail == SWT.CHECK) {
                    TableItem ti = (TableItem) event.item;
                    if (ti.getChecked()) {
                        String reqName = getProcessName(ti.getText());
                        fGroupAProcesses.put(reqName, fGroupAProcessesBackup.get(reqName));

                        // redraw groupA flame graph
                        buildDifferetialFlameGraph();
                    } else {
                        // remove request from fGroupAProcesses
                        String reqName = getProcessName(ti.getText());
                        fGroupAProcesses.remove(reqName);

                        // redraw groupA flame graph
                        buildDifferetialFlameGraph();
                    }
                }
            }
        });
        fRequestsA = requestsA;

        Table requestsB = new Table(sashFormGroupB, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        requestsB.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.detail == SWT.CHECK) {
                    TableItem ti = (TableItem) event.item;
                    if (ti.getChecked()) {
                        String reqName = getProcessName(ti.getText());
                        fGroupBProcesses.put(reqName, fGroupBProcessesBackup.get(reqName));

                        // redraw differential flame graph
                        buildDifferetialFlameGraph();
                    } else {
                        // remove request from fGroupBProcesses
                        String reqName = getProcessName(ti.getText());
                        fGroupBProcesses.remove(reqName);

                        // redraw differential flame graph
                        buildDifferetialFlameGraph();
                    }
                }
            }
        });
        fRequestsB = requestsB;

        sashFormGroupA.setWeights(DEFAULT_WEIGHTSFilteringH);
        sashFormGroupB.setWeights(DEFAULT_WEIGHTSFilteringH);

        sashFormFiltering.setWeights(DEFAULT_WEIGHTSFilteringV);

        super.createPartControl(sashForm);

        sashForm.setWeights(DEFAULT_WEIGHTS);

        fDurationDensityViewerGroupA.addDataListener(new DurationDataChangedListenerA());
        fSelfTimeDensityViewerGroupA.addDataListener(new SelfTimeDataChangedListenerA());

        fDurationDensityViewerGroupB.addDataListener(new DurationDataChangedListenerB());
        fSelfTimeDensityViewerGroupB.addDataListener(new SelfTimeDataChangedListenerB());

    }

    /**
     * Create a density viewer suitable for displaying the segment store content
     * for Execution Duration.
     *
     * @param parent
     *            the parent composite
     * @return the duration viewer
     */
    protected abstract ExecutionComparisonDurationViewer createDurationSegmentStoreDensityViewer(Composite parent);

    /**
     * Create a density viewer suitable for displaying the segment store content
     * for Execution selfTime.
     *
     * @param parent
     *            the parent composite
     * @return the density viewer
     */
    protected abstract ExecutionComparisonSelfTimeViewer createSelfTimeSegmentStoreDensityViewer(Composite parent);

    @Override
    public void setFocus() {
        final ExecutionComparisonDurationViewer viewer = fDurationDensityViewerGroupA;
        if (viewer != null) {
            viewer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {

        TmfSignalManager.deregister(this);
        if (fDurationDensityViewerGroupA != null) {
            fDurationDensityViewerGroupA.dispose();
        }
        if (fDurationDensityViewerGroupB != null) {
            fDurationDensityViewerGroupB.dispose();
        }

        if (fSelfTimeDensityViewerGroupA != null) {
            fSelfTimeDensityViewerGroupA.dispose();
        }

        if (fSelfTimeDensityViewerGroupB != null) {
            fSelfTimeDensityViewerGroupB.dispose();
        }
        super.dispose();
    }

    private static Map<String, SpanAggregatedCalledFunction> getProcesses(@Nullable Iterable<? extends ISegment> data) {
        Map<String, SpanAggregatedCalledFunction> elementList = new HashMap<String, SpanAggregatedCalledFunction>();
        for (ISegment em : data) {
            elementList.put(((ExecutionMetrics) em).getProcessName(), ((ExecutionMetrics) em).getProcess());
        }
        return elementList;

    }

    @TmfSignalHandler
    public void multiComparison(final TmfMultiComparisonSignal signal) {

        if (!signal.getMultiCompare()) {

            final Display display = Display.getDefault();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    fsashForm.setWeights(DEFAULT_WEIGHTSTwoReq);

                }
            });

            fGroupAProcesses = new HashMap<String, SpanAggregatedCalledFunction>();
            fGroupAProcesses.put(signal.getProcessNameA(), signal.getProcessA());

            fGroupBProcesses = new HashMap<String, SpanAggregatedCalledFunction>();
            fGroupBProcesses.put(signal.getProcessNameB(), signal.getProcessB());

            buildDifferetialFlameGraph();
        }
    }

    private static String getProcessName(String text) {
        int fromIndx = 10;// after Process =
        int toIndx = text.indexOf(";");
        return text.substring(fromIndx, toIndx);
    }

    @SuppressWarnings("null")
    @Override
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        super.traceSelected(signal);
        fDurationDensityViewerGroupA.traceSelected(signal);
        fDurationDensityViewerGroupB.traceSelected(signal);
        fSelfTimeDensityViewerGroupA.traceSelected(signal);
        fSelfTimeDensityViewerGroupB.traceSelected(signal);
    }

    private void buildDifferetialFlameGraph() {

        IWeightedTreeProvider<ICallStackSymbol, ?, ? extends WeightedTree<ICallStackSymbol>> provider;
        SpanFlameAnalysis CSAModule = new SpanFlameAnalysis() {

            @Override
            protected ITmfStateProvider createStateProvider() {
                return new SpanFlameStateProvider(Objects.requireNonNull(getTrace()));

            }

        };
        provider = CSAModule;

        ITmfTrace activetrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activetrace != null) {

            DifferentialCallGraphAnalysis diffCG = new DifferentialCallGraphAnalysis(fGroupAProcesses.values(), fGroupBProcesses.values(), (IWeightedTreeProvider<ICallStackSymbol, ?, WeightedTree<ICallStackSymbol>>) provider);
            setDiffCallGrapProvider(diffCG);

            TmfTraceSelectedSignal signal = new TmfTraceSelectedSignal(this, activetrace);
            traceSelected(signal);
            CSAModule.dispose();
            diffCG.dispose();
        }

    }

}