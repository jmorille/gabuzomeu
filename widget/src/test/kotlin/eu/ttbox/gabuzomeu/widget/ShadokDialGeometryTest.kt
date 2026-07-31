package eu.ttbox.gabuzomeu.widget

import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La géométrie du cadran.
 *
 * Ces tests existent parce qu'un cadran se trompe silencieusement : une aiguille à 90° de sa
 * place reste une aiguille plausible, et sur un widget de 180 dp personne ne le voit. Les
 * angles, eux, ne se discutent pas.
 */
class ShadokDialGeometryTest {

    private val tolerance = 0.01f

    @ParameterizedTest(name = "{0}h{1} -> aiguille des heures a {2} degres")
    @CsvSource(
        "0,  0,  0",
        "3,  0,  90",
        "6,  0,  180",
        "9,  0,  270",
        // Le cadran est sur douze heures : midi retombe en haut, comme minuit.
        "12, 0,  0",
        "15, 0,  90",
        "23, 0,  330",
        // La dérive : à la demie, l'aiguille est entre deux heures.
        "6,  30, 195",
        "0,  30, 15",
        "11, 59, 359.5",
    )
    fun `l'aiguille des heures derive avec les minutes`(hour: Int, minute: Int, expected: Float) {
        assertEquals(expected, ShadokDialGeometry.hourHandDegrees(hour, minute), tolerance)
    }

    @Test
    fun `l'aiguille des heures ne saute jamais`() {
        // Entre deux minutes consécutives, l'écart doit être exactement la dérive d'une
        // minute. Un saut signalerait un arrondi entier là où il faut des flottants.
        var previous = ShadokDialGeometry.hourHandDegrees(0, 0)
        for (minutes in 1 until 12 * 60) {
            val current = ShadokDialGeometry.hourHandDegrees(minutes / 60, minutes % 60)
            assertEquals(
                ShadokDialGeometry.HOUR_DRIFT_PER_MINUTE,
                current - previous,
                tolerance,
                "saut à ${minutes / 60}h${minutes % 60}",
            )
            previous = current
        }
    }

    @ParameterizedTest(name = "minute {0} -> {1} degres")
    @CsvSource("0, 0", "15, 90", "30, 180", "45, 270", "59, 354")
    fun `l'aiguille des minutes fait un tour par heure`(minute: Int, expected: Float) {
        assertEquals(expected, ShadokDialGeometry.minuteHandDegrees(minute), tolerance)
    }

    @Test
    fun `les deux aiguilles se superposent a midi et a minuit`() {
        assertEquals(
            ShadokDialGeometry.hourHandDegrees(12, 0),
            ShadokDialGeometry.minuteHandDegrees(0),
            tolerance,
        )
    }

    @Test
    fun `les quatre reperes sont Ga en haut puis dans le sens horaire`() {
        // L'ordre EST la lisibilité du cadran : Ga=0 en haut, et l'on compte en tournant.
        assertEquals(
            listOf(ShadokDigit.GA, ShadokDigit.BU, ShadokDigit.ZO, ShadokDigit.MEU),
            ShadokDialGeometry.quarterMarkers.map { it.digits.single() },
        )
        assertEquals(
            listOf(0f, 90f, 180f, 270f),
            ShadokDialGeometry.quarterMarkers.map { it.degrees },
        )
    }

    @Test
    fun `les reperes des quarts sont exactement les quatre chiffres Shadok`() {
        assertEquals(
            ShadokDigit.entries.toSet(),
            ShadokDialGeometry.quarterMarkers.map { it.digits.single() }.toSet(),
        )
    }

    // ------------------------------------------------- le cadran des douze heures

    @Test
    fun `le cadran des heures porte douze reperes, midi en haut`() {
        val markers = ShadokDialGeometry.hourMarkers

        assertEquals(12, markers.size)
        // Le repère de midi est à 0° : le sommet, comme sur toute horloge.
        assertEquals(1, markers.count { it.degrees == 0f })
    }

    @ParameterizedTest(name = "{0}h s''ecrit {1} et se place a {2} degres")
    @CsvSource(
        // Les heures en base 4 : 1..3 tiennent sur un chiffre, 4..12 en demandent deux.
        "1,  BU,      30",
        "2,  ZO,      60",
        "3,  MEU,     90",
        "4,  BU|GA,   120",
        "5,  BU|BU,   150",
        "6,  BU|ZO,   180",
        "7,  BU|MEU,  210",
        "8,  ZO|GA,   240",
        "9,  ZO|BU,   270",
        "10, ZO|ZO,   300",
        "11, ZO|MEU,  330",
        // 12 = 30 en base 4, et retombe au sommet.
        "12, MEU|GA,  0",
    )
    fun `chaque heure est ecrite en base 4 a sa place`(
        hour: Int,
        expectedDigits: String,
        expectedDegrees: Float,
    ) {
        val marker = ShadokDialGeometry.hourMarkers[hour - 1]

        assertEquals(expectedDigits, marker.digits.joinToString("|") { it.name })
        assertEquals(expectedDegrees, marker.degrees, tolerance)
    }

    @Test
    fun `aucun repere d'heure ne depasse deux chiffres`() {
        // La borne dont dépend la place réservée à chaque repère : douze, le plus grand, s'écrit
        // MeuGa. Un troisième chiffre déborderait sur ses voisins.
        val widest = ShadokDialGeometry.hourMarkers.maxOf { it.digits.size }

        assertEquals(2, widest)
    }

    @Test
    fun `les reperes d'heure sont regulierement espaces`() {
        // Trente degrés d'écart, sans exception : un repère mal placé sauterait aux yeux.
        val sorted = ShadokDialGeometry.hourMarkers.map { it.degrees }.sorted()

        sorted.zipWithNext().forEach { (first, second) ->
            assertEquals(ShadokDialGeometry.DEGREES_PER_HOUR, second - first, tolerance)
        }
    }

    @Test
    fun `le repere d'une heure coincide avec l'aiguille a cette heure pile`() {
        // L'accord entre les deux calculs : à 3 h 00 l'aiguille doit désigner le repère de 3 h.
        // C'est ce qui garantit qu'un cadran juste ne porte pas des repères décalés.
        for (hour in 1..12) {
            val marker = ShadokDialGeometry.hourMarkers[hour - 1]

            assertEquals(
                marker.degrees,
                ShadokDialGeometry.hourHandDegrees(hour % 12, minute = 0),
                tolerance,
                "${hour}h",
            )
        }
    }

    @ParameterizedTest(name = "{0} degres -> ({1}, {2})")
    @CsvSource(
        // Centre (100,100), rayon 50. L'ordonnée DESCEND à l'écran : le haut est y = 50.
        "0,   100, 50",
        "90,  150, 100",
        "180, 100, 150",
        "270, 50,  100",
        "360, 100, 50",
    )
    fun `zero degre est en haut, et l'on tourne dans le sens horaire`(
        degrees: Float,
        expectedX: Float,
        expectedY: Float,
    ) {
        val point = ShadokDialGeometry.pointAt(
            degrees,
            radius = 50f,
            centerX = 100f,
            centerY = 100f,
        )

        assertEquals(expectedX, point.x, tolerance)
        assertEquals(expectedY, point.y, tolerance)
    }

    @Test
    fun `un point reste toujours sur le cercle`() {
        // Invariant : la distance au centre vaut le rayon, quel que soit l'angle. C'est ce qui
        // garantit qu'aucun repère ne débordera du cadran.
        val radius = 80f
        for (degrees in 0 until 360) {
            val point = ShadokDialGeometry.pointAt(degrees.toFloat(), radius, 100f, 100f)
            val distance = kotlin.math.hypot(point.x - 100f, point.y - 100f)

            assertEquals(radius, distance, 0.05f, "à $degrees°")
        }
    }

    @Test
    fun `un rayon nul ramene au centre`() {
        val point = ShadokDialGeometry.pointAt(123f, radius = 0f, centerX = 7f, centerY = 9f)

        assertEquals(7f, point.x, tolerance)
        assertEquals(9f, point.y, tolerance)
    }

    @Test
    fun `une heure hors plage est refusee`() {
        assertThrows<IllegalArgumentException> { ShadokDialGeometry.hourHandDegrees(24, 0) }
        assertThrows<IllegalArgumentException> { ShadokDialGeometry.hourHandDegrees(-1, 0) }
        assertThrows<IllegalArgumentException> { ShadokDialGeometry.hourHandDegrees(0, 60) }
        assertThrows<IllegalArgumentException> { ShadokDialGeometry.minuteHandDegrees(60) }
    }

    @Test
    fun `tous les angles de la journee restent dans un tour`() {
        for (hour in 0..23) {
            for (minute in 0..59) {
                val hourAngle = ShadokDialGeometry.hourHandDegrees(hour, minute)
                val minuteAngle = ShadokDialGeometry.minuteHandDegrees(minute)

                assertTrue(hourAngle in 0f..<360f, "$hour:$minute -> $hourAngle")
                assertTrue(minuteAngle in 0f..<360f, "$hour:$minute -> $minuteAngle")
            }
        }
    }
}
