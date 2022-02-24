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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import java.io.File
import org.gradle.api.tasks.StopExecutionException

/**
 * Apply method has configuration for java, war and openApiGenerate tasks.
 */
open class ZosConnectPlugin : Plugin<Project> {

    companion object {
        const val ZOS_CONNECT_EXTENSION = "zosConnectApi"
        const val JAVA_SRC_DIR = "src/gen/java"
        const val CLEAN = "clean"
        const val BUILD = "build"
        const val MAIN = "main"
        const val WAR = "war"
        const val JAVA = "java"
        const val COMPILE_JAVA = "compileJava"
        const val OPEN_API_GEN = "openApiGenerate"
        const val ZOSCONNECT_JAXRS_TEMPLATE = "zosConnectJaxRSTemplate"
        const val PRE_BUILD_TASK = "zosConnectPreBuildTask"
        const val zosAssets = "src/main/zosAssets"
        const val zosOperations = "src/main/operations"
        const val webXmlFile = "src/main/web.xml"
    }

    override fun apply(project: Project) {

        // Apply the Base Plugin
		project.pluginManager.apply(BasePlugin::class.java)

        // Create the extension to allow for configuration by the user
		project.extensions.create(ZOS_CONNECT_EXTENSION, ZosConnectPluginExtension::class.java)

        project.pluginManager.apply(JavaBasePlugin::class.java)
        project.pluginManager.apply(WarPlugin::class.java)
        project.pluginManager.apply("org.openapi.generator")

        project.tasks.getByName(COMPILE_JAVA).dependsOn(OPEN_API_GEN)
        project.tasks.getByName(OPEN_API_GEN).dependsOn(PRE_BUILD_TASK)

        project.afterEvaluate {
            // Add dependencies
            dependencies.add("providedCompile", "javax.ws.rs:javax.ws.rs-api:2.0")
            dependencies.add("providedCompile", "javax.enterprise:cdi-api:2.0")
            dependencies.add("providedCompile", "javax.validation:validation-api:2.0.1.Final")
            dependencies.add("providedCompile", "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.5")
            dependencies.add("providedCompile", "javax.annotation:javax.annotation-api:1.3.2")
            dependencies.add("providedCompile", "javax.servlet:servlet-api:2.5")
            dependencies.add("providedCompile", "org.eclipse.microprofile:microprofile:4.1")
            dependencies.add("providedCompile", "com.ibm.zosconnect:com.ibm.zosconnect.project.spi.jar:1.0.0")

            // Set java config
            val convention = convention.plugins[JAVA] as JavaPluginConvention
            convention.sourceCompatibility = JavaVersion.VERSION_1_8
            convention.targetCompatibility = JavaVersion.VERSION_1_8
            convention.sourceSets.getByName(MAIN).java.srcDir(JAVA_SRC_DIR)

            // Set war config
            val zosConnectPluginExtension = extensions.getByName(ZOS_CONNECT_EXTENSION) as ZosConnectPluginExtension

            val archiveFileName = zosConnectPluginExtension.archiveFileName

            val archiveVersion = zosConnectPluginExtension.archiveVersion

            val webAppFolder = "${projectDir}/src/main/webapp"

            val warTask = tasks.withType(War::class.java).getByName(WAR)

            warTask.archiveFileName.set(archiveFileName)
            warTask.archiveVersion.set(archiveVersion)

            warTask.webInf {
                with(copySpec().from(zosAssets).into(zosAssets))
                with(copySpec().from(zosOperations).into(zosOperations))
            }

            warTask.webXml = File(webXmlFile)

            val apiFolder = "${projectDir}/src/main/api"

            val templateDirectory = if (zosConnectPluginExtension.templateDirectory != "") {
                zosConnectPluginExtension.templateDirectory
            } else {
                "${project.buildDir}/resources/" + ZOSCONNECT_JAXRS_TEMPLATE
            }

            val apiConfigOpt = mapOf("apiType" to "PROVIDER", "generatePom" to "false")
            val apiProp = mapOf("jackson" to "true", "dateLibrary" to "java8")

            val apiTask = tasks.getByName(OPEN_API_GEN)

            val genFolder = "${projectDir}/src/gen"

            delete("${genFolder}/java/com/ibm/zosconnect/api/RestApplication.java")

            apiTask.setProperty("generatorName", "jaxrs-spec")

            if (File("${apiFolder}/openapi.json").exists()) {
                apiTask.setProperty("inputSpec", "${apiFolder}/openapi.json")
            } else if (File("${apiFolder}/openapi.yaml").exists()) {
                apiTask.setProperty("inputSpec", "${apiFolder}/openapi.yaml")
            } else {
                throw StopExecutionException("[ERROR] Skipping openApiGenerate task since inputSpec doesn't exist.")
            }

            apiTask.setProperty("outputDir", "$projectDir")
            apiTask.setProperty("apiPackage", "com.ibm.zosconnect.api")
            apiTask.setProperty("invokerPackage", "com.ibm.zosconnect.api")
            apiTask.setProperty("modelPackage", "com.ibm.zosconnect.api.model")
            apiTask.setProperty("library", "openliberty")
            apiTask.setProperty("configOptions", apiConfigOpt)
            apiTask.setProperty("additionalProperties", apiProp)
            apiTask.setProperty("templateDir", templateDirectory)

            apiTask.inputs.files(fileTree(apiFolder))
            apiTask.inputs.files(fileTree(templateDirectory))
            apiTask.outputs.files(fileTree(genFolder))

            apiTask.doLast({
                // Removes the zosConnectJaxRSTemplate from the user's files after it has been used by OpenApi Generate
                delete("${project.buildDir}/resources")

                // Delete generated models as they may contain invalid content due to OAS capabilities not yet supported by OpenApi Generator
                // We don't use the models instead just get JSON directly
                project.delete("${project.projectDir}/src/gen/java/com/ibm/zosconnect/api/model")
            })

            // Removes the zosConnectJaxRSTemplate from the user's files after the build
            val buildTask = tasks.getByName(BUILD)
            buildTask.doLast { delete("${project.buildDir}/resources") }

            // Extend the gradle clean to include the generated files that are gitignored and the temporary libary folder
            val cleanTask = tasks.getByName(CLEAN)
            cleanTask.doLast {
                println("Deleting generated Java in $genFolder")
                delete(fileTree(genFolder))

                println("Deleting generated config in $webAppFolder")
                delete(fileTree(webAppFolder))
            }
        }

        // Define zosConnectPreBuildTask
		project.tasks.register(PRE_BUILD_TASK, ZosConnectPluginCopyTask::class.java) {
			this.description = "This prepares your workspace to allow the z/OS Connect API Plugin to work"
			this.group = BasePlugin.BUILD_GROUP
		}
    }

}
