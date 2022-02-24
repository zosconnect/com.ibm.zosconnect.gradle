/*
 * #%L
 * IBM z/OS Connect Gradle Plugin
 * %%
 * Copyright (C) 2021, 2022 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */

package com.ibm.zosconnect.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.TaskAction

open class ZosConnectPluginCopyTask : DefaultTask () {
    companion object {
        const val ZOSCONNECT_SPI_JAR = "com.ibm.zosconnect.project.spi.jar"
        const val ZOSCONNECT_JAXRS_TEMPLATE = "zosConnectJaxRSTemplate"
    }

    @get:OutputDirectory
    val resourcesFolder = "${project.buildDir}/resources/"

    @TaskAction
    fun copyTask() {
        // Gets the zosConnectJaxRSTemplate from the plugin jar and copy to user's project directory
        if (ZosConnectPlugin::class.java.getResource("/$ZOSCONNECT_JAXRS_TEMPLATE/")!!.path.endsWith(".jar!/$ZOSCONNECT_JAXRS_TEMPLATE/")) {
            project.copy {
                // The substring function removes the folder name and .jar! to allow it to be readable by the plugin
                from(project.zipTree(ZosConnectPlugin::class.java.getResource("/$ZOSCONNECT_JAXRS_TEMPLATE/")!!.path.substring(0, (ZosConnectPlugin::class.java.getResource("/$ZOSCONNECT_JAXRS_TEMPLATE/")!!.path.length - 26)))) {
                    exclude("com/**")
                    exclude("main/**")
                    exclude("META-INF/**")
                }
                into(resourcesFolder)
            }
        } else {  // Needed as the test uses the current version of the plugin build directly in the project and not the stable jar version from a repo, we need this logic.
            project.copy {
                from(project.fileTree(ZosConnectPlugin::class.java.getResource("/$ZOSCONNECT_JAXRS_TEMPLATE/")!!.path))
                into("$resourcesFolder$ZOSCONNECT_JAXRS_TEMPLATE/")
            }
        }
    }
}
