/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.concurrentstatesystem.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.incubator.internal.concurrentstatesystem.core.ConcurrentTransientState;
import org.eclipse.tracecompass.incubator.internal.concurrentstatesystem.core.ProcessQuark;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.IntegerRangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.TimeRangeCondition;
import org.eclipse.tracecompass.internal.statesystem.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLogBuilder;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * The State System for traces including concurrency
 *
 * @author fateme faraji daneshgar
 *
 */
public class ConcurrentStateSystem implements ITmfStateSystemBuilder {

    private static final String PARENT = ".."; //$NON-NLS-1$
    private static final String WILDCARD = "*"; //$NON-NLS-1$

    private final Map<String, ConcurrentTransientState> transStateMap = new LinkedHashMap<String, ConcurrentTransientState>();
    private final IStateHistoryBackend backend;
    private static final int MAX_STACK_DEPTH = 100000;
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(ConcurrentStateSystem.class);
    private boolean isDisposed = false;
    private final ConcurrentAttributeTree attributeTree;

    private boolean buildCancelled = false;

    /* Latch tracking if the state history is done building or not */
    private final CountDownLatch finishedLatch = new CountDownLatch(1);

    /**
     * New-file constructor. For when you build a state system with a new file,
     * or if the back-end does not require a file on disk.
     *
     * @param backend
     *            Back-end plugin to use
     */

    public ConcurrentStateSystem(@NonNull IStateHistoryBackend backEnd) {
        this.backend = backEnd;
        this.attributeTree = new ConcurrentAttributeTree(this);
    }

    /**
     * General constructor
     *
     * @param backend
     *            The "state history storage" back-end to use.
     * @param newFile
     *            Put true if this is a new history started from scratch. It is
     *            used to tell the state system where to get its attribute tree.
     * @throws IOException
     *             If there was a problem creating the new history file
     */
    public ConcurrentStateSystem(@NonNull IStateHistoryBackend backEnd, boolean newFile)
            throws IOException {
        this.backend = backEnd;

        if (newFile) {
            attributeTree = new ConcurrentAttributeTree(this);
        } else {
            /* We're opening an existing file */
            this.attributeTree = new ConcurrentAttributeTree(this, backEnd.supplyAttributeTreeReader());
            // transState.setInactive();
            finishedLatch.countDown(); /* The history is already built */
        }
    }

    @Override
    public synchronized void dispose() {

        isDisposed = true;
        Iterator<Map.Entry<String, ConcurrentTransientState>> itr = transStateMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, ConcurrentTransientState> entry = itr.next();
            entry.getValue().setInactive();
            buildCancelled = true;

        }
        backend.dispose();

    }

    // --------------------------------------------------------------------------
    // General methods related to the attribute tree
    // --------------------------------------------------------------------------

    /**
     * Get the attribute tree associated with this state system. This should be
     * the only way of accessing it (and if subclasses want to point to a
     * different attribute tree than their own, they should only need to
     * override this).
     *
     * @return The attribute tree
     */
    public ConcurrentAttributeTree getAttributeTree() {

        return attributeTree;
    }

    /**
     * Method used by the attribute tree when creating new attributes, to keep
     * the attribute count in the transient state in sync.
     */
    public int addEmptyAttribute(String processName, int ArributeQuark) {
        ConcurrentTransientState ts = getTransientState(processName);
        ts.addEmptyEntry(ArributeQuark);
        return ts.getOngoingStateValues().size();
    }

    @Override
    public int getNbAttributes() {
        return getAttributeTree().getNbAttributes();
    }

    @Override
    public String getAttributeName(int attributeQuark) {
        return getAttributeTree().getAttributeName(attributeQuark);
    }

    @Override
    public String getFullAttributePath(int attributeQuark) {
        return getAttributeTree().getFullAttributeName(attributeQuark);
    }

    @Override
    public String[] getFullAttributePathArray(int attributeQuark) {
        return getAttributeTree().getFullAttributePathArray(attributeQuark);
    }

    // --------------------------------------------------------------------------
    // Methods related to the storage backend
    // --------------------------------------------------------------------------

    @Override
    public long getStartTime() {
        return backend.getStartTime();
    }

    @Override
    public long getCurrentEndTime() {
        return backend.getEndTime();
    }

    @Override
    public void closeHistory(long endTime) throws TimeRangeException {
        File attributeTreeFile;
        long attributeTreeFilePos;
        long realEndTime = endTime;

        if (realEndTime < backend.getEndTime()) {
            /*
             * This can happen (empty nodes pushing the border further, etc.)
             * but shouldn't be too big of a deal.
             */
            realEndTime = backend.getEndTime();
        }
        Iterator<Map.Entry<String, ConcurrentTransientState>> itr = transStateMap.entrySet().iterator();
        int num = 0;
        while (itr.hasNext())

        {
            num++;
            Map.Entry<String, ConcurrentTransientState> entry = itr.next();
            if (num == 1) {
                entry.getValue().closeTransientState(realEndTime);
            } else {
                entry.getValue().closeTransientState2(realEndTime);

            }
        }

        backend.finishedBuilding(realEndTime);

        attributeTreeFile = backend.supplyAttributeTreeWriterFile();
        attributeTreeFilePos = backend.supplyAttributeTreeWriterFilePosition();
        if (attributeTreeFile != null) {
            /*
             * If null was returned, we simply won't save the attribute tree,
             * too bad!
             */
            getAttributeTree().writeSelf(attributeTreeFile, attributeTreeFilePos);
        }
        finishedLatch.countDown(); /* Mark the history as finished building */
    }

    // --------------------------------------------------------------------------
    // Quark-retrieving methods
    // --------------------------------------------------------------------------

    @Override
    public int getQuarkAbsolute(String... attribute)
            throws AttributeNotFoundException {
        int quark = getAttributeTree().getQuarkDontAdd(ROOT_ATTRIBUTE, attribute);
        if (quark == INVALID_ATTRIBUTE) {
            throw new AttributeNotFoundException(getSSID() + " Path:" + Arrays.toString(attribute)); //$NON-NLS-1$
        }
        return quark;
    }

    @Override
    public int optQuarkAbsolute(String... attribute) {
        return getAttributeTree().getQuarkDontAdd(ROOT_ATTRIBUTE, attribute);
    }

    public ProcessQuark getQuarkAbsoluteAndAddConcurrent(String... attribute) {
        String processName = attribute[1];
        return getAttributeTree().getQuarkAndAdd(processName, ROOT_ATTRIBUTE, attribute);
    }

    @Override
    public int getQuarkRelative(int startingNodeQuark, String... subPath)
            throws AttributeNotFoundException {
        int quark = getAttributeTree().getQuarkDontAdd(startingNodeQuark, subPath);
        if (quark == INVALID_ATTRIBUTE) {
            throw new AttributeNotFoundException(getSSID() + " Quark:" + startingNodeQuark + ", SubPath:" + Arrays.toString(subPath)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return quark;
    }

    @Override
    public int optQuarkRelative(int startingNodeQuark, String... subPath) {
        return getAttributeTree().getQuarkDontAdd(startingNodeQuark, subPath);
    }

    public ProcessQuark getQuarkRelativeAndAdd(String processName, int startingNodeQuark, String... subPath) {
        // String processName = subPath[0];
        return getAttributeTree().getQuarkAndAdd(processName, startingNodeQuark, subPath);
    }

    @Override
    public List<@NonNull Integer> getSubAttributes(int quark, boolean recursive) {
        return getAttributeTree().getSubAttributes(quark, recursive);
    }

    @Override
    public int getParentAttributeQuark(int quark) {
        return getAttributeTree().getParentAttributeQuark(quark);
    }

    // --------------------------------------------------------------------------
    // Methods related to insertions in the history
    // --------------------------------------------------------------------------

    public void modifyAttribute(long t, Object value, int attributeQuark, String processName)
            throws TimeRangeException, StateValueTypeException {
        ConcurrentTransientState ts = getTransientState(processName);
        ts.processStateChange(t, value, attributeQuark);

    }

    public void modifyStackDepth(int attributeQuark, String processName, int stackDepth)
            throws TimeRangeException, StateValueTypeException {
        ConcurrentTransientState ts = getTransientState(processName);
        ts.modifyStackDepth(attributeQuark, stackDepth);

    }

    public void pushAttribute(long t, Object value, int attributeQuark, String processName, String functionEntryParent)
            throws TimeRangeException, StateValueTypeException {
        int stackDepth;
        ProcessQuark subAttributeQuark;
        // Object previousSV = null;
        ConcurrentTransientState ts = getTransientState(processName);

        stackDepth = ts.getCallStack();

        if (stackDepth >= MAX_STACK_DEPTH) {
            /*
             * Limit stackDepth to 100000, to avoid having Attribute Trees grow
             * out of control due to buggy insertions
             */
            String message = " Stack limit reached, not pushing"; //$NON-NLS-1$
            throw new IllegalStateException(getSSID() + " Quark:" + attributeQuark + message); //$NON-NLS-1$
        }

        if (stackDepth > 0) {
            stackDepth = ts.TuneStackDepth(functionEntryParent);
        }
        stackDepth++;
        subAttributeQuark = getQuarkRelativeAndAdd(processName, attributeQuark, String.valueOf(stackDepth));
        TuneStateSize(processName, subAttributeQuark);

        modifyAttribute(t, stackDepth, 2, processName);
        modifyAttribute(t, value, subAttributeQuark.getTsQuarck(), processName);
        modifyStackDepth(subAttributeQuark.getTsQuarck(), processName, stackDepth);

    }

    // --------------------------------------------------------------------------
    // "Current" query/update methods
    // --------------------------------------------------------------------------

    public ITmfStateValue queryOngoingState(int attributeQuark, String processName) {
        return TmfStateValue.newValue(queryOngoing(attributeQuark, processName));
    }

    public Object queryOngoing(int attributeQuark, String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        return ts.getOngoingStateValue(attributeQuark);
    }

    public List<@Nullable Object> queryOngoing(String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        return ts.getOngoingStateValues();
    }

    public long getOngoingStartTime(int attribute, String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        return ts.getOngoingStartTime(attribute);
    }

    public void updateOngoingState(ITmfStateValue newValue, int attributeQuark, String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        ts.changeOngoingStateValue(attributeQuark, newValue.unboxValue());

    }

    public void updateOngoingState(Object newValue, int attributeQuark, String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        ts.changeOngoingStateValue(attributeQuark, newValue);
    }

    /**
     * Modify the whole "ongoing state" (state values + start times). This can
     * be used when "seeking" a state system to a different point in the trace
     * (and restoring the known stateInfo at this location). Use with care!
     *
     * @param newStateIntervals
     *            The new List of state values to use as ongoing state info
     */
    protected void replaceOngoingState(@NonNull List<@NonNull ITmfStateInterval> newStateIntervals, String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        ts.replaceOngoingState(newStateIntervals);
    }

    // --------------------------------------------------------------------------
    // Regular query methods (sent to the back-end)
    // --------------------------------------------------------------------------

    @Override
    public List<ITmfStateInterval> queryFullState(long t)
            throws TimeRangeException, StateSystemDisposedException {
        if (isDisposed) {
            throw new StateSystemDisposedException();
        }
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINER, "StateSystem:FullQuery", //$NON-NLS-1$
                "ssid", getSSID(), "ts", t).build()) { //$NON-NLS-1$ //$NON-NLS-2$

            final int nbAttr = getNbAttributes();
            List<@Nullable ITmfStateInterval> stateInfo = new ArrayList<>(nbAttr);

            /*
             * Bring the size of the array to the current number of attributes
             */
            for (int i = 0; i < nbAttr; i++) {
                stateInfo.add(null);
            }

            /*
             * If we are currently building the history, also query the
             * "ongoing" states for stuff that might not yet be written to the
             * history.
             */
            Iterator<Map.Entry<String, ConcurrentTransientState>> itr = transStateMap.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, ConcurrentTransientState> entry = itr.next();
                if (entry.getValue().isActive()) {
                    entry.getValue().doQuery(stateInfo, t);
                }
            }

            /* Query the storage backend */
            backend.doQuery(stateInfo, t);

            /*
             * We should have previously inserted an interval for every
             * attribute.
             */
            for (ITmfStateInterval interval : stateInfo) {
                if (interval == null) {
                    throw new IllegalStateException("Incoherent interval storage"); //$NON-NLS-1$
                }
            }
            return stateInfo;
        }
    }

    public ITmfStateInterval querySingleState(long t, int attributeQuark, String processName)
            throws TimeRangeException, StateSystemDisposedException {
        if (isDisposed) {
            throw new StateSystemDisposedException();
        }
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINER, "StateSystem:SingleQuery", //$NON-NLS-1$
                "ssid", this.getSSID(), //$NON-NLS-1$
                "ts", t, //$NON-NLS-1$
                "attribute", attributeQuark).build()) { //$NON-NLS-1$
            ITmfStateInterval ret = null;
            ConcurrentTransientState ts = getTransientState(processName);
            ret = ts.getIntervalAt(t, attributeQuark);

            if (ret == null) {
                /*
                 * The transient state did not have the information, let's look
                 * into the backend next.
                 */
                ret = backend.doSingularQuery(t, attributeQuark);
            }

            if (ret == null) {
                /*
                 * If we did our job correctly, there should be intervals for
                 * every possible attribute, over all the valid time range.
                 */
                throw new IllegalStateException("Incoherent interval storage"); //$NON-NLS-1$
            }
            return ret;
        }
    }

    public void removeAttribute(long t, int tsQuark, int attributeQuark, String processName)
            throws TimeRangeException {
        /*
         * Nullify our children first, recursively. We pass 'false' because we
         * handle the recursion ourselves.
         */
        List<Integer> childAttributes = getSubAttributes(attributeQuark, false, processName);
        for (int childNodeQuark : childAttributes) {
            if (attributeQuark == childNodeQuark) {
                /* Something went very wrong when building out attribute tree */
                throw new IllegalStateException();
            }
            removeAttribute(t, tsQuark, childNodeQuark, processName);
        }
        /* Nullify ourselves */
        ConcurrentTransientState ts = getTransientState(processName);
        try {
            ts.processStateChange(t, null, tsQuark);
        } catch (StateValueTypeException e) {
            /*
             * Will not happen since we're inserting null values only, but poor
             * compiler has no way of knowing this...
             */
            throw new IllegalStateException(e);
        }
        modifyStackDepth(tsQuark, processName, -1);
    }

    // --------------------------------------------------------------------------
    // Debug methods
    // --------------------------------------------------------------------------

    static void logMissingInterval(int attribute, long timestamp) {
        Activator.getDefault().logInfo("No data found in history for attribute " + //$NON-NLS-1$
                attribute + " at time " + timestamp + //$NON-NLS-1$
                ", returning dummy interval"); //$NON-NLS-1$
    }

    public ConcurrentTransientState getTransientState(String processName) {
        ConcurrentTransientState ts = null;
        for (Entry<String, ConcurrentTransientState> entry : transStateMap.entrySet()) {
            if (Objects.equal(processName, entry.getKey())) {
                ts = entry.getValue();
            }
        }
        if (ts == null) {
            ts = new ConcurrentTransientState(backend);
            transStateMap.put(processName, ts);
        }
        return ts;
    }

    public int getStateSize(String processName) {
        ConcurrentTransientState ts = getTransientState(processName);
        int size = ts.getOngoingStateValues().size() - 1;
        return size;
    }

    public void TuneStateSize(String processName, ProcessQuark pq) {
        ConcurrentTransientState ts = getTransientState(processName);
        if (ts.getOngoingStateValues().size()==pq.getTsQuarck()) {
            ts.addEmptyEntry(pq.getAttrQuarck());
        }
    }

    @Override
    public ITmfStateInterval querySingleState(long t, int attributeQuark)
            throws TimeRangeException, StateSystemDisposedException {
        if (isDisposed) {
            throw new StateSystemDisposedException();
        }
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINER, "StateSystem:SingleQuery", //$NON-NLS-1$
                "ssid", this.getSSID(), //$NON-NLS-1$
                "ts", t, //$NON-NLS-1$
                "attribute", attributeQuark).build()) { //$NON-NLS-1$
            ITmfStateInterval ret = backend.doSingularQuery(t, attributeQuark);

            if (ret == null) {
                /*
                 * If we did our job correctly, there should be intervals for
                 * every possible attribute, over all the valid time range.
                 */
                throw new IllegalStateException("Incoherent interval storage"); //$NON-NLS-1$
            }
            return ret;
        }
    }

    public int getAttributeQuarck(String processName, int tsQuark) {
        ConcurrentTransientState ts = getTransientState(processName);
        return ts.getAttributeQuarck(tsQuark);

    }

    public int getStackDepth(String processName, int tsQuark) {
        ConcurrentTransientState ts = getTransientState(processName);
        return ts.getStackDepth(tsQuark);

    }

    @Override
    public String getSSID() {
        return backend.getSSID();
    }

    @Override
    public boolean isCancelled() {
        return buildCancelled;
    }

    @Override
    public void waitUntilBuilt() {
        try {
            finishedLatch.await();
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    @Override
    public boolean waitUntilBuilt(long timeout) {
        boolean ret = false;
        try {
            ret = finishedLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Do nothing
        }
        return ret;
    }

    @Override
    public List<@NonNull Integer> getSubAttributes(int quark, boolean recursive, String pattern) {
        List<Integer> all = getSubAttributes(quark, recursive);
        List<@NonNull Integer> ret = new LinkedList<>();
        Pattern regex = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
        for (Integer attQuark : all) {
            String name = getAttributeName(attQuark.intValue());
            if (regex.matcher(name).matches()) {
                ret.add(attQuark);
            }
        }
        return ret;
    }

    @Override
    public List<@NonNull Integer> getQuarks(String... pattern) {
        return getQuarks(ROOT_ATTRIBUTE, pattern);
    }

    @Override
    public List<@NonNull Integer> getQuarks(int startingNodeQuark, String... pattern) {
        Builder<@NonNull Integer> builder = ImmutableSet.builder();
        if (pattern.length > 0) {
            getQuarks(builder, startingNodeQuark, Arrays.asList(pattern));
        } else {
            builder.add(startingNodeQuark);
        }
        return builder.build().asList();
    }

    private void getQuarks(Builder<@NonNull Integer> builder, int quark, List<String> pattern) {
        String element = pattern.get(0);
        if (element == null) {
            return;
        }
        List<String> remainder = pattern.subList(1, pattern.size());
        if (remainder.isEmpty()) {
            if (element.equals(WILDCARD)) {
                builder.addAll(getSubAttributes(quark, false));
            } else if (element.equals(PARENT)) {
                builder.add(getParentAttributeQuark(quark));
            } else {
                int subQuark = optQuarkRelative(quark, element);
                if (subQuark != INVALID_ATTRIBUTE) {
                    builder.add(subQuark);
                }
            }
        } else {
            if (element.equals(WILDCARD)) {
                getSubAttributes(quark, false).forEach(subquark -> getQuarks(builder, subquark, remainder));
            } else if (element.equals(PARENT)) {
                getQuarks(builder, getParentAttributeQuark(quark), remainder);
            } else {
                int subQuark = optQuarkRelative(quark, element);
                if (subQuark != INVALID_ATTRIBUTE) {
                    getQuarks(builder, subQuark, remainder);
                }
            }
        }
    }

    @Override
    public ITmfStateValue queryOngoingState(int attributeQuark) {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: queryOngoingState");
        return null;
    }

    @Override
    public long getOngoingStartTime(int attributeQuark) {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: getOngoingStartTime");

        return 0;
    }

    public Iterable<@NonNull ITmfStateInterval> query2D(String processName, Collection<@NonNull Integer> quarks, Collection<@NonNull Long> times)
            throws StateSystemDisposedException, TimeRangeException, IndexOutOfBoundsException {
        if (isDisposed) {
            throw new StateSystemDisposedException();
        }
        if (times.isEmpty()) {
            return Collections.emptyList();
        }

        TimeRangeCondition timeCondition = TimeRangeCondition.forDiscreteRange(times);
        return query2D(quarks, timeCondition, false, processName);
    }

    private Iterable<@NonNull ITmfStateInterval> query2D(@NonNull Collection<@NonNull Integer> quarks, TimeRangeCondition timeCondition, boolean reverse, String processName)
            throws TimeRangeException, IndexOutOfBoundsException {
        if (timeCondition.min() < getStartTime()) {
            throw new TimeRangeException("Time conditions " + timeCondition.min() + " is lower than state system start time: " + getStartTime()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (quarks.isEmpty()) {
            return Collections.emptyList();
        }

        IntegerRangeCondition quarkCondition = IntegerRangeCondition.forDiscreteRange(quarks);
        if (quarkCondition.min() < 0 || quarkCondition.max() >= getNbAttributes()) {
            throw new IndexOutOfBoundsException();

        }
        ConcurrentTransientState ts = getTransientState(processName);
        Iterable<@NonNull ITmfStateInterval> transStateIterable = ts.query2D(quarks, timeCondition);
        Iterable<@NonNull ITmfStateInterval> backendIterable = backend.query2D(quarkCondition, timeCondition, reverse);

        return Iterables.concat(transStateIterable, backendIterable);
    }

    public Iterable<@NonNull ITmfStateInterval> query2D(String processName, Collection<@NonNull Integer> quarks, long start, long end)
            throws StateSystemDisposedException, TimeRangeException, IndexOutOfBoundsException {
        if (isDisposed) {
            throw new StateSystemDisposedException();
        }

        boolean reverse = (start > end) ? true : false;
        TimeRangeCondition timeCondition = TimeRangeCondition.forContinuousRange(Math.min(start, end), Math.max(start, end));
        return query2D(quarks, timeCondition, reverse, processName);
    }

    @Override
    public int getQuarkAbsoluteAndAdd(String... attribute) {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: getQuarkAbsoluteAndAdd");

        return 0;
    }

    @Override
    public int getQuarkRelativeAndAdd(int startingNodeQuark, String... subPath) {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: getQuarkRelativeAndAdd");

        return 0;
    }

    @Override
    public void updateOngoingState(ITmfStateValue newValue, int attributeQuark) {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: updateOngoingState");

    }

    @Override
    public void modifyAttribute(long t, Object value, int attributeQuark) throws StateValueTypeException {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: modifyAttribute");

    }

    @Override
    public void pushAttribute(long t, Object value, int attributeQuark) throws StateValueTypeException {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: pushAttribute");

    }

    @Override
    public ITmfStateValue popAttribute(long t, int attributeQuark)
            throws TimeRangeException, StateValueTypeException {
        Object pop = popAttributeObject(t, attributeQuark);
        return pop != null ? TmfStateValue.newValue(pop) : null;
    }

    @Override
    public void removeAttribute(long t, int attributeQuark) {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: removeAttribute");

    }

    @Override
    public void removeFiles() {
        backend.removeFiles();
    }

    @Override
    public Iterable<ITmfStateInterval> query2D(Collection<Integer> quarks, Collection<Long> times) throws StateSystemDisposedException, IndexOutOfBoundsException, TimeRangeException {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: query2D");
        return null;
    }

    @Override
    public Iterable<ITmfStateInterval> query2D(Collection<Integer> quarks, long start, long end) throws StateSystemDisposedException, IndexOutOfBoundsException, TimeRangeException {
        // TODO Auto-generated method stub
        System.out.println("WWWAARRNNIINNGG: Unimplemented method: query2D");
        return null;
    }

    public Iterable<ITmfStateInterval> queryBackend2D(Collection<Integer> quarks, Collection<Long> times) throws StateSystemDisposedException, IndexOutOfBoundsException, TimeRangeException {
        // TODO Auto-generated method stub
        TimeRangeCondition timeCondition = TimeRangeCondition.forDiscreteRange(times);
        IntegerRangeCondition quarkCondition = IntegerRangeCondition.forDiscreteRange(quarks);

        return backend.query2D(quarkCondition, timeCondition);
    }

}
