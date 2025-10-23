import java.net.URI
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    jacoco
    `maven-publish`
    signing
    alias(libs.plugins.errorProne)
    alias(libs.plugins.nullaway)
    alias(libs.plugins.licenser)
    alias(libs.plugins.spotless)
}

repositories { mavenCentral() }

group = "io.github.iyanging.crafter"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }

    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(libs.jspecify)

    implementation(libs.javapoet)

    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.elementary)
    testImplementation(libs.javaparser)
    testImplementation(libs.guava)
    testImplementation(libs.jakartaValidationApi)

    testRuntimeOnly(libs.junitPlatformLauncher)

    testAnnotationProcessor(project)

    errorprone(libs.errorProneCore)
    errorprone(libs.nullaway)
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disableWarningsInGeneratedCode = true
        errorproneArgs = listOf("-XepAllSuggestionsAsWarnings")
        checks =
            mapOf(
                "ReferenceEquality" to CheckSeverity.ERROR,
                "UnnecessaryParentheses" to CheckSeverity.OFF,
            )

        nullaway {
            error()
            annotatedPackages.add(project.group.toString())
            excludedClassAnnotations.addAll("javax.annotation.processing.Generated")
        }
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
        csv.required = false
    }
}

tasks.check { dependsOn(tasks.jacocoTestReport) }

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])

            pom {
                url = "https://github.com/iyanging/crafter"
                description = "Java annotation processor for generating Type-Safe Builder"
                licenses {
                    license {
                        name = "Mulan Permissive Software License v2"
                        url = "https://license.coscl.org.cn/MulanPSL2"
                    }
                }
                developers {
                    developer {
                        id = "iyanging"
                        name = "iyanging"
                        url = "https://github.com/iyanging/"
                    }
                }
                scm { url = "https://github.com/iyanging/crafter" }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = URI.create("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                val mavenCentralUsername: String? by project
                val mavenCentralPassword: String? by project

                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
    }
}

signing {
    val signingInMemoryKey: String? by project
    val signingInMemoryKeyPassword: String? by project

    useInMemoryPgpKeys(signingInMemoryKey, signingInMemoryKeyPassword)

    sign(publishing.publications)
}

license { rule(file("$rootDir/configs/license-template.txt")) }

tasks.check { dependsOn(tasks.checkLicenses) }

spotless {
    java {
        target("**/*.java")
        targetExclude("**/build/")
        importOrderFile("$rootDir/configs/eclipse-organize-imports.importorder")
        removeUnusedImports()
        eclipse().configFile("$rootDir/configs/eclipse-code-formatter.xml")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt().kotlinlangStyle().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
            it.setRemoveUnusedImports(true)
        }
    }
    yaml {
        target("**/*.yaml", "**/*.yml")
        targetExclude("**/.venv/")
        leadingTabsToSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
    json {
        target("**/*.json")
        targetExclude("**/.venv/")
        gson().indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
}
