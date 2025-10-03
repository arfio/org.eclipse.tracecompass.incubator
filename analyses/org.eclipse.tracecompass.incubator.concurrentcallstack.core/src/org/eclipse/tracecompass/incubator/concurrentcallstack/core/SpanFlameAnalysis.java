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
package org.eclipse.tracecompass.incubator.concurrentcallstack.core;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 *
 * @author Fateme Faraji Daneshgar
 *
 *.
 */
public class SpanFlameAnalysis extends SpanCallStackAnalysis {

    private static final @NonNull String ID = "org.eclipse.tracecompass.incubator.concurrentcallstack.callstack"; //$NON-NLS-1$

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        return new SpanFlameStateProvider(trace);
    }
    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public @NonNull IHostIdResolver getHostIdResolver() {
        // TODO Auto-generated method stub
        return null;
    }
}
