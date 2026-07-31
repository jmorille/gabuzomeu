plugins {
    id("gabuzomeu.android.library")
    id("gabuzomeu.android.compose")
}

android {
    namespace = "eu.ttbox.gabuzomeu.widget"
}

dependencies {
    // Le widget n'affiche que les NOMS Shadok (Ga/Bu/Zo/Meu), pas les glyphes : il
    // n'a donc besoin ni de la police embarquée, ni du moteur d'évaluation.
    implementation(projects.core.shadok)

    implementation(libs.androidx.core.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    // goAsync() dans le receiver : le rendu Glance est suspendu.
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("test"))
}
