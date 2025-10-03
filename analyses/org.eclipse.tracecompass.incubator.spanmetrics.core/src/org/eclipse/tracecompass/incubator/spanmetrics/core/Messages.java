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

package org.eclipse.tracecompass.incubator.spanmetrics.core;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for Sched_Wakeup Sched_switch latency analysis.
 *
 * @author Maryam Ekhlasi
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.spanmetrics.messages"; //$NON-NLS-1$
    /**
     * Segment ID
     */
    public static @Nullable String SegmentAspectHelpText_Sid;
    /**
     * Segment ID
     */
    public static @Nullable String SegmentAspectName_Sid;
    /**
     * Operation or span name
     */
    public static @Nullable String SegmentAspectHelpText_OperationName;
    /**
     * Operation or span name
     */
    public static @Nullable String SegmentAspectName_OperationName;
    /**
     * TraceID of each request
     */
    public static @Nullable String SegmentAspectHelpText_Traceid;
    /**
     * ThreadID of each service
     */
    public static @Nullable String SegmentAspectHelpText_Threadid;


    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    /**
     * Helper method to expose externalized strings as non-null objects.
     */
    static String getMessage(@Nullable String msg) {
        if (msg == null) {
            return StringUtils.EMPTY;
        }
        return msg;
    }
}
