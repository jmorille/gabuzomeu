plugins {
    id("gabuzomeu.kotlin.jvm")
}

// Ce module ne doit JAMAIS dépendre d'Android : c'est ce qui garantit que la logique
// de conversion Shadok est testable sur la JVM, sans émulateur ni Robolectric.
// Le code d'origine dépendait d'un Context uniquement pour lire les glyphes depuis
// les ressources — ils sont désormais des constantes Kotlin.
