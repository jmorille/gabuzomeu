plugins {
    `kotlin-dsl`
}

group = "eu.ttbox.gabuzomeu.buildlogic"

java {
    toolchain {
        languageVersion =
            JavaLanguageVersion.of(
                libs.versions.jvmTarget
                    .get()
                    .toInt(),
            )
    }
}

dependencies {
    // compileOnly : ces plugins sont fournis par le build principal à l'exécution.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "gabuzomeu.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "gabuzomeu.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "gabuzomeu.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("kotlinJvm") {
            id = "gabuzomeu.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
    }
}
