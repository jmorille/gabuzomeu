package eu.ttbox.gabuzomeu.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** D'où viennent les couleurs de l'arrière-plan. */
internal enum class WidgetBackgroundTheme {

    /** Le thème de l'appareil, clair ou sombre — et ses couleurs dynamiques. */
    SYSTEM,

    /** Forcé clair, quel que soit le réglage du téléphone. */
    LIGHT,

    /** Forcé sombre. */
    DARK,
}

/**
 * L'arrière-plan d'un widget, réglable **sur les trois genres**.
 *
 * Ces options ne dépendent ni de l'heure ni du cadran : elles sont donc portées à part, et
 * chaque widget les relit depuis son propre état Glance. C'est ce qui permet de poser un cadran
 * transparent à côté d'une horloge opaque.
 *
 * @property visible l'arrière-plan est peint. Décoché, le widget se fond dans le fond d'écran.
 * @property opacityPercent l'opacité de 0 à 100. Distincte de [visible] à dessein : on peut
 *   vouloir un fond très discret sans le supprimer, et retrouver son réglage en le rallumant.
 * @property theme d'où viennent les couleurs. [WidgetBackgroundTheme.SYSTEM] suit le téléphone
 *   et bénéficie des couleurs dynamiques ; les deux autres figent un rendu, utile quand le fond
 *   d'écran est clair alors que le téléphone est en thème sombre.
 */
internal data class WidgetBackgroundOptions(
    val visible: Boolean = true,
    val opacityPercent: Int = FULLY_OPAQUE,
    val theme: WidgetBackgroundTheme = WidgetBackgroundTheme.SYSTEM,
) {

    /** L'opacité effective : nulle si l'arrière-plan est masqué. */
    val effectiveOpacityPercent: Int
        get() = if (visible) opacityPercent.coerceIn(FULLY_TRANSPARENT, FULLY_OPAQUE) else 0

    companion object {

        const val FULLY_TRANSPARENT: Int = 0
        const val FULLY_OPAQUE: Int = 100

        private val VISIBLE = booleanPreferencesKey("background-visible")
        private val OPACITY = intPreferencesKey("background-opacity")
        private val THEME = stringPreferencesKey("background-theme")

        val DEFAULT: WidgetBackgroundOptions = WidgetBackgroundOptions()

        fun from(preferences: Preferences): WidgetBackgroundOptions = WidgetBackgroundOptions(
            visible = preferences[VISIBLE] ?: DEFAULT.visible,
            // Une valeur hors bornes, venue d'un stockage abîmé, est ramenée plutôt que refusée :
            // un widget n'a aucun moyen de signaler une erreur.
            opacityPercent = (preferences[OPACITY] ?: DEFAULT.opacityPercent)
                .coerceIn(FULLY_TRANSPARENT, FULLY_OPAQUE),
            theme = preferences[THEME]
                ?.let { stored -> WidgetBackgroundTheme.entries.firstOrNull { it.name == stored } }
                ?: DEFAULT.theme,
        )

        fun writeTo(preferences: MutablePreferences, options: WidgetBackgroundOptions) {
            preferences[VISIBLE] = options.visible
            preferences[OPACITY] = options.opacityPercent
            preferences[THEME] = options.theme.name
        }
    }
}
