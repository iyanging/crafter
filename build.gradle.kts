import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    jacoco
    alias(libs.plugins.errorProne)
    alias(libs.plugins.nullaway)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.licenser)
    alias(libs.plugins.spotless)
}

repositories { mavenCentral() }

group = "io.github.iyanging"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

dependencies {
    api(libs.jspecify)

    compileOnly(libs.autoServiceAnnotations)

    annotationProcessor(libs.autoService)

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
        }
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

tasks.withType<Test> {
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        exceptionFormat = TestExceptionFormat.FULL
    }
}

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Empty(), sourcesJar = true))

    pom {
        url = "https://github.com/iyanging/crafter"
        description = ""
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

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    signAllPublications()
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
        ktfmt("0.50").kotlinlangStyle().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
            it.setRemoveUnusedImports(true)
        }
    }
    yaml {
        target("**/*.yaml", "**/*.yml")
        targetExclude("**/.venv/")
        indentWithSpaces(2)
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
