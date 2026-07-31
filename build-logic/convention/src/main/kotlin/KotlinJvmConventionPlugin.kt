import eu.ttbox.gabuzomeu.buildlogic.configureJUnitPlatform
import eu.ttbox.gabuzomeu.buildlogic.configureKotlinCompiler
import eu.ttbox.gabuzomeu.buildlogic.libs
import eu.ttbox.gabuzomeu.buildlogic.versionInt
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Modules Kotlin purs (:core:shadok, :core:eval) : aucune dépendance Android, donc
 * des tests qui s'exécutent sur la JVM en une fraction de seconde. C'est ce qui rend
 * le développement de la logique métier en TDD praticable.
 */
class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(versionInt("jvmTarget")))
            }
            sourceCompatibility = JavaVersion.toVersion(versionInt("jvmTarget"))
            targetCompatibility = JavaVersion.toVersion(versionInt("jvmTarget"))
        }

        val catalog = libs
        fun lib(alias: String) = catalog.findLibrary(alias).get()

        dependencies {
            add("testImplementation", platform(lib("junit-bom")))
            add("testImplementation", lib("junit-jupiter"))
            add("testRuntimeOnly", lib("junit-platform-launcher"))
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
        }

        configureKotlinCompiler()
        configureJUnitPlatform()
    }
}
