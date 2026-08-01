package eu.ttbox.gabuzomeu.ui.game

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Le jeu d'apprentissage.
 *
 * Tout ce qui compte ici est pur : la génération d'une question ne connaît ni Compose ni
 * Android, seulement des valeurs. Le tirage passe par un [Random] **fourni**, ce qui rend les
 * parties reproductibles — sans quoi aucune de ces propriétés ne serait vérifiable.
 */
class ShadokQuizTest {

    private fun questions(count: Int, mastery: Int = 0, seed: Int = 42) =
        Random(seed).let { random -> List(count) { ShadokQuiz.nextQuestion(random, mastery) } }

    @Test
    fun `la reponse figure toujours parmi les choix`() {
        // L'invariant sans lequel le jeu serait injouable — et le piège réel : placer la
        // réponse après les leurres la faisait sortir des quatre choix retenus.
        questions(count = 500, mastery = 12).forEach { question ->
            assertTrue(question.answer in question.choices, "$question")
        }
    }

    @Test
    fun `il y a toujours quatre choix distincts`() {
        questions(count = 500, mastery = 12).forEach { question ->
            assertEquals(ShadokQuiz.CHOICE_COUNT, question.choices.size, "$question")
            assertEquals(question.choices.size, question.choices.distinct().size, "$question")
        }
    }

    @Test
    fun `des le premier palier, quatre choix sont possibles`() {
        // Le cas limite : à un seul chiffre il n'y a que quatre valeurs en tout, et « 0 » n'a
        // presque pas de leurre plausible. Le complément au hasard existe pour ça.
        questions(count = 200, mastery = 0).forEach { question ->
            assertEquals(ShadokQuiz.CHOICE_COUNT, question.choices.size, "$question")
            assertTrue(question.choices.all { it in 0..3 }, "$question")
        }
    }

    @ParameterizedTest(name = "{0} reussites -> jusqu'a {1}")
    @CsvSource(
        // Un rang, puis deux, puis trois — et on s'arrête là.
        "0,  3",
        "3,  3",
        "4,  15",
        "7,  15",
        "8,  63",
        "20, 63",
        "99, 63",
    )
    fun `la difficulte suit les reussites, et plafonne`(mastery: Int, expected: Int) {
        assertEquals(expected, ShadokQuiz.ceilingFor(mastery))
    }

    @Test
    fun `le plafond ne redescend jamais`() {
        // Monotone : réussir ne doit jamais rendre le jeu plus facile.
        var previous = 0
        for (mastery in 0..50) {
            val ceiling = ShadokQuiz.ceilingFor(mastery)
            assertTrue(ceiling >= previous, "$mastery donne $ceiling apres $previous")
            previous = ceiling
        }
    }

    @Test
    fun `aucun choix ne sort du palier`() {
        questions(count = 300, mastery = 4).forEach { question ->
            assertTrue(question.choices.all { it in 0..15 }, "$question")
        }
    }

    @Test
    fun `les deux sens sont proposes`() {
        // Lire et écrire ne s'apprennent pas séparément : les deux doivent tomber.
        val directions = questions(count = 100, mastery = 8).map { it.direction }.toSet()

        assertEquals(QuizDirection.entries.toSet(), directions)
    }

    /**
     * La première question dont la réponse est [answer].
     *
     * Le [Random] est créé **une seule fois**, hors de la séquence : le construire à l'intérieur
     * rejouerait éternellement la même question, et la recherche ne terminerait jamais. La borne
     * est là pour échouer franchement plutôt que de tourner sans fin si la valeur ne sortait pas.
     */
    private fun questionAnswering(answer: Int, mastery: Int = 8): QuizQuestion {
        val random = Random(answer)
        return generateSequence { ShadokQuiz.nextQuestion(random, mastery) }
            .take(SEARCH_LIMIT)
            .first { it.answer == answer }
    }

    @Test
    fun `la base 4 lue comme du decimal figure parmi les leurres`() {
        // LE leurre qui apprend quelque chose : six s'écrit « 12 », et l'erreur naturelle est
        // de lire douze. Sans lui, on éliminerait les mauvaises réponses par leur taille.
        val question = questionAnswering(answer = 6)

        assertTrue(12 in question.choices, "${question.choices}")
    }

    @Test
    fun `l'ordre des rangs inverse figure parmi les leurres`() {
        // 9 s'écrit 21 ; lu à l'envers, 12 vaut six. Deux nombres qui ne se distinguent qu'en
        // lisant les rangs dans le bon sens.
        val question = questionAnswering(answer = 9)

        assertTrue(6 in question.choices, "${question.choices}")
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [0, 1, 2, 3, 6, 15, 16, 42, 63])
    fun `les deux ecritures d'un nombre sont celles du moteur`(value: Int) {
        // Le jeu n'a pas sa propre conversion : il montrerait autre chose que la calculatrice.
        val expected = mapOf(
            0 to ("◯" to "Ga"),
            1 to ("_" to "Bu"),
            2 to ("⅃" to "Zo"),
            3 to ("◿" to "Meu"),
            6 to ("_⅃" to "BuZo"),
            15 to ("◿◿" to "MeuMeu"),
            16 to ("_◯◯" to "BuGaGa"),
            42 to ("⅃⅃⅃" to "ZoZoZo"),
            63 to ("◿◿◿" to "MeuMeuMeu"),
        )

        assertEquals(expected.getValue(value).first, ShadokQuiz.glyphsOf(value))
        assertEquals(expected.getValue(value).second, ShadokQuiz.labelsOf(value))
    }

    @Test
    fun `une meme graine rejoue la meme partie`() {
        // La reproductibilité n'est pas qu'un confort de test : c'est ce qui garantit que le
        // tirage ne dépend d'aucun état caché.
        assertEquals(questions(count = 20, seed = 1), questions(count = 20, seed = 1))
    }

    private companion object {
        /** Largement au-delà du nécessaire : seize valeurs possibles, deux sens, quatre choix. */
        const val SEARCH_LIMIT = 2000
    }
}
