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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStack;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSymbolFactory;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAbstractCalledFunction;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ProcessStatusInterval;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;

/**
 * Call Graph Analysis for concurrent traces
 * @author Fateme Faraji Daneshgar
 *
 */
public class SpanCallGraphAnalysis extends CallGraphAnalysis {
    private boolean fHasKernelStatuses = false;
    public static final int SELF_TIME_METRIC_INDEX = 0;
    public static final int CPU_TIME_METRIC_INDEX = 1;
    public static final String Span = "span:";
    public static final String parentSpan = "parentSpan:";
    public static final String func = "opName:";
    private final IFlameChartProvider fCsProvider;





    public SpanCallGraphAnalysis(IFlameChartProvider csProvider) {
        super(csProvider);
        fCsProvider = csProvider;
    }

    @Override
    protected void iterateOverCallstackSerie(CallStackSeries callstackSerie, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // The root elements are the same as the one from the callstack series
        Collection<ICallStackElement> rootElements = callstackSerie.getRootElements();
        for (ICallStackElement element : rootElements) {
            if (monitor.isCanceled()) {
                return;
            }
            IFlameChartProvider callstackModule = fCsProvider;
            IHostIdProvider hostIdProvider = Objects.requireNonNull(callstackModule.getHostIdResolver().apply(element));
            IHostModel model = ModelManager.getModelFor(hostIdProvider.apply(start));

            iterateOverElement(element, model, callgraph, start, end, monitor);
        }
    }

    private void iterateOverElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // Iterator over the children of the element until we reach the leaves
        if (element.isLeaf()) {
            iterateOverLeafElement(element, model, callgraph, start, end, monitor);
            return;
        }
        for (ICallStackElement child : element.getChildrenElements()) {
            iterateOverElement(child, model, callgraph, start, end, monitor);
        }
    }

    private void iterateOverLeafElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        if (!(element instanceof InstrumentedCallStackElement)) {
            throw new IllegalStateException("Call Graph Analysis: The element does not have the right type"); //$NON-NLS-1$
        }
        SpanInstrumentedCallStackElement insElement = (SpanInstrumentedCallStackElement) element;

        SpanCallStack callStack = (SpanCallStack) insElement.getCallStack();

        // If there is no children for this callstack, just return
        if (callStack.getMaxDepth() == 0) {
            return;
        }
        fHasKernelStatuses |= callStack.hasKernelStatuses();
        // Start with the first function
        SpanAbstractCalledFunction nextFunction = (SpanAbstractCalledFunction) callStack.getNextFunction(callStack.getStartTime(), 1, null, model, start, end);
        while (nextFunction != null) {
            SpanAggregatedCalledFunction aggregatedChild = createSpanCallSite(CallStackSymbolFactory.createSymbol(getFuncName(nextFunction.getSymbol()), element, nextFunction.getStart()));
            iterateOverCallstack(element, callStack, nextFunction, 2, aggregatedChild, model, start, end, monitor);
            aggregatedChild.addFunctionCall(nextFunction);
            Iterable<ProcessStatusInterval> kernelStatuses = callStack.getKernelStatuses(nextFunction, Collections.emptyList());
            for (ProcessStatusInterval status : kernelStatuses) {
                aggregatedChild.addKernelStatus(status);
            }
            callgraph.addAggregatedCallSite(element, aggregatedChild);
            nextFunction = (SpanAbstractCalledFunction) callStack.getNextFunction(nextFunction.getEnd(), 1, null, model, start, end);
        }
    }

    private void iterateOverCallstack(ICallStackElement element, CallStack callstack, ICalledFunction function, int nextLevel, SpanAggregatedCalledFunction aggregatedCall, IHostModel model, long start, long end, IProgressMonitor monitor) {
        if (nextLevel > callstack.getMaxDepth()) {
            return;
        }
        //SpanAbstractCalledFunction parent =  SpanCalledFunctionFactory.create(function.getStart(), function.getEnd(), function.getSymbol(),function.getProcessId(),function.getThreadId(), function.getParent(), model);
        //SpanAbstractCalledFunction parent2 =  SpanCalledFunctionFactory.create(function.getStart(), function.getEnd(), function.getSymbol(),function.getProcessId(),function.getThreadId(), function.getParent(), model);

        SpanAbstractCalledFunction nextFunction = (SpanAbstractCalledFunction) callstack.getNextFunction(function.getStart(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
        int level = nextLevel;
        String funcSpanId = getSpanId(function.getSymbol());
        while (nextFunction !=null) {
            String nextFuncParentId = getParentId(nextFunction.getSymbol());
            if (!funcSpanId.equals(nextFuncParentId)) {
                //parent = parent2;
                nextFunction = (SpanAbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd()+1, level, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
                continue;
                }

            if (nextFunction !=null) {
                ((SpanAbstractCalledFunction) function).addChild(nextFunction);
                SpanAggregatedCalledFunction aggregatedChild = createSpanCallSite(CallStackSymbolFactory.createSymbol(getFuncName(nextFunction.getSymbol()), element, nextFunction.getStart()));
                iterateOverCallstack(element, callstack, nextFunction, level+1, aggregatedChild, model, start, end, monitor);
                aggregatedCall.addChild( nextFunction, aggregatedChild);

                //parent2 = parent;
                nextFunction = (SpanAbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd()+1, level, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
                 //nextFunction = (AbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));

            }
    }

    }
    private static String getParentId(@NonNull Object object) {
        String symbol = String.valueOf(object);
        int indx = symbol.indexOf(parentSpan);
        int lastIndx = symbol.indexOf(",o");
        if (indx ==-1) {
            return null;
        }
        return symbol.substring(indx+parentSpan.length(),lastIndx);
    }

    private static String getSpanId(@NonNull Object object) {
        String symbol = String.valueOf(object);
        int indx = symbol.indexOf(Span);
        int lastIndx = symbol.indexOf(",");
        if (indx ==-1) {
            return null;
        }
        return symbol.substring(indx+Span.length(),lastIndx);

    }

    private static String getFuncName(@NonNull Object object) {
        String symbol = String.valueOf(object);
        int indx = symbol.indexOf(func);
        int lastIndx = symbol.indexOf("]");
        if (indx ==-1) {
            return null;
        }
        return symbol.substring(indx+func.length(),lastIndx);

    }

    @Override
    public List<String> getExtraDataSets() {
        if (fHasKernelStatuses) {
            return Collections.singletonList(String.valueOf(org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.Messages.FlameChartDataProvider_KernelStatusTitle));
        }
        //return ICallGraphProvider.super.getExtraDataSets();
        return Collections.emptyList();
    }


    public SpanAggregatedCalledFunction createSpanCallSite(Object symbol) {
        return new SpanAggregatedCalledFunction((ICallStackSymbol) symbol);
    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);

    }


}