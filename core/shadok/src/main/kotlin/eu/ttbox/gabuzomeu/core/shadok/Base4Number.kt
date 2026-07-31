package eu.ttbox.gabuzomeu.core.shadok

/**
 * Un nombre écrit en base 4 avec les chiffres Shadok.
 *
 * Le signe est un champ dédié, jamais un chiffre : c'est ce qui évite la classe de bug
 * du code d'origine, où un `-` traversant la table de conversion produisait un
 * `NullPointerException`.
 *
 * @property integerDigits partie entière, du chiffre le plus significatif au moins
 *   significatif. Jamais vide : zéro s'écrit `[GA]`.
 * @property fractionDigits partie fractionnaire, du premier chiffre après la virgule
 *   au dernier. Vide pour un entier.
 * @property approximate `true` si le développement en base 4 ne termine pas et a donc
 *   été tronqué (cas de 1/3 ou de 0.1 décimal).
 */
data class Base4Number(
    val negative: Boolean,
    val integerDigits: List<ShadokDigit>,
    val fractionDigits: List<ShadokDigit> = emptyList(),
    val approximate: Boolean = false,
) {
    init {
        require(integerDigits.isNotEmpty()) {
            "La partie entière ne peut être vide : zéro s'écrit [GA]"
        }
    }

    val isZero: Boolean
        get() = fractionDigits.isEmpty() && integerDigits.all { it == ShadokDigit.GA }
}
