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

package org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core;

import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * The signal that is triggered after extracting the requests from input trace (traces), indicating if there
 * exists just two requests or more. And provide the differential flame graph view with the callgraph provider
 * that is needed to build differential call graph provider
 *
 * @author Fateme Faraji Daneshgar
 */
public class TmfMultiComparisonSignal extends TmfSignal{



    private final boolean fMultiComparison;
    private SpanAggregatedCalledFunction fProcessA;
    private String fProcessNameA;
    private SpanAggregatedCalledFunction fProcessB;
    private String fProcessNameB;



    /**
     * Constructor
     *
     * @param source
     *            Object sending this signal
     * @param multiCompare
     *            indicates that the trace includes two requests or more
     * @param ProcessA
     *            the first request for comparison
     * @param ProcessNameA
     *             the name of the Request in ProcessA
     * @param ProcessB
     *            the second request for comparison
     * @param ProcessNameB
     *             the name of the Request in ProcessB
     */
    public TmfMultiComparisonSignal(Object source, boolean multiCompare, SpanAggregatedCalledFunction processA, String processNameA, SpanAggregatedCalledFunction processB, String processNameB) {
        super(source);
        fMultiComparison = multiCompare;
        fProcessA = processA;
        fProcessNameA = processNameA;
        fProcessB = processB;
        fProcessNameB = processNameB;

    }

    /**
     * @return The trace referred to by this signal
     */
    public boolean getMultiCompare() {
        return fMultiComparison;
    }

    @Override
    public String toString() {
        return "MultiComparison is "+fMultiComparison; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public SpanAggregatedCalledFunction getProcessA() {
        return fProcessA;
    }

    public SpanAggregatedCalledFunction getProcessB() {
        return fProcessB;
    }


    public String getProcessNameA() {
        return fProcessNameA;
    }

    public String getProcessNameB() {
        return fProcessNameB;
    }


}
