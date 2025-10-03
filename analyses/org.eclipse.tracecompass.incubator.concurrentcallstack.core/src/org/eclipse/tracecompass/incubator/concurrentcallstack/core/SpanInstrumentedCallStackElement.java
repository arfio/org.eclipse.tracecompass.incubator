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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStack;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.InstrumentedGroupDescriptor;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public class SpanInstrumentedCallStackElement extends InstrumentedCallStackElement {
    private @Nullable SpanCallStack fCallstack = null;
    private final IHostIdResolver fHostResolver;

    public SpanInstrumentedCallStackElement(IHostIdResolver hostResolver, ITmfStateSystem stateSystem, Integer quark, IWeightedTreeGroupDescriptor group, IWeightedTreeGroupDescriptor nextGroup,
            InstrumentedCallStackElement parent) {
        super(hostResolver, stateSystem, quark, group, nextGroup, null, parent);
        fHostResolver = hostResolver;
    }

    @Override
    public CallStack getCallStack() {
        SpanCallStack callstack = fCallstack;
        List<Integer> subAttributes = getStackQuarks();
        if (callstack == null) {
            IHostIdProvider hostProvider = fHostResolver.apply(this);
            callstack = new SpanCallStack(getStateSystem(), subAttributes, this, hostProvider);
            fCallstack = callstack;
        } else {
            synchronized (callstack) {
                // Update the callstack if attributes were added
                if (callstack.getMaxDepth() < subAttributes.size()) {
                    callstack.updateAttributes(subAttributes);
                }
            }
        }
        return Objects.requireNonNull(callstack);
    }

    public static Collection<ICallStackElement> getRootElements(InstrumentedGroupDescriptor rootGroup, IHostIdResolver hostResolver, @Nullable IThreadIdResolver resolver, Map<Integer, ICallStackElement> cache) {
        return getNextElements(rootGroup, rootGroup.getStateSystem(), ITmfStateSystem.ROOT_ATTRIBUTE, hostResolver, null, cache);
    }

    private static Collection<ICallStackElement> getNextElements(InstrumentedGroupDescriptor nextGroup, ITmfStateSystem stateSystem, int baseQuark, IHostIdResolver hostResolver, @Nullable InstrumentedCallStackElement parent,
            Map<Integer, ICallStackElement> cache) {
        // Get the elements from the base quark at the given pattern
        List<Integer> quarks = stateSystem.getQuarks(baseQuark, nextGroup.getSubPattern());
        if (quarks.isEmpty()) {
            return Collections.emptyList();
        }

        InstrumentedGroupDescriptor nextLevel = nextGroup.getNextGroup();
        // If the next level is null, then this is a callstack final element
        List<ICallStackElement> elements = new ArrayList<>(quarks.size());
        for (Integer quark : quarks) {
            ICallStackElement element = cache.get(quark);
            if (element == null) {
                element = new SpanInstrumentedCallStackElement(hostResolver, stateSystem, quark,
                        nextGroup, nextLevel, parent);
                if (nextGroup.isSymbolKeyGroup()) {
                    element.setSymbolKeyElement(element);
                }
                if (parent != null) {
                    parent.addChild(element);
                }
                cache.put(quark, element);
            }
            elements.add(element);
        }
        return elements;
    }

}
