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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.InstrumentedGroupDescriptor;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 * CallStackSeries for concurrent traces
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public class SpanCallStackSeries extends CallStackSeries {
    private final InstrumentedGroupDescriptor fRootGroup;
    private final @Nullable IThreadIdResolver fResolver;
    private final IHostIdResolver fHostResolver;
    private final Map<Integer, ICallStackElement> fRootElements = new HashMap<>();

    public SpanCallStackSeries(ITmfStateSystem ss, List<String[]> patternPaths, int symbolKeyLevelIndex, String name, IHostIdResolver hostResolver, @Nullable IThreadIdResolver threadResolver) {
        // Build the groups from the state system and pattern paths
        super(ss, patternPaths, symbolKeyLevelIndex, name, hostResolver, threadResolver);
        int startIndex = patternPaths.size() - 1;
        InstrumentedGroupDescriptor prevLevel = new InstrumentedGroupDescriptor(ss, patternPaths.get(startIndex), null, symbolKeyLevelIndex == startIndex ? true : false);
        for (int i = startIndex - 1; i >= 0; i--) {
            InstrumentedGroupDescriptor level = new InstrumentedGroupDescriptor(ss, patternPaths.get(i), prevLevel, symbolKeyLevelIndex == i ? true : false);
            prevLevel = level;
        }
        fRootGroup = prevLevel;
        fResolver = threadResolver;
        fHostResolver = hostResolver;
    }

    @Override
    public Collection<ICallStackElement> getRootElements() {
        return SpanInstrumentedCallStackElement.getRootElements(fRootGroup, fHostResolver, fResolver, fRootElements);

    }
}
