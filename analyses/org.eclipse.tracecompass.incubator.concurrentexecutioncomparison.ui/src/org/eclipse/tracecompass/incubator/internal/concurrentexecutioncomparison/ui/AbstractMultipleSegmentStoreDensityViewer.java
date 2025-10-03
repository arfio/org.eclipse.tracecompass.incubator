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
package org.eclipse.tracecompass.incubator.internal.concurrentexecutioncomparison.ui;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.model.DoubleArraySeriesModel;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density2.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density2.ISegmentStoreDensityViewerDataListener;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.format.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.table.SegmentStoreContentProvider.SegmentStoreWithRange;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.AxisRange;

/**
 * Displays the segment store provider data in a density chart. its very similar
 * to AbstractMultipleSegmentStoreDensityViewer, but it is used when we want to
 * feed the chart from one of the attributes of a segments providing more than
 * one attribute. the Segment.getlength is replaced with getLength function that
 * can be override in different extends to be related to a specefic attribute of
 * a segment. The serieType is assumed to be BAR.
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public abstract class AbstractMultipleSegmentStoreDensityViewer extends AbstractSegmentStoreDensityViewer {
    private Chart fMultiChart;
    private static final RGB Multi_BAR_COLOR = new RGB(0x42, 0x85, 0xf4);
    private static final ColorRegistry Multi_COLOR_REGISTRY = new ColorRegistry();
    public static final Pair<Double, Double> DEFAULT_RANGE = new Pair<>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

    private AxisRange fMultiCurrentDurationRange = new AxisRange(DEFAULT_RANGE.getFirst(), DEFAULT_RANGE.getSecond());
    private TmfTimeRange fMultiCurrentTimeRange = TmfTimeRange.NULL_RANGE;

    private static final Format Multi_DENSITY_TIME_FORMATTER = SubSecondTimeWithUnitFormat.getInstance();
    private final List<ISegmentStoreDensityViewerDataListener> fMultiListeners;

    /**
     * Constructs a new density viewer.
     *
     * @param parent
     *            the parent of the viewer
     */
    public AbstractMultipleSegmentStoreDensityViewer(Composite parent) {
        super(parent);
        fMultiChart = getControl();
        fMultiListeners = new ArrayList<>();
        IAxis xAxis = fMultiChart.getAxisSet().getXAxis(0);
        xAxis.getTitle().setText(nullToEmptyString(getTimeAxisLable()));

    }

    protected long getSegmentLength(ISegment segment) {
        return segment.getLength();
    }

    protected Comparator getComparator() {
        return SegmentComparators.INTERVAL_LENGTH_COMPARATOR;
    }

    protected String getTimeAxisLable() {

        return Messages.AbstractMultipleSegmentStoreDensityViewer_TimeAxisLabel;
    }

    private synchronized void updateDisplay(Iterable<ISegment> data) {
        ISeries<Integer> series = createSeries();
        int barWidth = 4;
        int preWidth = fMultiChart.getPlotArea().getSize().x / barWidth;
        final int width = preWidth;
        double[] xOrigSeries = new double[width];
        double[] yOrigSeries = new double[width];
        // Set a positive value that is greater than 0 and less than 1.0
        Arrays.fill(yOrigSeries, Double.MIN_VALUE);
        Optional<ISegment> maxSegment = StreamSupport.stream(data.spliterator(), false).max(getComparator());
        long maxLength = Long.MIN_VALUE;
        if (maxSegment.isPresent()) {
            maxLength = getSegmentLength(maxSegment.get());
        } else {
            for (ISegment segment : data) {
                maxLength = Math.max(maxLength, getSegmentLength(segment));
            }
            if (maxLength == Long.MIN_VALUE) {
                maxLength = 1;
            }
        }
        double maxFactor = 1.0 / (maxLength + 1.0);
        long minX = Long.MAX_VALUE;
        for (ISegment segment : data) {
            double xBox = getSegmentLength(segment) * maxFactor * width;
            if (yOrigSeries[(int) xBox] < 1) {
                yOrigSeries[(int) xBox] = 1;
            } else {
                yOrigSeries[(int) xBox]++;
            }
            minX = Math.min(minX, getSegmentLength(segment));
        }
        double timeWidth = (double) maxLength / (double) width;
        for (int i = 0; i < width; i++) {
            xOrigSeries[i] = i * timeWidth;
        }
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < width; i++) {
            maxY = Math.max(maxY, yOrigSeries[i]);
        }
        if (minX == maxLength) {
            maxLength++;
            minX--;
        }
        series.setDataModel(new DoubleArraySeriesModel(xOrigSeries, yOrigSeries));
        final IAxis xAxis = fMultiChart.getAxisSet().getXAxis(0);
        /*
         * adjustrange appears to bring origin back since we pad the series with
         * 0s, not interesting.
         */
        AxisRange currentDurationRange = fMultiCurrentDurationRange;
        if (Double.isFinite(currentDurationRange.getLower()) && Double.isFinite(currentDurationRange.getUpper())) {
            xAxis.setRange(new Range(currentDurationRange.getLower(), currentDurationRange.getUpper()));
        } else {
            xAxis.adjustRange();
        }

        xAxis.getTick().setFormat(Multi_DENSITY_TIME_FORMATTER);
        /*
         * Clamp range lower to 0.9 to make it log, 0.1 would be scientifically
         * accurate, but we cannot have partial counts.
         */
        for (ISeries<?> internalSeries : fMultiChart.getSeriesSet().getSeries()) {
            maxY = Math.max(maxY, internalSeries.getDataModel().getMaxY().doubleValue());
        }
        fMultiChart.getAxisSet().getYAxis(0).setRange(new Range(0.9, Math.max(1.0, maxY)));
        fMultiChart.getAxisSet().getYAxis(0).enableLogScale(true);
        new Thread(() -> {
            for (ISegmentStoreDensityViewerDataListener l : fMultiListeners) {
                l.chartUpdated();
            }
        }).start();
    }

    public void updateModel(Iterable<? extends ISegment> data) {
        Iterable<ISegment> data2 = (Iterable<ISegment>) data;
        Display.getDefault().asyncExec(() -> updateDisplay(data2));

    }

    private ISeries<Integer> createSeries() {
        IBarSeries<Integer> series = (IBarSeries<Integer>) fMultiChart.getSeriesSet().createSeries(SeriesType.BAR, Messages.AbstractMultiSegmentStoreDensityViewer_SeriesLabel);
        series.setVisible(true);
        series.setBarPadding(0);
        series.setBarColor(getColorForRGB(Multi_BAR_COLOR));
        return series;
    }

    private static Color getColorForRGB(RGB rgb) {
        String rgbString = rgb.toString();
        Color color = Multi_COLOR_REGISTRY.get(rgbString);
        if (color == null) {
            Multi_COLOR_REGISTRY.put(rgbString, rgb);
            color = Objects.requireNonNull(Multi_COLOR_REGISTRY.get(rgbString));
        }
        return color;
    }

    @Override
    public void select(final AxisRange durationRange) {
        fMultiCurrentDurationRange = durationRange;
        final TmfTimeRange timeRange = fMultiCurrentTimeRange;
        computeDataAsync(timeRange, durationRange).thenAccept(data -> {
            synchronized (fMultiListeners) {
                if (fMultiCurrentTimeRange.equals(timeRange) && fMultiCurrentDurationRange.equals(durationRange)) {
                    for (ISegmentStoreDensityViewerDataListener listener : fMultiListeners) {
                        for (SegmentStoreWithRange<ISegment> value : data.values()) {
                            listener.selectedDataChanged(value);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void zoom(final AxisRange durationRange) {
        fMultiCurrentDurationRange = durationRange;
        final TmfTimeRange timeRange = fMultiCurrentTimeRange;
        computeDataAsync(timeRange, durationRange).thenAccept(data -> {
            synchronized (fMultiListeners) {
                if (fMultiCurrentTimeRange.equals(timeRange) && fMultiCurrentDurationRange.equals(durationRange)) {
                    applyData(data);
                }
            }
        });
    }

    @Override
    public void updateWithRange(final TmfTimeRange timeRange) {
        fMultiCurrentTimeRange = timeRange;
        final AxisRange durationRange = getDefaultRange();
        fMultiCurrentDurationRange = durationRange;
        computeDataAsync(timeRange, durationRange).thenAccept(data -> {
            synchronized (fMultiListeners) {
                if (fMultiCurrentTimeRange.equals(timeRange) && fMultiCurrentDurationRange.equals(durationRange)) {
                    applyData(data);
                }
            }
        });
    }

    /**
     * Add a data listener.
     *
     * @param dataListener
     *            the data listener to add
     */
    @Override
    public void addDataListener(ISegmentStoreDensityViewerDataListener dataListener) {
        fMultiListeners.add(dataListener);
    }

    /**
     * Remove a data listener.
     *
     * @param dataListener
     *            the data listener to remove
     */
    @Override
    public void removeDataListener(ISegmentStoreDensityViewerDataListener dataListener) {
        fMultiListeners.remove(dataListener);
    }

    private static AxisRange getDefaultRange() {
        return new AxisRange(DEFAULT_RANGE.getFirst(), DEFAULT_RANGE.getSecond());
    }

    @Override
    @TmfSignalHandler
    public void windowRangeUpdated(@Nullable TmfWindowRangeUpdatedSignal signal) {
        if (signal == null) {
            return;
        }
        fMultiCurrentTimeRange = NonNullUtils.checkNotNull(signal.getCurrentRange());
        super.windowRangeUpdated(signal);
    }

    @Override
    protected void loadTrace(@Nullable ITmfTrace trace) {
        TmfTraceContext ctx = TmfTraceManager.getInstance().getCurrentTraceContext();
        TmfTimeRange windowRange = ctx.getWindowRange();
        fMultiCurrentTimeRange = windowRange;
        super.loadTrace(trace);
    }

    private static ITmfTrace getTrace() {
        return TmfTraceManager.getInstance().getActiveTrace();
    }

    private void applyData(final Map<String, SegmentStoreWithRange<ISegment>> map) {
        Set<Entry<String, SegmentStoreWithRange<ISegment>>> entrySet = map.entrySet();
        if (entrySet.isEmpty()) {
            return;
        }
        entrySet.parallelStream().forEach(entry -> {
            SegmentStoreWithRange<ISegment> data = Objects.requireNonNull(entry.getValue());
            data.setComparator(SegmentComparators.INTERVAL_LENGTH_COMPARATOR);
            Display.getDefault().asyncExec(() -> updateDisplay(data));
            for (ISegmentStoreDensityViewerDataListener l : fMultiListeners) {
                l.viewDataChanged(data);
            }
        });
        fMultiChart.redraw();
    }

    private CompletableFuture<Map<String, SegmentStoreWithRange<ISegment>>> computeDataAsync(final TmfTimeRange timeRange, final AxisRange durationRange) {
        return CompletableFuture.supplyAsync(() -> computeData(timeRange, durationRange));
    }

    private @Nullable Map<String, SegmentStoreWithRange<ISegment>> computeData(final TmfTimeRange timeRange, final AxisRange durationRange) {
        Map<String, SegmentStoreWithRange<ISegment>> retVal = new HashMap<>();
        final ISegmentStoreProvider segmentProvider = getSegmentStoreProvider(getTrace());
        final ISegmentStore<ISegment> segStore = segmentProvider.getSegmentStore();
        if (segStore != null) {

            // Filter on the segment duration if necessary
            if (durationRange.getLower() > Double.MIN_VALUE || durationRange.getUpper() < Double.MAX_VALUE) {
                Predicate<ISegment> predicate = segment -> getSegmentLength(segment) >= durationRange.getLower() && getSegmentLength(segment) <= durationRange.getUpper();
                retVal.put(getTrace().getName(), new SegmentStoreWithRange<>(segStore, timeRange, predicate));
            } else {
                retVal.put(getTrace().getName(), new SegmentStoreWithRange<>(segStore, timeRange));
            }
        }

        return retVal;
    }

    @Override
    public void dispose() {
        if (!fMultiChart.isDisposed()) {
            fMultiChart.dispose();
        }
        super.dispose();
    }

}
