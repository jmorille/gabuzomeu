package eu.ttbox.gabuzomeu.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation

/**
 * Les variantes d'affichage de l'horloge numérique, **propres à chaque widget posé**.
 *
 * Le stockage est l'état Glance de l'instance — un DataStore Preferences indexé par
 * identifiant de widget. Deux horloges côte à côte peuvent donc afficher l'une des glyphes,
 * l'autre des noms, ce qu'un réglage global n'aurait pas permis.
 *
 * @property notation l'écriture de l'heure. Les trois valeurs de [ShadokNotation] sont déjà
 *   implémentées et testées dans `:core:shadok` : il n'y avait rien à réinventer, seulement à
 *   laisser le choix.
 * @property showDecimalTime l'heure décimale en petit, sous l'heure Shadok — comme la
 *   calculatrice affiche la traduction décimale sous les glyphes.
 * @property dateFormat l'écriture de la date, jour de la semaine compris. Un seul réglage
 *   plutôt que deux interrupteurs : c'est ce qui empêche l'état incohérent où l'on masquait la
 *   date et où « VENDREDI » restait affiché.
 *
 * @property dateAboveTime place la date au-dessus de l'heure au lieu de dessous. L'heure
 *   décimale, elle, reste sous l'heure Shadok : elle en est la traduction, pas une information
 *   de date, et la séparer de ce qu'elle traduit n'aurait pas de sens.
 * @property background l'arrière-plan, réglable de la même façon sur les trois genres de
 *   widget : c'est pourquoi il vit dans son propre type plutôt qu'ici.
 *
 * Ces choix ne sont qu'un souhait : la place disponible peut imposer d'en effacer une partie.
 * C'est [ClockWidgetLayout] qui arbitre.
 */
internal data class ClockWidgetOptions(
    val notation: ShadokNotation = ShadokNotation.GLYPHS,
    val showDecimalTime: Boolean = false,
    val dateFormat: ClockDateFormat = ClockDateFormat.WEEKDAY_AND_LONG,
    val dateAboveTime: Boolean = false,
    val background: WidgetBackgroundOptions = WidgetBackgroundOptions(),
) {

    companion object {

        private val NOTATION = stringPreferencesKey("clock-notation")
        private val SHOW_DECIMAL_TIME = booleanPreferencesKey("clock-show-decimal-time")
        private val DATE_FORMAT = stringPreferencesKey("clock-date-format")
        private val DATE_ABOVE_TIME = booleanPreferencesKey("clock-date-above-time")

        /** Les valeurs d'un widget qui n'a jamais été configuré. */
        val DEFAULT: ClockWidgetOptions = ClockWidgetOptions()

        /**
         * Relit les options depuis l'état d'une instance.
         *
         * Toute valeur absente ou illisible retombe sur [DEFAULT] : un état vide est le cas
         * normal — celui d'un widget posé sans passer par l'écran de configuration, que
         * `configuration_optional` autorise — et non une anomalie. Une notation inconnue peut
         * quant à elle venir d'une version antérieure de l'application.
         */
        fun from(preferences: Preferences): ClockWidgetOptions = ClockWidgetOptions(
            notation = preferences[NOTATION]
                ?.let { stored -> ShadokNotation.entries.firstOrNull { it.name == stored } }
                ?: DEFAULT.notation,
            showDecimalTime = preferences[SHOW_DECIMAL_TIME] ?: DEFAULT.showDecimalTime,
            dateFormat = preferences[DATE_FORMAT]
                ?.let { stored -> ClockDateFormat.entries.firstOrNull { it.name == stored } }
                ?: DEFAULT.dateFormat,
            dateAboveTime = preferences[DATE_ABOVE_TIME] ?: DEFAULT.dateAboveTime,
            background = WidgetBackgroundOptions.from(preferences),
        )

        /** Écrit les options dans l'état d'une instance. */
        fun writeTo(preferences: MutablePreferences, options: ClockWidgetOptions) {
            preferences[NOTATION] = options.notation.name
            preferences[SHOW_DECIMAL_TIME] = options.showDecimalTime
            preferences[DATE_FORMAT] = options.dateFormat.name
            preferences[DATE_ABOVE_TIME] = options.dateAboveTime
            WidgetBackgroundOptions.writeTo(preferences, options.background)
        }
    }
}
