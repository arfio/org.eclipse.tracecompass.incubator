/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.tests.flamechart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartDataProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Test the {@link FlameChartDataProvider} class
 *
 * @author Geneviève Bastien
 */
public class FlameChartDataProviderTest extends CallStackTestBase {

    private static final @Nullable IProgressMonitor MONITOR = new NullProgressMonitor();

    private FlameChartDataProvider getDataProvider() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        FlameChartDataProviderFactory factory = new FlameChartDataProviderFactory();

        FlameChartDataProvider dataProvider = (FlameChartDataProvider) factory.createProvider(getTrace(), module.getId());
        assertNotNull(dataProvider);
        return dataProvider;
    }

    /**
     * Test getting the descriptors built in the flame chart data provider factory
     */
    @Test
    public void testGetDescriptors() {
        FlameChartDataProviderFactory dataProviderFactory = new FlameChartDataProviderFactory();
        Collection<IDataProviderDescriptor> descriptors = dataProviderFactory.getDescriptors(getTrace());
        assertEquals(descriptors.size(), 2);

        for (IDataProviderDescriptor descriptor : descriptors) {
            switch(descriptor.getId()) {
            case "org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.flamechart:org.eclipse.tracecompass.incubator.callstack.analysis.test":
                assertEquals("FlameChart Test Callstack", descriptor.getName());
                assertEquals(IDataProviderDescriptor.ProviderType.TIME_GRAPH, descriptor.getType());
                assertEquals("Show FlameChart provided by Analysis module: Test Callstack", descriptor.getDescription());
                break;
            case "org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.flamechart:callstack.analysis":
                assertEquals("FlameChart Test XML callstack", descriptor.getName());
                assertEquals(IDataProviderDescriptor.ProviderType.TIME_GRAPH, descriptor.getType());
                assertEquals("Show FlameChart provided by Analysis module: Test XML callstack", descriptor.getDescription());
                break;
            default:
                fail("Unknown Entry" + descriptor.getId());
                break;
            }
        }
    }


    /**
     * Test getting the tree from the flame chart data provider
     */
    @Test
    public void testFetchTree() {
        FlameChartDataProvider dataProvider = getDataProvider();

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> responseTree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());
        assertTrue(responseTree.getStatus().equals(ITmfResponse.Status.COMPLETED));

        // Test the size of the tree
        TmfTreeModel<@NonNull FlameChartEntryModel> model = responseTree.getModel();
        assertNotNull(model);
        List<@NonNull FlameChartEntryModel> modelEntries = model.getEntries();
        assertEquals(18, modelEntries.size());

        String traceName = getTrace().getName();

        // Test the hierarchy of the tree
        for (FlameChartEntryModel entry : modelEntries) {
            FlameChartEntryModel parent = findEntryById(modelEntries, entry.getParentId());
            switch (entry.getEntryType()) {
            case FUNCTION:
                assertNotNull(parent);
                assertEquals(EntryType.LEVEL, parent.getEntryType());
                break;
            case LEVEL: {
                assertNotNull(parent);
                // Verify the hierarchy of the elements
                switch (entry.getName()) {
                case "1":
                    assertEquals(traceName, parent.getName());
                    break;
                case "2":
                    assertEquals("1", parent.getName());
                    break;
                case "3":
                    assertEquals("1", parent.getName());
                    break;
                case "5":
                    assertEquals(traceName, parent.getName());
                    break;
                case "6":
                    assertEquals("5", parent.getName());
                    break;
                case "7":
                    assertEquals("5", parent.getName());
                    break;
                default:
                    fail("Unknown entry " + entry.getName());
                    break;
                }
            }
                break;
            case KERNEL:
                fail("There should be no kernel entry in this callstack");
                break;
            case TRACE:
                assertEquals(-1, entry.getParentId());
                break;
            default:
                fail("Unknown entry " + entry);
                break;
            }
        }
    }

    private static @Nullable FlameChartEntryModel findEntryById(Collection<FlameChartEntryModel> list, long id) {
        return list.stream()
                .filter(entry -> entry.getId() == id)
                .findFirst().orElse(null);
    }

    private static @Nullable FlameChartEntryModel findEntryByNameAndType(Collection<FlameChartEntryModel> list, String name, EntryType type) {
        return list.stream()
                .filter(entry -> entry.getEntryType().equals(type) && entry.getName().equals(name))
                .findFirst().orElse(null);
    }

    private static @Nullable FlameChartEntryModel findEntryByDepthAndType(Collection<FlameChartEntryModel> list, int depth, EntryType type) {
        return list.stream()
                .filter(entry -> entry.getEntryType().equals(type) && entry.getDepth() == depth)
                .findFirst().orElse(null);
    }

    private static List<FlameChartEntryModel> findEntriesByParent(Collection<FlameChartEntryModel> list, long parentId) {
        return list.stream()
                .filter(entry -> entry.getParentId() == parentId)
                .collect(Collectors.toList());
    }

    /**
     * Test getting the model from the flame chart data provider
     */
    @Test
    public void testFetchModel() {
        FlameChartDataProvider dataProvider = getDataProvider();

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> responseTree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());
        assertTrue(responseTree.getStatus().equals(ITmfResponse.Status.COMPLETED));
        TmfTreeModel<@NonNull FlameChartEntryModel> model = responseTree.getModel();
        assertNotNull(model);
        List<@NonNull FlameChartEntryModel> modelEntries = model.getEntries();
        // Find the entries corresponding to threads 3 and 6 (along with pid 5)
        Set<@NonNull Long> selectedIds = new HashSet<>();
        // Thread 3
        FlameChartEntryModel tid3 = findEntryByNameAndType(modelEntries, "3", EntryType.LEVEL);
        assertNotNull(tid3);
        selectedIds.add(tid3.getId());
        List<FlameChartEntryModel> tid3Children = findEntriesByParent(modelEntries, tid3.getId());
        assertEquals(2, tid3Children.size());
        tid3Children.forEach(child -> selectedIds.add(child.getId()));
        // Pid 5
        FlameChartEntryModel pid5 = findEntryByNameAndType(modelEntries, "5", EntryType.LEVEL);
        assertNotNull(pid5);
        selectedIds.add(pid5.getId());
        // Thread 6
        FlameChartEntryModel tid6 = findEntryByNameAndType(modelEntries, "6", EntryType.LEVEL);
        assertNotNull(tid6);
        selectedIds.add(tid6.getId());
        List<FlameChartEntryModel> tid6Children = findEntriesByParent(modelEntries, tid6.getId());
        assertEquals(3, tid6Children.size());
        tid6Children.forEach(child -> selectedIds.add(child.getId()));

        // Get the row model for those entries with high resolution
        TmfModelResponse<@NonNull TimeGraphModel> rowResponse = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(3, 15, 50, selectedIds)), new NullProgressMonitor());
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());

        TimeGraphModel rowModel = rowResponse.getModel();
        assertNotNull(rowModel);
        List<@NonNull ITimeGraphRowModel> rows = rowModel.getRows();
        assertEquals(8, rows.size());

        // Verify the level entries
        verifyStates(rows, tid3, Collections.emptyList());
        verifyStates(rows, pid5, Collections.emptyList());
        verifyStates(rows, tid6, Collections.emptyList());
        // Verify function level 1 of tid 3
        verifyStates(rows, findEntryByDepthAndType(tid3Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(3, 17, Integer.MIN_VALUE, "op2")));
        // Verify function level 2 of tid 3
        verifyStates(rows, findEntryByDepthAndType(tid3Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 4, Integer.MIN_VALUE),
                new TimeGraphState(5, 1, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(6, 1, Integer.MIN_VALUE),
                new TimeGraphState(7, 6, Integer.MIN_VALUE, "op2"),
                new TimeGraphState(13, 8, Integer.MIN_VALUE)));
        // Verify function level 1 of tid 6
        verifyStates(rows, findEntryByDepthAndType(tid6Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(1, 19, Integer.MIN_VALUE, "op1")));
        // Verify function level 2 of tid 6
        verifyStates(rows, findEntryByDepthAndType(tid6Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(2, 5, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(7, 1, Integer.MIN_VALUE),
                new TimeGraphState(8, 3, Integer.MIN_VALUE, "op2"),
                new TimeGraphState(11, 1, Integer.MIN_VALUE),
                new TimeGraphState(12, 8, Integer.MIN_VALUE, "op4")));
        // Verify function level 3 of tid 6
        verifyStates(rows, findEntryByDepthAndType(tid6Children, 3, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 3, Integer.MIN_VALUE),
                new TimeGraphState(4, 2, Integer.MIN_VALUE, "op1"),
                new TimeGraphState(6, 3, Integer.MIN_VALUE),
                new TimeGraphState(9, 1, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(10, 11, Integer.MIN_VALUE)));

        // Get the row model for those entries with low resolution
        rowResponse = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(3, 15, 2, selectedIds)), new NullProgressMonitor());
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());

        rowModel = rowResponse.getModel();
        assertNotNull(rowModel);
        rows = rowModel.getRows();
        assertEquals(8, rows.size());

        // Verify the level entries
        verifyStates(rows, tid3, Collections.emptyList());
        verifyStates(rows, pid5, Collections.emptyList());
        verifyStates(rows, tid6, Collections.emptyList());
        // Verify function level 1 of tid 3
        verifyStates(rows, findEntryByDepthAndType(tid3Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(3, 17, Integer.MIN_VALUE, "op2")));
        // Verify function level 2 of tid 3
        verifyStates(rows, findEntryByDepthAndType(tid3Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 4, Integer.MIN_VALUE),
                new TimeGraphState(13, 8, Integer.MIN_VALUE)));
        // Verify function level 1 of tid 6
        verifyStates(rows, findEntryByDepthAndType(tid6Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(1, 19, Integer.MIN_VALUE, "op1")));
        // Verify function level 2 of tid 6
        verifyStates(rows, findEntryByDepthAndType(tid6Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(2, 5, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(12, 8, Integer.MIN_VALUE, "op4")));
        // Verify function level 3 of tid 6
        verifyStates(rows, findEntryByDepthAndType(tid6Children, 3, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 3, Integer.MIN_VALUE),
                new TimeGraphState(10, 11, Integer.MIN_VALUE)));
    }

    /**
     * Test following a callstack backward and forward
     */
    @Test
    public void testFollowEvents() {
        FlameChartDataProvider dataProvider = getDataProvider();

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> responseTree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());
        assertTrue(responseTree.getStatus().equals(ITmfResponse.Status.COMPLETED));
        TmfTreeModel<@NonNull FlameChartEntryModel> model = responseTree.getModel();
        assertNotNull(model);
        List<@NonNull FlameChartEntryModel> modelEntries = model.getEntries();

        // Thread 2
        FlameChartEntryModel tid2 = findEntryByNameAndType(modelEntries, "2", EntryType.LEVEL);
        assertNotNull(tid2);
        List<FlameChartEntryModel> tid2Children = findEntriesByParent(modelEntries, tid2.getId());
        assertEquals(3, tid2Children.size());

        // For each child, make sure the response is always the same
        for (FlameChartEntryModel tid2Child : tid2Children) {
            TmfModelResponse<@NonNull TimeGraphModel> rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(6, Long.MAX_VALUE, 2, Collections.singleton(tid2Child.getId()))), MONITOR);
            verifyFollowResponse(rowModel, 1, 7);
        }

        // Go forward from time 7 till the end for one of the child element
        Set<@NonNull Long> selectedEntry = Objects.requireNonNull(Collections.singleton(tid2Children.get(1).getId()));
        TmfModelResponse<@NonNull TimeGraphModel> rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(7, Long.MAX_VALUE, 2, selectedEntry)), MONITOR);
        verifyFollowResponse(rowModel, 0, 10);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(10, Long.MAX_VALUE, 2, selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 1, 12);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(12, Long.MAX_VALUE, 2, selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 0, 20);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(20, Long.MAX_VALUE, 2, selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, -1, -1);

        // Go backward from the back
        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, 20L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 1, 12);

        // Go backward from time 7 till the beginning
        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, 7L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 2, 5);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, 5L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 3, 4);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, 4L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 2, 3);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, 3L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 1, 1);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, 1L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, -1, -1);
    }

    private static void verifyFollowResponse(TmfModelResponse<@NonNull TimeGraphModel> rowModel, int expectedDepth, int expectedTime) {
        assertEquals(ITmfResponse.Status.COMPLETED, rowModel.getStatus());

        TimeGraphModel model = rowModel.getModel();
        if (expectedDepth < 0) {
            assertNull(model);
            return;
        }
        assertNotNull(model);
        List<@NonNull ITimeGraphRowModel> rows = model.getRows();
        assertEquals(1, rows.size());
        List<ITimeGraphState> row = rows.get(0).getStates();
        assertEquals(1, row.size());
        ITimeGraphState stackInterval = row.get(0);
        long depth = stackInterval.getValue();
        assertEquals(expectedDepth, depth);
        assertEquals(expectedTime, stackInterval.getStartTime());
    }

    private static void verifyStates(List<ITimeGraphRowModel> rowModels, FlameChartEntryModel entry, List<TimeGraphState> expectedStates) {
        assertNotNull(entry);
        ITimeGraphRowModel rowModel = rowModels.stream()
                .filter(model -> model.getEntryID() == entry.getId())
                .findFirst().orElse(null);
        assertNotNull(rowModel);
        List<ITimeGraphState> states = rowModel.getStates();
        for (int i = 0; i < states.size(); i++) {
            String entryName = entry.getName();
            if (i > expectedStates.size() - 1) {
                fail("Unexpected state at position " + i + " for entry " + entryName + ": " + states.get(i));
            }
            ITimeGraphState actual = states.get(i);
            ITimeGraphState expected = expectedStates.get(i);
            assertEquals("State start time at " + i + " for entry " + entryName, expected.getStartTime(), actual.getStartTime());
            assertEquals("Duration at " + i + " for entry " + entryName, expected.getDuration(), actual.getDuration());
            assertEquals("Label at " + i + " for entry " + entryName, expected.getLabel(), actual.getLabel());

        }
    }

}
