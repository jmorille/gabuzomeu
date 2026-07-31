plugins {
    id("gabuzomeu.kotlin.jvm")
}

dependencies {
    // api : le type Rational fait partie de la surface publique de l'évaluateur.
    api(projects.core.shadok)
}
