package com.ibm.zosconnect.gradle

import org.gradle.testkit.runner.GradleRunner

import java.lang.management.ManagementFactory

class Utils {

    private static boolean isDebug = ManagementFactory.getRuntimeMXBean().getInputArguments().any { it.startsWith("-agentlib:jdwp") }

    static def runGradle(List args, File rootProjectDir, boolean shouldPass) {
        def result
        args.add("--info")
        args.add("--stacktrace")
        GradleRunner gradleRunner = GradleRunner
            .create()
            .withProjectDir(rootProjectDir)
            .withPluginClasspath()
            .withArguments(args)
            .withDebug(isDebug)
            .withGradleVersion("6.8")
            .forwardOutput()

        if (shouldPass) {
            result = gradleRunner.build()
        } else {
            result = gradleRunner.buildAndFail()
        }

        return result
    }
}
