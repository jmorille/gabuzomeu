package eu.ttbox.gabuzomeu.ui.game

import eu.ttbox.gabuzomeu.core.shadok.Rational
import eu.ttbox.gabuzomeu.core.shadok.ShadokConverter
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation
import kotlin.random.Random

/** Ce qui est montré, et donc ce qu'il faut retrouver. */
enum class QuizDirection {

    /** On montre des glyphes, il faut désigner la valeur décimale. */
    READ_SHADOK,

    /** On montre un décimal, il faut désigner son écriture Shadok. */
    WRITE_SHADOK,
}

/**
 * Une question, réduite à des **valeurs**.
 *
 * L'écran décide comment les écrire selon [direction] : c'est ce qui permet aux deux sens de
 * partager la même génération, et aux tests de vérifier le fond sans toucher à l'affichage.
 */
data class QuizQuestion(val direction: QuizDirection, val answer: Int, val choices: List<Int>)

/**
 * Le jeu : apprendre à lire les nombres Shadok en les reconnaissant.
 *
 * Deux partis pris pédagogiques, et ce sont eux qui font la différence avec un tirage au hasard.
 *
 * **Les leurres sont les vraies erreurs.** Proposer trois nombres pris au hasard n'apprend rien :
 * on élimine par la taille sans jamais lire. Les leurres sont donc les confusions que l'on fait
 * réellement — la base 4 lue comme du décimal (`12` pour six), les chiffres dans l'ordre inverse,
 * un rang décalé, un chiffre voisin.
 *
 * **La difficulté suit les réussites**, pas le nombre de questions : on reste sur un chiffre
 * jusqu'à ce qu'il soit acquis, et l'échec ne fait pas régresser — il redonne simplement une
 * chance au même palier.
 */
object ShadokQuiz {

    const val CHOICE_COUNT: Int = 4

    /** Réussites nécessaires pour ouvrir un rang de plus. */
    private const val CORRECT_PER_LEVEL = 4

    /** Trois rangs suffisent : au-delà, on ne lit plus un nombre, on l'épelle. */
    private const val MAX_PLACES = 3

    /** Le plus grand nombre proposé après [mastery] réussites. */
    fun ceilingFor(mastery: Int): Int {
        val places = (mastery / CORRECT_PER_LEVEL + 1).coerceAtMost(MAX_PLACES)
        // Trois rangs en base 4 vont jusqu'à 333₄, soit 63.
        var ceiling = 1
        repeat(places) { ceiling *= ShadokDigit.RADIX }
        return ceiling - 1
    }

    fun nextQuestion(random: Random, mastery: Int): QuizQuestion {
        val ceiling = ceilingFor(mastery)
        val answer = random.nextInt(ceiling + 1)
        // Les deux sens alternent au hasard : lire et écrire ne s'apprennent pas séparément.
        val direction = if (random.nextBoolean()) {
            QuizDirection.READ_SHADOK
        } else {
            QuizDirection.WRITE_SHADOK
        }

        // La réponse **en tête** avant le `take` : la placer en queue permettrait aux leurres
        // de la pousser hors des quatre choix, et la question serait sans solution.
        val choices = (listOf(answer) + distractorsFor(answer, ceiling))
            .distinct()
            .take(CHOICE_COUNT)
            .shuffled(random)

        return QuizQuestion(direction = direction, answer = answer, choices = choices)
    }

    /**
     * Les erreurs plausibles autour de [answer], de la plus instructive à la plus anodine.
     *
     * L'ordre compte : `take` garde les premières, donc les confusions de lecture passent avant
     * les simples voisins. Une liste complétée au hasard en dernier recours, faute de quoi les
     * petits nombres — où il y a peu de leurres possibles — offriraient moins de choix.
     */
    private fun distractorsFor(answer: Int, ceiling: Int): List<Int> {
        val digits = digitsOf(answer)

        val candidates = buildList {
            // L'erreur reine : lire « 12 » comme douze au lieu de six.
            add(digits.joinToString(separator = "") { it.base4Char.toString() }.toInt())
            // L'ordre des rangs inversé.
            add(valueOf(digits.reversed()))
            // Un rang de trop, un rang de moins.
            add(answer * ShadokDigit.RADIX)
            add(answer / ShadokDigit.RADIX)
            // Un chiffre voisin.
            add(answer + 1)
            add(answer - 1)
        }

        val plausible = candidates
            .filter { it != answer && it in 0..ceiling }
            .distinct()

        // Complément au hasard : sans lui, « 0 » n'aurait qu'un leurre et le jeu montrerait
        // deux boutons au lieu de quatre.
        val filler = (0..ceiling).filter { it != answer && it !in plausible }
        return plausible + filler
    }

    private fun digitsOf(value: Int): List<ShadokDigit> =
        ShadokConverter.toBase4(Rational.of(value)).integerDigits

    private fun valueOf(digits: List<ShadokDigit>): Int =
        digits.fold(0) { acc, digit -> acc * ShadokDigit.RADIX + digit.value }

    /** L'écriture en glyphes d'une valeur — ce que l'écran affiche ou propose. */
    fun glyphsOf(value: Int): String = ShadokFormatter.format(
        number = ShadokConverter.toBase4(Rational.of(value)),
        notation = ShadokNotation.GLYPHS,
        markApproximation = false,
    )

    /** Son écriture en noms : ce que lit TalkBack, puisque les formes ne se prononcent pas. */
    fun labelsOf(value: Int): String = ShadokFormatter.format(
        number = ShadokConverter.toBase4(Rational.of(value)),
        notation = ShadokNotation.LABELS,
        markApproximation = false,
    )
}
