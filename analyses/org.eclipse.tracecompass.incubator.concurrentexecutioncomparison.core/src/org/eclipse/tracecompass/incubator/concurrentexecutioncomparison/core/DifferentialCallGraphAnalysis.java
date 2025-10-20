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
package org.eclipse.tracecompass.incubator.concurrentexecutioncomparison.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.WeightedTreeUtils;
import org.eclipse.tracecompass.incubator.internal.concurrentcallstack.core.SpanAggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;

/**
 * build a differential call graph using the differentialWeightedTreeSet from
 * two sets of call graphs.
 *
 *
 * @author Fateme Faraji Daneshgar
 */

public class DifferentialCallGraphAnalysis extends TmfAbstractAnalysisModule {

    public static final String ID = "org.eclipse.tracecompass.incubator.executioncomparision.diffcallgraph"; //$NON-NLS-1$
    private DifferentialCallGraphProvider<ICallStackSymbol> fDifferentialCallGraphProvider;

    /**
     * Constructor
     */

    public DifferentialCallGraphAnalysis(Collection<SpanAggregatedCalledFunction> originalCGs, Collection<SpanAggregatedCalledFunction> diffCGs, IWeightedTreeProvider<ICallStackSymbol, ?, WeightedTree<ICallStackSymbol>> provider) {
        super();
        Collection<WeightedTree<ICallStackSymbol>> originalTree = new ArrayList<WeightedTree<ICallStackSymbol>>();
        Collection<WeightedTree<ICallStackSymbol>> diffTree = new ArrayList<WeightedTree<ICallStackSymbol>>();

        if (!originalCGs.isEmpty()) {
            SpanAggregatedCalledFunction mergedGroupOriginal = mergeCGlist(originalCGs);
            originalTree.add(mergedGroupOriginal);
        }

        if (!diffCGs.isEmpty()) {
            SpanAggregatedCalledFunction mergedGroupDiff = mergeCGlist(diffCGs);
            diffTree.add(mergedGroupDiff);
        }
        Collection<DifferentialWeightedTree<ICallStackSymbol>> trees;
        if (diffTree.isEmpty()) {
            trees = WeightedTreeUtils.diffTrees(diffTree, originalTree, "Duration");
        } else {
            trees = WeightedTreeUtils.diffTrees(originalTree, diffTree, "Duration");
        }
        fDifferentialCallGraphProvider = new DifferentialCallGraphProvider(provider, trees);

    }

    public SpanAggregatedCalledFunction mergeCGlist(Collection<SpanAggregatedCalledFunction> procestssLi) {

        Iterator<SpanAggregatedCalledFunction> iterator = procestssLi.iterator();

        SpanAggregatedCalledFunction process = iterator.next();

        while (iterator.hasNext()) {

            SpanAggregatedCalledFunction child = iterator.next();
            if (!child.getObject().equals(process.getObject())) {
                throw new IllegalStateException("The Callgraphs should be the same for merge operation");
            }
            process = mergeCG(process,child);


        }

        return process;

    }

    public IWeightedTreeSet<ICallStackSymbol, Object, DifferentialWeightedTree<ICallStackSymbol>> getCallGraph() {
        return fDifferentialCallGraphProvider.getTreeSet();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void canceling() {
        // TODO Auto-generated method stub

    }

    public DifferentialWeightedTreeProvider<ICallStackSymbol> getDiffProvider() {
        return fDifferentialCallGraphProvider;

    }


    /* merge two SpanAggregatedCalledFunction  */

    public SpanAggregatedCalledFunction mergeCG(SpanAggregatedCalledFunction cg1, SpanAggregatedCalledFunction cg2) {

        cg1.merge(cg2);
        ///As the merge function in Weighted tree adds the values og two trees, we need to divide them by 2.
        cg1.tuneValues(cg2);
        return cg1;

    }


    @Override
    public void dispose() {
        super.dispose();
    }

}
