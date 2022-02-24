/*
 * #%L
 * IBM z/OS Connect Gradle Plugin
 * %%
 * Copyright (C) 2021 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */

package com.ibm.zosconnect.gradle

/**
*   Both archiveFileName and archiveVersion are both configurable through the DSL block in build.gradle

*   Example DSL block

    zosConnectApi {
        archiveFileName = "customFileName.war"
        archiveVersion = "1.0"
    }

 */
open class ZosConnectPluginExtension {
    var archiveFileName = "api.war"
    var archiveVersion = ""
    var templateDirectory = ""
}
