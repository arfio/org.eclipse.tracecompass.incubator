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

/**
 *
 * @author fateme faraji daneshgar
 *
 */
public class ProcessQuark {

    private int tsQuarck;
    private int attrQuarck;

    public ProcessQuark(int ts, int attr) {
        tsQuarck = ts;
        attrQuarck = attr;
    }

    public int getTsQuarck() {
        return tsQuarck;
    }

    public int getAttrQuarck() {
        return attrQuarck;
    }

    public void setTsQuarck(int val) {
        tsQuarck = val;
    }

    public void setAttrQuarck(int val) {
        attrQuarck = val;
    }

}
