/**
* Copyright IBM Corp. 2021
*/

package com.ibm.zosconnect.gradle

import groovy.io.FileType
import org.apache.commons.io.IOUtils
import spock.lang.Specification
import org.apache.commons.io.FileUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static com.ibm.zosconnect.gradle.Utils.runGradle

class ZosConnectPluginTest extends Specification {

    def "The OAS is used correctly to build, rebuild and then clean" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File destFile = setUpTest(rootProjectName)

        when: 'A gradle build is run'
            def resultWithOAS = runGradle(["build"], destFile, true)

        then: 'All expected tasks are run and files produced'
            def expectedOutputStrings = [
                "> Configure project",
                "> Task :zosConnectPreBuildTask",
                "> Task :openApiGenerate",
                "Processing operation employeesIdGet",
                "Processing operation employeesIdPut",
                "Processing operation employeesIdDelete",
                "Processing operation employeesGet",
                "Processing operation employeesPost",
                "> Task :compileJava",
                "> Task :war",
                "BUILD SUCCESSFUL"
            ]
            def expectedOutputFiles = ["build/libs/api.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithOAS.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            expectedOutputFiles.each { expectedOutputFile ->
                assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
            }

        when: 'A gradle build is rerun'
            def rerunResultWithOAS = runGradle(["build"], destFile, true)

        then: 'All expected tasks are run with war UP-TO-DATE and files remain'
            def newExpectedOutputStrings = [
                "> Configure project",
                "> Task :zosConnectPreBuildTask",
                "Task ':zosConnectPreBuildTask' is not up-to-date",
                "> Task :openApiGenerate",
                "Processing operation employeesIdGet",
                "Processing operation employeesIdPut",
                "Processing operation employeesIdDelete",
                "Processing operation employeesGet",
                "Processing operation employeesPost",
                "> Task :compileJava",
                "> Task :war UP-TO-DATE",
                "Skipping task ':war' as it is up-to-date.",
                "BUILD SUCCESSFUL"
            ]

            newExpectedOutputStrings.each { expectedOutputString ->
                assert rerunResultWithOAS.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            expectedOutputFiles.each { expectedOutputFile ->
                assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
            }

        when: 'Clean is run'
            def cleanResult = runGradle(["clean"], destFile, true)

        then: 'Relevant files are deleted'
            def cleanExpectedOutputStrings = [
                "> Configure project",
                "> Task :clean",
                "Deleting generated Java in",
                "Deleting generated config in",
                "BUILD SUCCESSFUL"
            ]
            def unexpectedOutputFiles = ["build/libs/api.war"]

            cleanExpectedOutputStrings.each { expectedOutputString ->
                assert cleanResult.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            unexpectedOutputFiles.each { unexpectedOutputFile ->
                assert !getFileInDir(destFile, unexpectedOutputFile).exists(): "Following a project clean: Did not expect, but found, output file '${unexpectedOutputFile}'"
            }
    }

    def "A clean build can be run" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File destFile = setUpTest(rootProjectName)

        when: 'A gradle clean build is run'
            def resultWithCleanBuild = runGradle(["clean", "build"], destFile, true)

        then: 'Relevant files are deleted and then all expected tasks are run and files produced'
            def expectedOutputStrings = [
                "> Configure project",
                "> Task :clean",
                "Deleting generated Java in",
                "Deleting generated config in",
                "> Task :zosConnectPreBuildTask",
                "> Task :openApiGenerate",
                "Processing operation employeesIdGet",
                "Processing operation employeesIdPut",
                "Processing operation employeesIdDelete",
                "Processing operation employeesGet",
                "Processing operation employeesPost",
                "> Task :compileJava",
                "> Task :war",
                "BUILD SUCCESSFUL"
            ]
            def expectedOutputFiles = ["build/libs/api.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithCleanBuild.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            expectedOutputFiles.each { expectedOutputFile ->
                assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
            }
    }

    def "The operations and zOS assets are copied into the WAR file" () {
        given: 'A project with operations and z/OS assets'
        def rootProjectName = "operationsAndAssets"

        File destFile = setUpTest(rootProjectName)

        when: 'A gradle clean build is run'
        runGradle(["clean", "build"], destFile, true)

        then: 'the WAR file contains the operations and z/OS assets'
        def expectedOutputFiles = ["build/libs/api.war"]

        expectedOutputFiles.each { expectedOutputFile ->
            assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
        }

        expectFileStructureInZip(destFile, "src/main/operations", "build/libs/api.war", "WEB-INF/src/main/operations")
        expectFileStructureInZip(destFile, "src/main/zosAssets", "build/libs/api.war", "WEB-INF/src/main/zosAssets")
    }

    def "The variables can be changed from plugin extension" () {
        given: 'A minimal project with change variables'
            def rootProjectName = "editedVariables"

            File destFile = setUpTest(rootProjectName)

        when: 'A gradle build is run'
            def resultWithExtension = runGradle(["build"], destFile, true)

        then: 'All expected tasks are run and files produced'
            def expectedOutputStrings = [
                "> Configure project",
                "> Task :zosConnectPreBuildTask",
                "> Task :openApiGenerate",
                "Processing operation employeesIdGet",
                "Processing operation employeesIdPut",
                "Processing operation employeesIdDelete",
                "Processing operation employeesGet",
                "Processing operation employeesPost",
                "> Task :compileJava",
                "> Task :war",
                "BUILD SUCCESSFUL"
            ]
            def expectedOutputFiles = ["build/libs/changed.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithExtension.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            expectedOutputFiles.each { expectedOutputFile ->
                assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
            }
    }

    def "No OAS available" () {
        given: 'A minimal project not containing an OAS'
            def rootProjectName = "noSpec"

            File destFile = setUpTest(rootProjectName)

        when: 'A gradle build is run'
            def resultWithoutOAS = runGradle(["build"], destFile, false)

        then: 'The build should fail with an error and files are not produced'
            def expectedOutputStrings = [
                "> Configure project",
                "FAILURE: Build failed with an exception.",
                "[ERROR] Skipping openApiGenerate task since inputSpec doesn't exist.",
                "BUILD FAILED"
            ]

            def unexpectedOutputFiles = ["build/libs/api.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithoutOAS.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            unexpectedOutputFiles.each { unexpectedOutputFile ->
                assert !getFileInDir(destFile, unexpectedOutputFile).exists(): "Did not expect, but found, output file '${unexpectedOutputFile}'"
            }

    }

    def "The generated Java contains some key expected content" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File destFile = setUpTest(rootProjectName)

        when: 'A gradle build is run'
            def resultWithOAS = runGradle(["build"], destFile, true)

        then: 'All expected tasks are run, files are produced and java is generated'
            def expectedOutputStrings = [
                "> Configure project",
                "> Task :zosConnectPreBuildTask",
                "> Task :openApiGenerate",
                "Processing operation employeesIdGet",
                "Processing operation employeesIdPut",
                "Processing operation employeesIdDelete",
                "Processing operation employeesGet",
                "Processing operation employeesPost",
                "> Task :compileJava",
                "> Task :war",
                "BUILD SUCCESSFUL"
            ]
            def expectedOutputFiles = ["build/libs/api.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithOAS.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            expectedOutputFiles.each { expectedOutputFile ->
                assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
            }

            // Assert models directory does not exist
            assert !new File(destFile.path + "/src/gen/java/com/ibm/zosconnect/api/model").exists() : "/src/gen/java/com/ibm/zosconnect/api/model directory exists when it should have been deleted."

            // Test contents of RestApplication.java
            def restApplicationExpectedJavaCode = [
                "@ApplicationPath(\"\")",
                "classes.add(com.ibm.zosconnect.api.EmployeesApi.class);"
            ]
            expectStringsInFile("basic/src/gen/java/com/ibm/zosconnect/api/RestApplication.java", restApplicationExpectedJavaCode)

            // Test contents of EmployeesApi.java
            def employeesApiExpectedJavaCode = [
                "@Path(\"/employees\")",
                "@ApplicationScoped",
                "private Application application;",
                "private UriInfo uriInfo;",
                "private HttpHeaders headers;",
                "private SecurityContext securityContext;",
                "com.ibm.zosconnect.engine.Operation operation;",
                "MetricRegistry metricRegistry;",
                "@SimplyTimed(name",
                "@Counted(name",
                "com.ibm.zosconnect.engine.OperationRequest opRequest = new com.ibm.zosconnect.engine.OperationRequest(",
                "opRequest.setMetricRegistry(metricRegistry);",
                "com.ibm.zosconnect.engine.OperationResponse opResponse = operation.processOperation(opRequest);",
                "return Response.status("
            ]
            expectStringsInFile("basic/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java", employeesApiExpectedJavaCode)
    }

    protected static def getFileInDir(File directory, String fileName) {
        return new File(directory.path + "/$fileName")
    }

    private def setUpTest(String rootProjectName) {
        File srcFile = getFileInDir(new File("build/resources/test"), rootProjectName)
        File destFile = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName")
        FileUtils.copyDirectory(srcFile, destFile)

        return destFile
    }

    private void expectStringsInFile(String relativeFilePath, List<String> expectedStrings) {
        File fileToSearch = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$relativeFilePath")

        def lines = fileToSearch.readLines()
        def foundAll = false
        def i = 0

        // For each line in file while we still have strings to find keep searching
        lines.each { line ->
            // Only process line if we're still searching
            if (!foundAll) {
                if (line.trim().contains(expectedStrings.get(i))) {
                    // Match found increment i
                    i++
                    if (i == expectedStrings.size()) {
                        // All matches found
                        foundAll = true
                    }
                }
            }
        }

        if (!foundAll) {
            assert false : "Failed to find match in file [$relativeFilePath] for string: " + expectedStrings.get(i)
        }
    }

    /**
     * <p>Checks that all paths in {@code projectRelativeFileStructure} are also found in the {@code basePathInZip} directory
     * inside {@code projectRelativeZipFile}, and have the same content.</p>
     *
     * <p>Currently just checks files - could check directories too!</p>
     * @param projectRelativeFileStructure
     * @param projectRelativeZipFile
     * @param basePathInZip
     */
    static void expectFileStructureInZip(File project, String projectRelativeFileStructure, String projectRelativeZipFile, String basePathInZip = "") {
        File topFileToExpect = getFileInDir(project, projectRelativeFileStructure)

        // Find all files that we will expect
        def unfoundFiles = [] as Set
        topFileToExpect.eachFileRecurse(FileType.FILES) { File fileToExpect ->
            def relativePath = new File(basePathInZip, topFileToExpect.relativePath(fileToExpect))
            unfoundFiles.add(relativePath.toString())
        }
        File zipFile = getFileInDir(project, projectRelativeZipFile)
        assert zipFile.exists()

        // Cross off every file we find in the zip that we were expecting
        new ZipFile(zipFile).withCloseable { ZipFile zf ->
            zf.entries().findAll { !it.directory }.each { ZipEntry zipFileEntry ->
                if (unfoundFiles.remove(zipFileEntry.name)) {
                    // We were looking for this file; now check the content is the same
                    zf.getInputStream(zipFileEntry).withCloseable { InputStream zis ->
                        def zipText = IOUtils.toString(zis, StandardCharsets.UTF_8.name())
                        def expectedText = project.toPath().resolve(projectRelativeFileStructure).resolve(Paths.get(basePathInZip).relativize(Paths.get(zipFileEntry.name))).toFile().text
                        assert zipText == expectedText
                    }
                }
            }
        }

        assert unfoundFiles.empty
    }


}
