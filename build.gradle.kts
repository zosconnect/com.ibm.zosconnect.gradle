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

plugins {
    id("groovy")
    id("java-gradle-plugin")
    id("maven-publish")
    id("war")
    `kotlin-dsl`
    id("signing")
}

// Strings needed for publishing
val artifactory_contextURL: String by project
val artifactoryGradlePluginLocalRepo: String by project
val artifactoryUser: String by project
val artifactoryApiKey: String by project
val ossrhUsername: String? by project
val ossrhPassword: String? by project

group = "com.ibm.zosconnect"
version = "1.0.0-local-SNAPSHOT"

dependencies {
    testImplementation("com.github.spotbugs:spotbugs-annotations:3.1.3")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(module = "groovy-all")
    }
    testImplementation("commons-io:commons-io:2.11.0")
    implementation(group = "org.openapitools", name = "openapi-generator-gradle-plugin", version = "5.3.0")
}

gradlePlugin {
    plugins {
        register("com.ibm.zosconnect.gradle") {
            id = "com.ibm.zosconnect.gradle"
            displayName = "IBM z/OS Connect Gradle Plugin"
            description = "Plugin to be used in z/OS Connect API projects"
            implementationClass = "com.ibm.zosconnect.gradle.ZosConnectPlugin"
        }
    }
}

signing {
    if (!version.toString().endsWith("SNAPSHOT")) {
        useGpgCmd()
        sign(publishing.publications)
    }
}

publishing {
    publications {
        create<MavenPublication>("zosConnectPlugin") {
            from(components["kotlin"])
            pom {
                name.set("IBM z/OS Connect Gradle Plugin")
                description.set("Plugin to be used in z/OS Connect API projects")
                url.set("https://www.ibm.com/docs/en/zosconnect/beta")
                licenses {
                    license {
                        name.set("Eclipse Public License, Version 2.0")
                        url.set("https://www.eclipse.org/legal/epl-2.0/")
                    }
                }
                developers {
                    developer {
                        name.set("Ben Cox")
                        email.set("ben.cox@uk.ibm.com")
                    }
                    developer {
                        name.set("Jack Dunning")
                        email.set("jack.dunning1@uk.ibm.com")
                    }
                    developer {
                        name.set("Kiara Jones")
                        email.set("kiara.jones@ibm.com")
                    }
                    developer {
                        name.set("Sue Bayliss")
                        email.set("sue_bayliss@uk.ibm.com")
                    }
                }
                scm {
                    url.set("https://www.ibm.com/docs/en/zosconnect/beta")
                }
            }
        }
    }
    repositories {
        if (version.toString().endsWith("SNAPSHOT")) {
            maven {
                name = "ibmArtifactoryRepo"
                url = uri("${artifactory_contextURL}/${artifactoryGradlePluginLocalRepo}/")
                credentials {
                    username = artifactoryUser
                    password = artifactoryApiKey
                }
            }
        } else {
            maven {
                name = "OSSRH"
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
        }
    }

}

repositories {
    mavenCentral()
}
