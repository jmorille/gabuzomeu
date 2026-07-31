package eu.ttbox.gabuzomeu.core.shadok

/**
 * L'heure écrite en Shadok.
 *
 * Reprend la convention du widget d'origine (`ClockWidgetProvider.java:82-87`) : les
 * heures et les minutes sont converties **chacune comme un nombre**, et non chiffre à
 * chiffre. 14:35 donne donc `MeuZo:ZoGaMeu` — 14₁₀ = 32₄ et 35₁₀ = 203₄.
 *
 * Fonction pure, sans dépendance Android : le widget se teste sans appareil.
 */
object ShadokClock {

    const val TIME_SEPARATOR: Char = ':'

    private const val LAST_HOUR = 23
    private const val LAST_MINUTE = 59

    /**
     * @param notation [ShadokNotation.LABELS] par défaut — les noms prononcés, seule
     *   écriture lisible sur un widget, et la seule qui ne demande aucune police
     *   particulière.
     */
    fun format(hour: Int, minute: Int, notation: ShadokNotation = ShadokNotation.LABELS): String {
        require(hour in 0..LAST_HOUR) { "Heure hors plage : $hour" }
        require(minute in 0..LAST_MINUTE) { "Minute hors plage : $minute" }
        return buildString {
            append(numberToShadok(hour, notation))
            append(TIME_SEPARATOR)
            append(numberToShadok(minute, notation))
        }
    }

    private fun numberToShadok(value: Int, notation: ShadokNotation): String =
        ShadokFormatter.format(
            number = ShadokConverter.toBase4(Rational.of(value)),
            notation = notation,
            // Un entier a toujours un développement fini : jamais d'approximation ici.
            markApproximation = false,
        )
}
