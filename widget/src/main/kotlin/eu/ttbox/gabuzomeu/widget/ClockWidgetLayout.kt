package eu.ttbox.gabuzomeu.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ce qui tient dans la place accordée au widget.
 *
 * Un widget d'accueil peut être redimensionné librement, y compris à **une seule cellule de
 * haut** — c'est même ce qui en fait un élément facile à caser. À cette hauteur, il n'y a de
 * place que pour l'heure : afficher la date en plus la ferait rogner par le système, ou
 * écraserait l'heure jusqu'à l'illisible.
 *
 * D'où ce calcul, volontairement séparé du dessin : il ne manipule que des hauteurs, se teste
 * donc sur la JVM, et c'est exactement le genre de règle qu'on ne peut pas vérifier à l'œil
 * sans redimensionner le widget à la main dans toutes les tailles possibles.
 */
internal object ClockWidgetLayout {

    /** La hauteur que l'heure se réserve, quelle que soit la taille du widget. */
    private val TIME_HEIGHT = 40.dp

    /** La hauteur d'une ligne secondaire : décimale, jour de la semaine ou date. */
    private val SECONDARY_LINE_HEIGHT = 17.dp

    /** La marge, en fraction de la hauteur, entre les deux bornes ci-dessous. */
    private const val PADDING_RATIO = 0.08f

    private val MIN_PADDING = 4.dp
    private val MAX_PADDING = 12.dp

    /**
     * Le nombre de lignes secondaires qui tiennent sous l'heure.
     *
     * Zéro sur un widget d'une cellule de haut, ce qui est le comportement voulu : les
     * informations de date disparaissent d'elles-mêmes plutôt que d'être tronquées.
     */
    fun secondaryLineBudget(availableHeight: Dp): Int {
        val forLines = availableHeight - paddingFor(availableHeight) * 2 - TIME_HEIGHT
        if (forLines <= 0.dp) return 0
        return (forLines.value / SECONDARY_LINE_HEIGHT.value).toInt()
    }

    /**
     * La marge du widget : resserrée quand la hauteur est comptée.
     *
     * **Proportionnelle** à la hauteur, et non un palier. Un palier — 4 dp en dessous de 90 dp,
     * 12 dp au-dessus — paraissait plus simple, mais il rendait [secondaryLineBudget] non
     * monotone : passer de 89 à 90 dp faisait perdre 16 dp de marge d'un coup, donc *une ligne*.
     * Agrandir le widget en effaçait alors une information, ce qui est absurde. Le test de
     * monotonie l'a attrapé.
     */
    fun paddingFor(availableHeight: Dp): Dp =
        (availableHeight.value * PADDING_RATIO).coerceIn(MIN_PADDING.value, MAX_PADDING.value).dp

    /**
     * Les lignes secondaires réellement affichées, dans l'ordre de priorité.
     *
     * L'ordre **est** la règle d'effacement : ce qui est en fin de liste disparaît d'abord. La
     * date passe donc après l'heure décimale, conformément au principe que l'on garde l'heure
     * lisible avant tout le reste.
     */
    fun linesFor(options: ClockWidgetOptions, availableHeight: Dp): List<ClockLine> {
        val wanted = buildList {
            if (options.showDecimalTime) add(ClockLine.DECIMAL_TIME)
            when (options.dateFormat) {
                ClockDateFormat.NONE -> Unit

                ClockDateFormat.WEEKDAY -> add(ClockLine.WEEKDAY)

                ClockDateFormat.WEEKDAY_AND_LONG -> {
                    add(ClockLine.WEEKDAY)
                    add(ClockLine.DATE)
                }

                ClockDateFormat.LONG,
                ClockDateFormat.NUMERIC,
                ClockDateFormat.SHADOK,
                -> add(ClockLine.DATE)
            }
        }
        return wanted.take(secondaryLineBudget(availableHeight))
    }

    /**
     * Répartit les lignes retenues de part et d'autre de l'heure.
     *
     * L'heure décimale reste toujours **sous** l'heure Shadok, même quand la date passe dessus :
     * elle en est la traduction, et la séparer de ce qu'elle traduit rendrait l'affichage
     * incompréhensible. Seules les lignes de date changent de côté.
     *
     * @return les lignes à placer avant l'heure, puis celles à placer après.
     */
    fun splitAroundTime(
        lines: List<ClockLine>,
        dateAboveTime: Boolean,
    ): Pair<List<ClockLine>, List<ClockLine>> {
        if (!dateAboveTime) return emptyList<ClockLine>() to lines
        return lines.partition { line -> line != ClockLine.DECIMAL_TIME }
    }
}

/** Une ligne sous l'heure. */
internal enum class ClockLine { DECIMAL_TIME, WEEKDAY, DATE }
