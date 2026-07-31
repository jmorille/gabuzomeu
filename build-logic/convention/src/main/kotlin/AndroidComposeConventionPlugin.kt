import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import eu.ttbox.gabuzomeu.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Active Compose. Depuis Kotlin 2.0 le compilateur Compose est livré avec Kotlin
 * lui-même, via le plugin org.jetbrains.kotlin.plugin.compose — il n'y a plus de
 * table de correspondance « compilateur Compose / version de Kotlin » à maintenir.
 *
 * S'applique indifféremment à un module application ou bibliothèque.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.findByType(ApplicationExtension::class.java)?.apply {
            buildFeatures { compose = true }
        }
        extensions.findByType(LibraryExtension::class.java)?.apply {
            buildFeatures { compose = true }
        }

        val catalog = libs
        fun lib(alias: String) = catalog.findLibrary(alias).get()

        dependencies {
            // Le BOM aligne toutes les versions des artefacts Compose entre eux.
            add("implementation", platform(lib("compose-bom")))
            add("androidTestImplementation", platform(lib("compose-bom")))

            add("implementation", lib("compose-ui"))
            add("implementation", lib("compose-ui-graphics"))
            add("implementation", lib("compose-ui-tooling-preview"))
            add("implementation", lib("compose-foundation"))
            add("debugImplementation", lib("compose-ui-tooling"))
        }
    }
}
