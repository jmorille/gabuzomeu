package eu.ttbox.gabuzomeu.widget

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Les réglages d'arrière-plan, communs aux trois widgets.
 *
 * Le calcul des couleurs est volontairement pur pour être vérifié ici : une opacité mal appliquée
 * ou un clair/sombre inversé ne se voit pas à la relecture, et le constater sur l'appareil
 * demanderait de reposer un widget pour chaque combinaison de réglages.
 */
internal class WidgetBackgroundTest {

    // ------------------------------------------------------------------- l'opacité

    @ParameterizedTest(name = "{1} % d''opacite -> alpha {2}")
    @CsvSource(
        // La couleur de base est opaque ; seul l'alpha doit bouger.
        "0xFF336699, 100, 255",
        "0xFF336699, 50,  127",
        "0xFF336699, 10,  25",
        "0xFF336699, 0,   0",
    )
    fun `l'opacite ne touche que le canal alpha`(argb: Long, percent: Int, expectedAlpha: Int) {
        val result = WidgetBackground.applyOpacity(argb.toInt(), percent)

        assertEquals(expectedAlpha, (result ushr 24) and 0xFF, "alpha")
        // La teinte est conservée : un fond à 10 % est le même bleu, simplement discret.
        assertEquals(0x336699, result and 0xFFFFFF, "rouge/vert/bleu")
    }

    @Test
    fun `une opacite hors bornes est ramenee plutot que refusee`() {
        // Peut venir d'un stockage abîmé : un widget n'a aucun moyen de signaler une erreur.
        assertEquals(255, (WidgetBackground.applyOpacity(0xFF000000.toInt(), 500) ushr 24) and 0xFF)
        assertEquals(0, (WidgetBackground.applyOpacity(0xFF000000.toInt(), -20) ushr 24) and 0xFF)
    }

    @Test
    fun `l'opacite est monotone`() {
        // Monter le curseur ne doit jamais rendre le fond plus transparent.
        var previous = -1
        for (percent in 0..100) {
            val alpha =
                (WidgetBackground.applyOpacity(0xFF112233.toInt(), percent) ushr 24) and 0xFF
            assertTrue(alpha >= previous, "$percent % donne $alpha après $previous")
            previous = alpha
        }
    }

    // --------------------------------------------------------------- clair / sombre

    @Test
    fun `le mode systeme suit le telephone`() {
        assertTrue(WidgetBackground.isDark(WidgetBackgroundTheme.SYSTEM, systemIsDark = true))
        assertFalse(WidgetBackground.isDark(WidgetBackgroundTheme.SYSTEM, systemIsDark = false))
    }

    @Test
    fun `les modes forces ignorent le telephone`() {
        // Tout l'intérêt du réglage : un fond d'écran clair sous un téléphone en thème sombre
        // rendait les glyphes illisibles.
        listOf(true, false).forEach { systemIsDark ->
            assertFalse(WidgetBackground.isDark(WidgetBackgroundTheme.LIGHT, systemIsDark))
            assertTrue(WidgetBackground.isDark(WidgetBackgroundTheme.DARK, systemIsDark))
        }
    }

    // ------------------------------------------------------------------ persistance

    @Test
    fun `un etat vide donne un fond opaque suivant le telephone`() {
        val defaults = WidgetBackgroundOptions.from(mutablePreferencesOf())

        assertEquals(WidgetBackgroundOptions.DEFAULT, defaults)
        assertTrue(defaults.visible)
        assertEquals(100, defaults.opacityPercent)
        assertEquals(WidgetBackgroundTheme.SYSTEM, defaults.theme)
    }

    @ParameterizedTest
    @EnumSource(WidgetBackgroundTheme::class)
    fun `l'aller-retour conserve chaque reglage`(theme: WidgetBackgroundTheme) {
        val options = WidgetBackgroundOptions(visible = false, opacityPercent = 40, theme = theme)
        val preferences = mutablePreferencesOf()

        WidgetBackgroundOptions.writeTo(preferences, options)

        assertEquals(options, WidgetBackgroundOptions.from(preferences))
    }

    @Test
    fun `une opacite persistee hors bornes est ramenee a la relecture`() {
        val preferences = mutablePreferencesOf(intPreferencesKey("background-opacity") to 250)

        assertEquals(100, WidgetBackgroundOptions.from(preferences).opacityPercent)
    }

    @Test
    fun `un theme inconnu retombe sur le defaut`() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("background-theme") to "FLUORESCENT",
        )

        assertEquals(
            WidgetBackgroundTheme.SYSTEM,
            WidgetBackgroundOptions.from(preferences).theme,
        )
    }

    @Test
    fun `masquer l'arriere-plan annule l'opacite sans l'oublier`() {
        // L'opacité choisie est conservée pour le rallumage, mais ne s'applique pas tant que
        // l'arrière-plan est masqué. Deux réglages distincts, et c'est voulu.
        val hidden = WidgetBackgroundOptions(visible = false, opacityPercent = 70)

        assertEquals(0, hidden.effectiveOpacityPercent)
        assertEquals(70, hidden.opacityPercent)
        assertEquals(70, hidden.copy(visible = true).effectiveOpacityPercent)
    }

    @Test
    fun `tous les reglages se relisent, ce qui evite la fausse remise a zero`() {
        // Le bug corrigé : l'écran de configuration s'ouvrait sur les valeurs par défaut. Rouvrir
        // les paramètres d'un widget réglé semblait tout remettre à zéro, et valider sans y
        // toucher écrasait réellement les choix. L'écran relit maintenant cet état — encore
        // faut-il que TOUT y soit persisté, ce que ce test verrouille champ par champ.
        val options = ClockWidgetOptions(
            notation = ShadokNotation.BASE4,
            showDecimalTime = true,
            dateFormat = ClockDateFormat.SHADOK,
            dateAboveTime = true,
            background = WidgetBackgroundOptions(
                visible = false,
                opacityPercent = 15,
                theme = WidgetBackgroundTheme.LIGHT,
            ),
        )
        val preferences = mutablePreferencesOf()

        ClockWidgetOptions.writeTo(preferences, options)
        val reread = ClockWidgetOptions.from(preferences)

        assertEquals(options, reread)
        // Explicite sur chaque champ : une égalité globale ne dirait pas lequel a été oublié.
        assertEquals(ShadokNotation.BASE4, reread.notation)
        assertTrue(reread.showDecimalTime)
        assertEquals(ClockDateFormat.SHADOK, reread.dateFormat)
        assertTrue(reread.dateAboveTime)
        assertFalse(reread.background.visible)
        assertEquals(15, reread.background.opacityPercent)
        assertEquals(WidgetBackgroundTheme.LIGHT, reread.background.theme)
    }

    @Test
    fun `la position de la date par defaut est sous l'heure`() {
        assertFalse(ClockWidgetOptions.DEFAULT.dateAboveTime)
    }

    @Test
    fun `les reglages de fond voyagent avec les options de l'horloge`() {
        // L'horloge compose l'arrière-plan : écrire ses options doit persister les deux.
        val options = ClockWidgetOptions(
            dateFormat = ClockDateFormat.NUMERIC,
            background = WidgetBackgroundOptions(
                visible = true,
                opacityPercent = 30,
                theme = WidgetBackgroundTheme.DARK,
            ),
        )
        val preferences = mutablePreferencesOf()

        ClockWidgetOptions.writeTo(preferences, options)

        assertEquals(options, ClockWidgetOptions.from(preferences))
    }
}
