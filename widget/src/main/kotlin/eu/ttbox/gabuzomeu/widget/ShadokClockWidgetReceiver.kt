package eu.ttbox.gabuzomeu.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Point d'entrée système du widget.
 *
 * Un seul receiver couvre tout le cycle de vie : mise à jour périodique, changement
 * d'heure ou de fuseau, redémarrage de l'appareil.
 *
 * Ce que le code d'origine ne faisait pas :
 * - il ne réagissait ni à `TIME_SET`, ni à `TIMEZONE_CHANGED`, ni à `BOOT_COMPLETED` :
 *   après un redémarrage, l'alarme posée dans `onEnabled` était perdue et le widget se
 *   figeait définitivement ;
 * - il prenait un `WakeLock` dans un `BroadcastReceiver`, avec le tag littéral
 *   `"YOUR TAG"` (`AlarmManagerBroadcastReceiver.java:20-21`), et réclamait donc la
 *   permission `WAKE_LOCK`. Inutile : le système garde déjà le processus éveillé pendant
 *   `onReceive`. La permission a disparu du manifeste.
 */
class ShadokClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ShadokClockWidget()

    override fun onReceive(context: Context, intent: Intent) {
        // Laisse Glance traiter APPWIDGET_UPDATE et les actions qu'il gère lui-même.
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            -> {
                // Glance n'a pas rafraîchi pour ces actions : on le demande.
                redrawAsync(context)
                ClockTickScheduler.scheduleNextMinute(context)
            }

            else -> {
                // Notamment après chaque tic : on repose l'alarme de la minute suivante.
                ClockTickScheduler.scheduleNextMinute(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ClockTickScheduler.scheduleNextMinute(context)
    }

    override fun onDisabled(context: Context) {
        // Plus aucune instance sur l'écran d'accueil : ne pas laisser tourner l'alarme.
        ClockTickScheduler.cancel(context)
        super.onDisabled(context)
    }

    /** `goAsync` : le rendu Glance est suspendu, `onReceive` ne peut pas l'attendre. */
    private fun redrawAsync(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                ShadokClockWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
