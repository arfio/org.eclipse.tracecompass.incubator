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

package org.eclipse.tracecompass.incubator.concurrentstatesystem.core;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 *
 * @author fateme faraji daneshgar
 *
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    public static @Nullable String TmfConcurrentStateSystemAnalysisModule_PropertiesAnalysisNotExecuted;
    /**
     * Backend property text
     */
    public static @Nullable String TmfConcurrentStateSystemAnalysisModule_PropertiesBackend;
    /**
     * File size property text
     */
    public static @Nullable String TmfConcurrentStateSystemAnalysisModule_PropertiesFileSize;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
