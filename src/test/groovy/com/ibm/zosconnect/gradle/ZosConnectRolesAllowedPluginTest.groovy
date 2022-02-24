/**
* Copyright IBM Corp. 2022
*/

package com.ibm.zosconnect.gradle

import spock.lang.Specification
import org.apache.commons.io.FileUtils
import static com.ibm.zosconnect.gradle.Utils.runGradle

/**
 * Test z/OS Connect plugin correctly generates @RolesAllowed annotations
 * in the API JAX-RS class when custom OAS 3.0 specification extension
 x-ibm-zcon-roles-allowed is specified in OpenAPI doc.
 * Notes:
 * yaml syntax supports either:
 * x-ibm-zcon-roles-allowed:
 * - roleName1
 * - roleName2
 * or:
 * x-ibm-zcon-roles-allowed: [roleName1, roleName2]
 * with this syntax LDAP distinguished names (dn) must be in quotes.
 * json syntax supports:
 * "x-ibm-zcon-roles-allowed":["roleName1", "roleName2"]
 */
class ZosConnectRolesAllowedPluginTest extends Specification {

    def "OpenAPI yaml document contains valid LDAP roles for clean build and subsequent build" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File openApiDocToTest = getFileInDir(new File("src/test/resources"), "OpenAPI_Valid_LdapRoles.yaml")
            File destFile = setUpTest(rootProjectName, openApiDocToTest)

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

            def expectedRoleMap = ["API": "@RolesAllowed({ \"cn=Staff,ou=groups,o=zosconnect,c=uk\" })",
                "employeesIdPut": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\" })",
                "employeesIdDelete": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\" })",
                "employeesGet": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\", \"cn=Clerk,ou=groups,o=zosconnect,c=uk\" })",
                "employeesPost": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\", \"cn=Clerk,ou=groups,o=zosconnect,c=uk\" })"]

            // Parse the generated JAX-RS class for @RolesAllowed annotations
            File generatedJaxrsJava = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java")
            def actualRoleMap = parseJaxRsForRolesAllowed(generatedJaxrsJava)

            assert expectedRoleMap.equals(actualRoleMap):"Map expected: ${expectedRoleMap} does not match actual ${actualRoleMap}"


        when: 'A gradle build is rerun'
            def resultWithRebuild = runGradle(["build"], destFile, true)

        then: 'All expected tasks are run and files produced'
            def expectedOutputStringsRebuild = [
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

            expectedOutputStringsRebuild.each { expectedOutputString ->
                assert resultWithRebuild.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            expectedOutputFiles.each { expectedOutputFile ->
                assert getFileInDir(destFile, expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
            }

            // Parse the generated JAX-RS class for @RolesAllowed annotations
            def generatedJaxrsJavaRebuild = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java")
            def actualRoleMapRebuild = parseJaxRsForRolesAllowed(generatedJaxrsJavaRebuild)

            assert expectedRoleMap.equals(actualRoleMapRebuild):"Map expected: ${expectedRoleMap} does not match actual ${actualRoleMapRebuild}"
    }

    def "OpenAPI json document contains valid LDAP roles" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File openApiDocToTest = getFileInDir(new File("src/test/resources"), "OpenAPI_Valid_LdapRoles.json")
            File destFile = setUpTest(rootProjectName, openApiDocToTest)

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

            def expectedRoleMap = ["API": "@RolesAllowed({ \"cn=Staff,ou=groups,o=zosconnect,c=uk\" })",
                "employeesIdPut": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\" })",
                "employeesIdDelete": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\" })",
                "employeesGet": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\", \"cn=Clerk,ou=groups,o=zosconnect,c=uk\" })",
                "employeesPost": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\", \"cn=Clerk,ou=groups,o=zosconnect,c=uk\" })"]

            // Parse the generated JAX-RS class for @RolesAllowed annotations
            File generatedJaxrsJava = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java")
            def actualRoleMap = parseJaxRsForRolesAllowed(generatedJaxrsJava)

            assert expectedRoleMap.equals(actualRoleMap):"Map expected: ${expectedRoleMap} does not match actual ${actualRoleMap}"
    }

    def "OpenAPI yaml document contains invalid LDAP roles - [] syntax mssing quotes" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File openApiDocToTest = getFileInDir(new File("src/test/resources"), "OpenAPI_Invalid_LdapRoles_MissingQuotes.yaml")
            File destFile = setUpTest(rootProjectName, openApiDocToTest)

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

            /*
            * This builds successfully but treats each of the comma separated parts of an
            * LDAP distinguished name (dn) as separate roles, which is not the intent.
            */
            def expectedRoleMap = ["API": "@RolesAllowed({ \"cn=Staff,ou=groups,o=zosconnect,c=uk\" })",
                "employeesIdPut": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\" })",
                "employeesIdDelete": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\" })",
                "employeesGet": "@RolesAllowed({ \"cn=Manager,ou=groups,o=zosconnect,c=uk\", \"cn=Clerk,ou=groups,o=zosconnect,c=uk\" })",
                "employeesPost": "@RolesAllowed({ \"cn=Manager\", \"ou=groups\", \"o=zosconnect\", \"c=uk\", \"cn=Clerk\", \"ou=groups\", \"o=zosconnect\", \"c=uk\" })"]

            // Parse the generated JAX-RS class for @RolesAllowed annotations
            File generatedJaxrsJava = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java")
            def actualRoleMap = parseJaxRsForRolesAllowed(generatedJaxrsJava)

            assert expectedRoleMap.equals(actualRoleMap):"Map expected: ${expectedRoleMap} does not match actual ${actualRoleMap}"
    }

    def "OpenAPI json document contains invalid roles - role name not inside quotes" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File openApiDocToTest = getFileInDir(new File("src/test/resources"), "OpenAPI_Invalid_Roles_MissingQuotes.json")
            File destFile = setUpTest(rootProjectName, openApiDocToTest)

        when: 'A gradle clean build is run'
            def resultWithCleanBuild = runGradle(["clean", "build"], destFile, false)

        then: 'The build should fail with an error and files are not produced'
            def expectedOutputStrings = [
                "> Configure project",
                "> Task :clean",
                "Deleting generated Java in",
                "Deleting generated config in",
                "> Task :zosConnectPreBuildTask",
                "> Task :openApiGenerate",
                "com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'Staff': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')",
                "BUILD FAILED"
            ]

            def unexpectedOutputFiles = ["build/libs/api.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithCleanBuild.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            unexpectedOutputFiles.each { unexpectedOutputFile ->
                assert !getFileInDir(destFile, unexpectedOutputFile).exists(): "Did not expect, but found, output file '${unexpectedOutputFile}'"
            }
    }

    def "OpenAPI json document contains invalid roles - role name not inside array" () {
        given: 'A minimal project'
            def rootProjectName = "basic"

            File openApiDocToTest = getFileInDir(new File("src/test/resources"), "OpenAPI_Invalid_Roles_MissingArray.json")
            File destFile = setUpTest(rootProjectName, openApiDocToTest)

        when: 'A gradle clean build is run'
            def resultWithCleanBuild = runGradle(["clean", "build"], destFile, false)

        then: 'The build should fail with an error and files are not produced'
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
                "> Task :compileJava FAILED",
                // Full line split to remove the line number to make further updates to contents of generated code break less tests
                // "OpenAPI json document contains invalid roles - role name not inside array/basic/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java:116: error: illegal start of type",
                "OpenAPI json document contains invalid roles - role name not inside array/basic/src/gen/java/com/ibm/zosconnect/api/EmployeesApi.java",
                "error: illegal start of type",
                "BUILD FAILED"
            ]

            def unexpectedOutputFiles = ["build/libs/api.war"]

            expectedOutputStrings.each { expectedOutputString ->
                assert resultWithCleanBuild.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
            }

            unexpectedOutputFiles.each { unexpectedOutputFile ->
                assert !getFileInDir(destFile, unexpectedOutputFile).exists(): "Did not expect, but found, output file '${unexpectedOutputFile}'"
            }
    }

    protected static def getFileInDir(File directory, String fileName) {
        return new File(directory.path + "/$fileName")
    }

    private def setUpTest(String rootProjectName, File openApiDoc) {
        // Copy API project to build folder
        File srcFile = getFileInDir(new File("build/resources/test"), rootProjectName)
        File destFile = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName")
        FileUtils.copyDirectory(srcFile, destFile)

        // Delete openapi.yaml from API project
        File existingOpenApiYaml = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName/src/main/api/openapi.yaml")
        FileUtils.forceDelete(existingOpenApiYaml)
        // Copy OpenAPI document to be tested containing x-ibm-zcon-roles-allowed extension
        // into API project as yaml or json
        def openApiFileType = openApiDoc.name.substring(openApiDoc.name.lastIndexOf('.') + 1)
        assert (openApiFileType == "yaml" | openApiFileType == "json"):"OpenAPI document to be tested does not have file extension yaml or json: ${openApiDoc}"
        def targetOpenApiFilename = null
        if (openApiFileType == "yaml") {
            targetOpenApiFilename = "openapi.yaml"
        } else if (openApiFileType == "json") {
            targetOpenApiFilename = "openapi.json"
        }
        File targetOpenApi = getFileInDir(new File("build/test-projects"), "$specificationContext.currentIteration.name/$rootProjectName/src/main/api/$targetOpenApiFilename")
        FileUtils.copyFile(openApiDoc, targetOpenApi)

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
     * Parse the generated JAX-RS java class for @RolesAllowed annotations.
     * @param the generated JAX-RS java class
     * @return Map where key is API operation and value is associated @RolesAllowed annotations
     */
    private LinkedHashMap parseJaxRsForRolesAllowed(File jaxrsClass) {
        def actualRoleMap = [:]
        def rolesAllowed = null
        // Parse file for @RolesAllowed annotations
        def lines = jaxrsClass.readLines()
        lines.each { line ->
            if (line.contains("@RolesAllowed")) {
                rolesAllowed = line.trim()
            } else if (line.contains("public class ")) {
                if (rolesAllowed != null) {
                    // Found API level annotation so add to actual map
                    actualRoleMap["API"] = rolesAllowed
                    // unset rolesAllowed
                    rolesAllowed = null
                }
            } else if (line.contains("public Response")) {
                def methodName = (line.tokenize()[2]).split("\\(")[0]
                if (rolesAllowed != null) {
                    // Found operation level annotation so add to actual map
                    actualRoleMap[methodName] = rolesAllowed
                    // unset rolesAllowed
                    rolesAllowed = null
                }
            }
        }
        return actualRoleMap
    }
}
