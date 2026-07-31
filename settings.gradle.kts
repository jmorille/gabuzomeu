pluginManagement {
    // Les convention plugins vivent dans un build composite : ils sont compilés avant
    // le build principal et exposés comme de vrais plugins (gabuzomeu.android.*).
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Aucun module ne déclare ses propres dépôts : tout passe par ici.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "gabuzomeu"

include(":app")
include(":widget")
include(":core:shadok")
include(":core:eval")
