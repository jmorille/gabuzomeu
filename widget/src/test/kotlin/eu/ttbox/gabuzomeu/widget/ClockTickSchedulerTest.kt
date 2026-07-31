package eu.ttbox.gabuzomeu.widget

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * La cadence du widget d'horloge.
 *
 * Le module `:widget` n'avait aucun test : tout y dépend d'Android, sauf ce calcul. Il valait
 * pourtant d'être couvert, parce que c'est exactement là que le code d'origine se trompait —
 * `ClockWidgetProvider.java:54-55` demandait un `setRepeating` d'une seconde, une cadence que
 * l'API 19 ramène à 60 s et rend inexacte : la demande n'a jamais été honorée.
 */
class ClockTickSchedulerTest {

    private val utc = ZoneId.of("UTC")

    private fun nextTick(instant: String, zone: ZoneId = utc): Instant = Instant.ofEpochMilli(
        ClockTickScheduler.nextMinuteBoundaryMillis(Instant.parse(instant), zone),
    )

    @Test
    fun `au milieu d'une minute, le reveil est a la minute suivante`() {
        assertEquals(Instant.parse("2026-07-31T14:36:00Z"), nextTick("2026-07-31T14:35:27Z"))
    }

    @Test
    fun `sur une minute pile, le reveil avance d'une minute entiere`() {
        // Le cas qui compte : rendre l'instant courant reposterait une alarme déjà échue,
        // délivrée aussitôt — le widget se réveillerait en boucle au lieu d'une fois/minute.
        assertEquals(Instant.parse("2026-07-31T14:36:00Z"), nextTick("2026-07-31T14:35:00Z"))
    }

    @Test
    fun `une seule nanoseconde apres la minute suffit a viser la suivante`() {
        assertEquals(
            Instant.parse("2026-07-31T14:36:00Z"),
            nextTick("2026-07-31T14:35:00.000000001Z"),
        )
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "2026-07-31T14:35:27.123456789Z",
            "2026-07-31T14:35:00Z",
            "2026-07-31T14:35:59.999999999Z",
            "2026-07-31T23:59:59Z",
            "2026-12-31T23:59:30Z",
            "2026-02-28T23:59:01Z",
            "1970-01-01T00:00:00Z",
        ],
    )
    fun `le reveil est toujours strictement dans le futur, et a moins d'une minute`(
        instant: String,
    ) {
        val now = Instant.parse(instant)
        val tick = nextTick(instant)

        assertTrue(tick.isAfter(now), "$tick doit suivre $now")
        assertTrue(
            tick <= now.plus(1, ChronoUnit.MINUTES),
            "$tick ne doit pas dépasser $now d'une minute",
        )
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "2026-07-31T14:35:27Z",
            "2026-07-31T14:35:00Z",
            "2026-12-31T23:59:30Z",
        ],
    )
    fun `le reveil tombe pile sur une frontiere de minute`(instant: String) {
        // Ni seconde, ni nanoseconde résiduelle : c'est ce qui aligne l'affichage sur
        // le changement de minute réel plutôt qu'à un décalage arbitraire.
        val tick = nextTick(instant)

        assertEquals(0, tick.atZone(utc).second)
        assertEquals(0, tick.atZone(utc).nano)
        assertEquals(tick, tick.truncatedTo(ChronoUnit.MINUTES))
    }

    @Test
    fun `le passage d'heure, de jour et d'annee est correct`() {
        assertEquals(Instant.parse("2026-07-31T15:00:00Z"), nextTick("2026-07-31T14:59:30Z"))
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), nextTick("2026-07-31T23:59:30Z"))
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), nextTick("2026-12-31T23:59:30Z"))
        // 2026 n'est pas bissextile : le 28 février est suivi du 1er mars.
        assertEquals(Instant.parse("2026-03-01T00:00:00Z"), nextTick("2026-02-28T23:59:30Z"))
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "UTC",
            "Europe/Paris",
            // Décalage à la demi-heure : reste un nombre entier de minutes.
            "Asia/Kolkata",
            // Décalage au quart d'heure.
            "Asia/Kathmandu",
            "Pacific/Kiritimati",
        ],
    )
    fun `le fuseau ne change pas l'instant du reveil`(zoneId: String) {
        // Tous les fuseaux actuels sont décalés d'un nombre entier de minutes par rapport à
        // UTC : tronquer à la minute donne donc le même instant partout. Autrement dit, lire
        // ZoneId.systemDefault() est sans conséquence ici — mais le paramètre reste explicite
        // plutôt que caché dans un appel au système.
        val instant = "2026-07-31T14:35:27Z"

        assertEquals(nextTick(instant, utc), nextTick(instant, ZoneId.of(zoneId)))
    }

    @ParameterizedTest(name = "{0} widget(s)")
    @ValueSource(ints = [1, 2, 7])
    fun `le tic continue tant qu'un widget est pose`(widgetCount: Int) {
        assertTrue(ClockTickScheduler.shouldKeepTicking(widgetCount))
    }

    @Test
    fun `sans widget pose, le tic doit s'arreter`() {
        // La condition d'arrêt auto-réparante. Elle compte parce que le bug précédent était
        // exactement l'inverse : la branche `else` d'onReceive réarmait l'alarme pour toute
        // action, y compris APPWIDGET_DISABLED — juste après qu'onDisabled l'ait annulée. Un
        // réveil par minute survivait donc au retrait du dernier widget, indéfiniment.
        assertFalse(ClockTickScheduler.shouldKeepTicking(0))
    }

    @Test
    fun `l'action du tic est privee, et n'est pas APPWIDGET_UPDATE`() {
        // Le second bug : un Intent(ACTION_APPWIDGET_UPDATE) sans EXTRA_APPWIDGET_IDS est
        // ignoré par AppWidgetProvider.onReceive, qui n'appelle onUpdate que si l'extra porte
        // au moins un identifiant. Le tic coûtait un réveil par minute sans jamais redessiner
        // l'horloge. Une action à nous ne peut pas être confondue avec celle du système.
        assertEquals(
            "eu.ttbox.gabuzomeu.widget.action.TICK",
            ClockTickScheduler.ACTION_TICK,
        )
        // Le littéral, plutôt que la constante d'AppWidgetManager : ce test tourne sur la JVM,
        // où l'android.jar de test est un stub.
        assertNotEquals(
            "android.appwidget.action.APPWIDGET_UPDATE",
            ClockTickScheduler.ACTION_TICK,
        )
    }

    @Test
    fun `un changement d'heure d'ete ne decale pas la minute`() {
        // À Paris, le 29 mars 2026 à 02:00 locale, l'horloge saute à 03:00. Le décalage est
        // d'une heure entière : la frontière de minute, elle, ne bouge pas.
        val paris = ZoneId.of("Europe/Paris")
        val justBeforeTransition = "2026-03-29T00:59:30Z"

        assertEquals(
            Instant.parse("2026-03-29T01:00:00Z"),
            nextTick(justBeforeTransition, paris),
        )
    }
}
