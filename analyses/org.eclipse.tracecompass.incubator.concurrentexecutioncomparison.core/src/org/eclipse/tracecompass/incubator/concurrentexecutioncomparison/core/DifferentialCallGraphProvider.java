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

import java.util.Collection;

import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
/**
 *
 * @author Fateme Faraji Daneshgar
 *
 * @param <N>
 *           The type of objects represented by each node in the tree
 */
public class DifferentialCallGraphProvider<N> extends DifferentialWeightedTreeProvider<N> {

    private final IWeightedTreeProvider<N, ?, WeightedTree<N>> fOriginalTree;

    public DifferentialCallGraphProvider(IWeightedTreeProvider originalTree, Collection trees) {
        super(originalTree, trees);
        fOriginalTree = originalTree;

        // TODO Auto-generated constructor stub
    }

    @Override
    public IDataPalette getPalette() {
        return DifferentialFlamePalette.getInstance();
    }

    @Override
    public String toDisplayString(DifferentialWeightedTree<N> tree) {
        String label = fOriginalTree.toDisplayString(tree.getOriginalTree());
        if (!Double.isNaN(tree.getDifference())) {
            return String.format("(%#.02f %% ) %s", tree.getDifference() * 100, label);
        }
        return label;

    }

}
