package eu.ttbox.gabuzomeu.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

/**
 * Les options d'affichage d'une horloge posée, et leur persistance.
 *
 * DataStore Preferences est une bibliothèque JVM : l'aller-retour se vérifie donc sans appareil,
 * alors que c'est bien le format réellement écrit dans l'état Glance de chaque instance.
 */
class ClockWidgetOptionsTest {

    @Test
    fun `un etat vide donne les valeurs par defaut`() {
        // Le cas de loin le plus fréquent : un widget posé sans passer par l'écran de réglages,
        // ce que `configuration_optional` autorise explicitement. Ce n'est pas une anomalie.
        assertEquals(ClockWidgetOptions.DEFAULT, ClockWidgetOptions.from(mutablePreferencesOf()))
    }

    @Test
    fun `les glyphes sont l'ecriture par defaut`() {
        // C'est l'identité de l'application : un widget neuf montre des glyphes Shadok.
        assertEquals(ShadokNotation.GLYPHS, ClockWidgetOptions.DEFAULT.notation)
        assertEquals(false, ClockWidgetOptions.DEFAULT.showDecimalTime)
        // Le comportement historique : jour de la semaine puis date longue.
        assertEquals(ClockDateFormat.WEEKDAY_AND_LONG, ClockWidgetOptions.DEFAULT.dateFormat)
    }

    @ParameterizedTest
    @EnumSource(ShadokNotation::class)
    fun `l'aller-retour conserve chaque ecriture`(notation: ShadokNotation) {
        val options = ClockWidgetOptions(
            notation = notation,
            showDecimalTime = true,
            dateFormat = ClockDateFormat.SHADOK,
        )
        val preferences = mutablePreferencesOf()

        ClockWidgetOptions.writeTo(preferences, options)

        assertEquals(options, ClockWidgetOptions.from(preferences))
    }

    @Test
    fun `l'aller-retour conserve chaque format de date`() {
        ClockDateFormat.entries.forEach { format ->
            listOf(true, false).forEach { decimal ->
                val options = ClockWidgetOptions(
                    notation = ShadokNotation.LABELS,
                    showDecimalTime = decimal,
                    dateFormat = format,
                )
                val preferences = mutablePreferencesOf()

                ClockWidgetOptions.writeTo(preferences, options)

                assertEquals(options, ClockWidgetOptions.from(preferences), "$format / $decimal")
            }
        }
    }

    @Test
    fun `un format de date inconnu retombe sur le defaut`() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("clock-date-format") to "EN_RUNES",
        )

        assertEquals(
            ClockWidgetOptions.DEFAULT.dateFormat,
            ClockWidgetOptions.from(preferences).dateFormat,
        )
    }

    @Test
    fun `une ecriture inconnue retombe sur le defaut plutot que de lever`() {
        // Peut venir d'un état écrit par une version antérieure, ou d'un stockage abîmé. Un
        // widget doit alors s'afficher quand même : il n'a aucun moyen de signaler une erreur.
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("clock-notation") to "HIEROGLYPHES",
        )

        assertEquals(
            ClockWidgetOptions.DEFAULT.notation,
            ClockWidgetOptions.from(preferences).notation,
        )
    }

    @Test
    fun `l'apercu montre deux chiffres differents dans chaque ecriture`() {
        // 6 h 6 : 6 vaut 12 en base 4, donc l'aperçu enchaîne bien deux chiffres distincts.
        assertEquals("_⅃:_⅃", ShadokTimeGlyphs.previewOf(ShadokNotation.GLYPHS))
        assertEquals("BuZo:BuZo", ShadokTimeGlyphs.previewOf(ShadokNotation.LABELS))
        assertEquals("12:12", ShadokTimeGlyphs.previewOf(ShadokNotation.BASE4))
    }
}
