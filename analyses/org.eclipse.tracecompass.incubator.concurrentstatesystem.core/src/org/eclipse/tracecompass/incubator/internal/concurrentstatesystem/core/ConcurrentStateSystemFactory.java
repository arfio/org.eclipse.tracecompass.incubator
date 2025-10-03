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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.concurrentstatesystem.core.ConcurrentStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;

/**
 * Factory to create state systems for concurrent executions.
 *
 * Since state system are meant to be accessed using the {@link ITmfStateSystem}
 * and {@link ITmfStateSystemBuilder} interfaces, you can use this factory to
 * instantiate new ones.
 *
 * @author Fateme Faraji Daneshgar
 */
@NonNullByDefault
public final class ConcurrentStateSystemFactory {

    private ConcurrentStateSystemFactory() {
    }

    /**
     * New-file factory method. For when you build a state system with a new
     * file, or if the back-end does not require a file on disk.
     *
     * @param backend
     *            Back-end plugin to use
     * @return The new state system
     * @since 1.0
     */
    public static ITmfStateSystemBuilder newStateSystem(IStateHistoryBackend backend) {
        return new ConcurrentStateSystem(backend);
    }

    /**
     * General factory method. The backend may try to open or create a file on
     * disk (the file contents and location are defined by the backend).
     *
     * @param backend
     *            The "state history storage" back-end to use.
     * @param newFile
     *            Put true if this is a new history started from scratch (any
     *            existing file will be overwritten).
     * @return The new state system
     * @throws IOException
     *             If there was a problem creating the new history file
     * @since 1.0
     */
    public static ConcurrentStateSystem newStateSystem(IStateHistoryBackend backend,
            boolean newFile) throws IOException {
        return new ConcurrentStateSystem(backend, newFile);
    }

}
