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

    fun scheduleNextMinute(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // set() remplace toute alarme déjà posée avec le même PendingIntent : la
        // replanification ne peut pas s'empiler.
        alarmManager.set(AlarmManager.RTC, nextMinuteBoundaryMillis(), tickIntent(context))
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(tickIntent(context))
    }

    private fun nextMinuteBoundaryMillis(): Long = Instant.now()
        .atZone(ZoneId.systemDefault())
        .truncatedTo(ChronoUnit.MINUTES)
        .plusMinutes(1)
        .toInstant()
        .toEpochMilli()

    /**
     * Une diffusion `APPWIDGET_UPDATE` vers notre propre receiver : Glance prend alors
     * le relais et rappelle `provideGlance`, qui relit l'heure.
     */
    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            component = ComponentName(context, ShadokClockWidgetReceiver::class.java)
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
