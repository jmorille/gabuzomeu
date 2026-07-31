package eu.ttbox.gabuzomeu.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Réveille le widget au prochain changement de minute.
 *
 * Trois problèmes du code d'origine sont corrigés ici.
 *
 * **1. Cadence.** `ClockWidgetProvider.java:54-55` demandait `setRepeating(RTC_WAKEUP,
 * …, 1000, …)`, soit une seconde. Depuis l'API 19, un `setRepeating` est ramené à 60 s
 * minimum **et** rendu inexact : cette demande n'a jamais été honorée. La résolution à
 * la seconde n'est plus possible pour un widget sur Android moderne, par conception ;
 * on se cale donc sur la minute et l'affichage des secondes est retiré.
 *
 * **2. PendingIntent.** L'original utilisait `PendingIntent.getBroadcast(…, 0)`
 * (`ClockWidgetProvider.java:38-39,52`). Depuis l'API 31, omettre `FLAG_IMMUTABLE` ou
 * `FLAG_MUTABLE` lève une `IllegalArgumentException` — le widget **plantait** sur tout
 * Android 12 ou plus récent.
 *
 * **3. Permission.** On replanifie avec `set()`, une alarme *inexacte* : aucune
 * permission `SCHEDULE_EXACT_ALARM` n'est requise. Un décalage de quelques secondes sur
 * l'affichage d'une horloge Shadok est sans conséquence.
 */
object ClockTickScheduler {

    /** L'action du tic de minute, interne à l'application. */
    internal const val ACTION_TICK: String = "eu.ttbox.gabuzomeu.widget.action.TICK"

    fun scheduleNextMinute(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // set() remplace toute alarme déjà posée avec le même PendingIntent : la
        // replanification ne peut pas s'empiler.
        alarmManager.set(
            AlarmManager.RTC,
            nextMinuteBoundaryMillis(Instant.now()),
            tickIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(tickIntent(context))
    }

    /**
     * L'instant du prochain changement de minute, strictement après [now].
     *
     * `now` est un paramètre et non un `Instant.now()` interne : c'est le seul calcul de ce
     * fichier qui ne touche pas à Android, et le sortir ainsi le rend vérifiable sur la JVM
     * — sans Robolectric ni émulateur. Le reste (AlarmManager, PendingIntent) n'est que du
     * câblage plateforme.
     *
     * `plusMinutes(1)` **après** troncature, et jamais l'inverse : sur une heure pile, rendre
     * `now` reposterait une alarme déjà échue, que le système délivrerait immédiatement — le
     * widget se réveillerait alors en boucle au lieu d'une fois par minute.
     */
    internal fun nextMinuteBoundaryMillis(
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = now
        .atZone(zone)
        .truncatedTo(ChronoUnit.MINUTES)
        .plusMinutes(1)
        .toInstant()
        .toEpochMilli()

    /**
     * `true` tant qu'au moins une instance du widget est posée sur un écran d'accueil.
     *
     * Le garde-fou qui rend la planification **auto-réparante** : sans widget affiché, un
     * tic ne sert plus à rien, et l'alarme doit s'éteindre d'elle-même. C'est ce qui empêche
     * une alarme orpheline de survivre indéfiniment si un `cancel()` a été manqué.
     */
    internal fun shouldKeepTicking(widgetCount: Int): Boolean = widgetCount > 0

    /**
     * Une action **privée**, et non `APPWIDGET_UPDATE`.
     *
     * C'était le bug central : un `Intent(ACTION_APPWIDGET_UPDATE)` sans
     * `EXTRA_APPWIDGET_IDS` est ignoré par `AppWidgetProvider.onReceive`, qui n'appelle
     * `onUpdate` que si l'extra contient au moins un identifiant. Le tic réveillait donc le
     * processus chaque minute **sans jamais redessiner l'horloge** : tout le coût, aucun
     * effet. Une action à nous, traitée explicitement, rend le chemin sans ambiguïté.
     */
    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(ACTION_TICK).apply {
            // Un receiver dédié, partagé par les deux widgets : voir ShadokClockTickReceiver.
            component = ComponentName(context, ShadokClockTickReceiver::class.java)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            // FLAG_IMMUTABLE : obligatoire depuis l'API 31.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val REQUEST_CODE = 1
}
