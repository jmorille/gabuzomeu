package eu.ttbox.gabuzomeu.widget

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ce qui tient dans la place accordée au widget.
 *
 * Vérifier cela par un test plutôt qu'à l'œil est le seul moyen raisonnable : sinon il faudrait
 * redimensionner le widget à la main dans chaque hauteur possible, pour chacun des six formats
 * de date. Ici, la règle est explicite et les bords sont couverts.
 */
internal class ClockWidgetLayoutTest {

    @Test
    fun `sur une cellule de haut, aucune ligne de date ne tient`() {
        // La demande à l'origine de ce calcul : une hauteur de 1 pour un widget facile à caser.
        // À 40 dp il n'y a de place que pour l'heure, et les lignes doivent s'effacer seules
        // plutôt que d'être rognées par le système.
        assertEquals(0, ClockWidgetLayout.secondaryLineBudget(40.dp))
        assertEquals(0, ClockWidgetLayout.secondaryLineBudget(50.dp))
    }

    @Test
    fun `le budget de lignes croit avec la hauteur`() {
        // Monotone : agrandir un widget ne doit jamais faire disparaître une ligne.
        var previous = 0
        var height = 40
        while (height <= 300) {
            val budget = ClockWidgetLayout.secondaryLineBudget(height.dp)
            assertTrue(budget >= previous, "${height}dp donne $budget après $previous")
            previous = budget
            height += 5
        }
    }

    @Test
    fun `une hauteur confortable laisse la place aux deux lignes`() {
        // 2 cellules environ : le jour de la semaine ET la date longue.
        assertTrue(ClockWidgetLayout.secondaryLineBudget(140.dp) >= 2)
    }

    @Test
    fun `la marge se resserre quand la hauteur est comptee`() {
        // Sur un widget écrasé, 12 dp de marge en haut et en bas mangeraient l'heure.
        assertTrue(ClockWidgetLayout.paddingFor(40.dp) < ClockWidgetLayout.paddingFor(200.dp))
    }

    @ParameterizedTest
    @EnumSource(ClockDateFormat::class)
    fun `aucun format ne survit a une cellule de haut`(format: ClockDateFormat) {
        // Y compris SHADOK, le plus long : c'est la hauteur qui tranche, pas le format.
        val options = ClockWidgetOptions(dateFormat = format, showDecimalTime = false)

        assertEquals(emptyList(), ClockWidgetLayout.linesFor(options, 40.dp))
    }

    @ParameterizedTest
    @EnumSource(ClockDateFormat::class)
    fun `le nombre de lignes annonce par le format est celui produit`(format: ClockDateFormat) {
        // L'accord entre `lineCount` et ce que `linesFor` fabrique réellement, à hauteur libre.
        val options = ClockWidgetOptions(dateFormat = format, showDecimalTime = false)

        val lines = ClockWidgetLayout.linesFor(options, 400.dp)

        assertEquals(format.lineCount, lines.size, format.name)
    }

    @Test
    fun `le format NONE n'affiche ni jour de la semaine ni date`() {
        // LE bug corrigé : « afficher la date » ne gouvernait pas la ligne du jour, et
        // « VENDREDI » restait visible alors que la date était masquée.
        val options = ClockWidgetOptions(dateFormat = ClockDateFormat.NONE)

        val lines = ClockWidgetLayout.linesFor(options, 400.dp)

        assertTrue(ClockLine.WEEKDAY !in lines, "le jour de la semaine doit disparaître aussi")
        assertTrue(ClockLine.DATE !in lines)
    }

    @Test
    fun `le jour seul n'entraine pas la date, et l'inverse non plus`() {
        val weekdayOnly = ClockWidgetOptions(dateFormat = ClockDateFormat.WEEKDAY)
        val dateOnly = ClockWidgetOptions(dateFormat = ClockDateFormat.LONG)

        assertEquals(
            listOf(ClockLine.WEEKDAY),
            ClockWidgetLayout.linesFor(weekdayOnly, 400.dp),
        )
        assertEquals(
            listOf(ClockLine.DATE),
            ClockWidgetLayout.linesFor(dateOnly, 400.dp),
        )
    }

    @Test
    fun `l'heure decimale passe avant la date quand la place manque`() {
        // L'ordre EST la règle d'effacement : on garde l'heure lisible avant le reste.
        val options = ClockWidgetOptions(
            dateFormat = ClockDateFormat.WEEKDAY_AND_LONG,
            showDecimalTime = true,
        )
        val height = heightForExactly(1)

        assertEquals(listOf(ClockLine.DECIMAL_TIME), ClockWidgetLayout.linesFor(options, height))
    }

    @Test
    fun `avec de la place, tout s'affiche dans l'ordre`() {
        val options = ClockWidgetOptions(
            dateFormat = ClockDateFormat.WEEKDAY_AND_LONG,
            showDecimalTime = true,
        )

        assertEquals(
            listOf(ClockLine.DECIMAL_TIME, ClockLine.WEEKDAY, ClockLine.DATE),
            ClockWidgetLayout.linesFor(options, 400.dp),
        )
    }

    // ------------------------------------------------ la date au-dessus de l'heure

    @Test
    fun `par defaut la date reste sous l'heure`() {
        val lines = listOf(ClockLine.DECIMAL_TIME, ClockLine.WEEKDAY, ClockLine.DATE)

        val (above, below) = ClockWidgetLayout.splitAroundTime(lines, dateAboveTime = false)

        assertEquals(emptyList(), above)
        assertEquals(lines, below)
    }

    @Test
    fun `la date passe au-dessus mais l'heure decimale reste dessous`() {
        // L'heure décimale est la traduction de l'heure Shadok : la séparer de ce qu'elle traduit
        // rendrait l'affichage incompréhensible. Seules les lignes de date changent de côté.
        val lines = listOf(ClockLine.DECIMAL_TIME, ClockLine.WEEKDAY, ClockLine.DATE)

        val (above, below) = ClockWidgetLayout.splitAroundTime(lines, dateAboveTime = true)

        assertEquals(listOf(ClockLine.WEEKDAY, ClockLine.DATE), above)
        assertEquals(listOf(ClockLine.DECIMAL_TIME), below)
    }

    @Test
    fun `l'ordre du jour et de la date est conserve au-dessus`() {
        // Le jour de la semaine avant la date : l'ordre de lecture ne dépend pas du côté.
        val lines = listOf(ClockLine.WEEKDAY, ClockLine.DATE)

        val (above, _) = ClockWidgetLayout.splitAroundTime(lines, dateAboveTime = true)

        assertEquals(listOf(ClockLine.WEEKDAY, ClockLine.DATE), above)
    }

    @Test
    fun `la repartition ne perd et ne duplique aucune ligne`() {
        // L'invariant qui compte : déplacer la date ne doit pas faire disparaître une information.
        listOf(true, false).forEach { above ->
            val lines = listOf(ClockLine.DECIMAL_TIME, ClockLine.WEEKDAY, ClockLine.DATE)

            val (before, after) = ClockWidgetLayout.splitAroundTime(lines, above)

            assertEquals(lines.size, before.size + after.size, "au-dessus = $above")
            assertEquals(lines.toSet(), (before + after).toSet())
        }
    }

    /** La plus petite hauteur qui accorde exactement [lines] lignes secondaires. */
    private fun heightForExactly(lines: Int) = (40..400)
        .first { height -> ClockWidgetLayout.secondaryLineBudget(height.dp) == lines }
        .dp
}
