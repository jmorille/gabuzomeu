package eu.ttbox.gabuzomeu.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Accès au catalogue de versions depuis un convention plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.versionInt(alias: String): Int =
    libs.findVersion(alias).get().requiredVersion.toInt()

internal val Project.javaVersion: JavaVersion
    get() = JavaVersion.toVersion(libs.findVersion("jvmTarget").get().requiredVersion)

/**
 * Aligne la cible JVM de Kotlin sur celle de Java et fait échouer le build sur les
 * warnings du compilateur : sur un projet neuf il n'y a aucune dette à tolérer.
 */
internal fun Project.configureKotlinCompiler() {
    val target = JvmTarget.fromTarget(libs.findVersion("jvmTarget").get().requiredVersion)
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(target)
            allWarningsAsErrors.set(true)
        }
    }
}

internal fun Project.configureJUnitPlatform() {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
