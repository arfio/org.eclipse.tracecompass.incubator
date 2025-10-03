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

package org.eclipse.tracecompass.incubator.internal.concurrentstatesystem.core;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HTInterval;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HTNode;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HistoryTreeBackend;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.ParentNode;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 *
 * @author fateme faraji daneshgar
 *
 */
public class ConcurrentHistoryTreeBackend extends HistoryTreeBackend {

    /**
     * Constructor for new history files. Use this when creating a new history
     * from scratch.
     *
     * @param ssid
     *            The state system's ID
     * @param newStateFile
     *            The filename/location where to store the state history (Should
     *            end in .ht)
     * @param providerVersion
     *            Version of of the state provider. We will only try to reopen
     *            existing files if this version matches the one in the
     *            framework.
     * @param startTime
     *            The earliest time stamp that will be stored in the history
     * @param blockSize
     *            The size of the blocks in the history file. This should be a
     *            multiple of 4096.
     * @param maxChildren
     *            The maximum number of children each core node can have
     * @throws IOException
     *             Thrown if we can't create the file for some reason
     */
    public ConcurrentHistoryTreeBackend(@NonNull String ssid,
            File newStateFile,
            int providerVersion,
            long startTime,
            int blockSize,
            int maxChildren) throws IOException {
        super(ssid, newStateFile, providerVersion, startTime, blockSize, maxChildren);
    }

    /**
     * Constructor for new history files. Use this when creating a new history
     * from scratch. This version supplies sane defaults for the configuration
     * parameters.
     *
     * @param ssid
     *            The state system's id
     * @param newStateFile
     *            The filename/location where to store the state history (Should
     *            end in .ht)
     * @param providerVersion
     *            Version of of the state provider. We will only try to reopen
     *            existing files if this version matches the one in the
     *            framework.
     * @param startTime
     *            The earliest time stamp that will be stored in the history
     * @throws IOException
     *             Thrown if we can't create the file for some reason
     * @since 1.0
     */
    public ConcurrentHistoryTreeBackend(@NonNull String ssid, File newStateFile, int providerVersion, long startTime)
            throws IOException {
        super(ssid, newStateFile, providerVersion, startTime, 64 * 1024, 50);
    }

    /**
     * Existing history constructor. Use this to open an existing state-file.
     *
     * @param ssid
     *            The state system's id
     * @param existingStateFile
     *            Filename/location of the history we want to load
     * @param providerVersion
     *            Expected version of of the state provider plugin.
     * @throws IOException
     *             If we can't read the file, if it doesn't exist, is not
     *             recognized, or if the version of the file does not match the
     *             expected providerVersion.
     */
    public ConcurrentHistoryTreeBackend(@NonNull String ssid, @NonNull File existingStateFile, int providerVersion)
            throws IOException {
        super(ssid, existingStateFile, providerVersion);
    }

    @Override
    public ITmfStateInterval doSingularQuery(long t, int attributeQuark)
            throws TimeRangeException, StateSystemDisposedException {
        try {
            return getRelevantInterval(t, attributeQuark);
        } catch (ClosedChannelException e) {
            throw new StateSystemDisposedException(e);
        }
    }

    private void checkValidTime(long t) {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (t < startTime || t > endTime) {
            throw new TimeRangeException(String.format("%s Time:%d, Start:%d, End:%d", //$NON-NLS-1$
                    t, startTime, endTime));
        }
    }

    /**
     * Inner method to find the interval in the tree containing the requested
     * key/timestamp pair, wherever in which node it is.
     */
    private HTInterval getRelevantInterval(long t, int key)
            throws TimeRangeException, ClosedChannelException {
        checkValidTime(t);

        Deque<Integer> queue = new ArrayDeque<>();
        Deque<Integer> queue2 = new ArrayDeque<>();

        queue.add(getSHT().getRootNode().getSequenceNumber());
        queue2.add(getSHT().getRootNode().getSequenceNumber());

        HTInterval interval;
        HTInterval finalIinterval = null;
        while (!queue.isEmpty()) {
            int sequenceNumber = queue.pop();
            HTNode currentNode = getSHT().readNode(sequenceNumber);
            if (currentNode.getNodeType() == HTNode.NodeType.CORE) {
                /* Here we add the relevant children nodes for BFS */
                queue.addAll(((ParentNode) currentNode).selectNextChildren(t, key));
                queue2.addAll(((ParentNode) currentNode).selectNextChildren(t, key));
            }
        }
        while (!queue2.isEmpty()) {
            int sequenceNumber = queue2.pop();
            HTNode currentNode = getSHT().readNode(sequenceNumber);
            interval = currentNode.getRelevantInterval(key, t);
            if (interval != null) {
                if (finalIinterval == null || interval.getEndTime() < finalIinterval.getEndTime()) {
                    finalIinterval = interval;
                }
            }
        }

        return finalIinterval;

    }

}
