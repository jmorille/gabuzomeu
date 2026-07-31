package eu.ttbox.gabuzomeu.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Le cycle de vie commun aux deux widgets d'horloge.
 *
 * Les deux genres — l'heure en glyphes et le cadran — réagissent exactement de la même façon
 * aux évènements système ; seul leur rendu diffère. Ce socle porte donc la logique, et chaque
 * sous-classe ne fournit que son [glanceAppWidget].
 *
 * Ce que le code d'origine ne faisait pas :
 * - il ne réagissait ni à `TIME_SET`, ni à `TIMEZONE_CHANGED`, ni à `BOOT_COMPLETED` : après
 *   un redémarrage, l'alarme posée dans `onEnabled` était perdue et le widget se figeait
 *   définitivement ;
 * - il prenait un `WakeLock` dans un `BroadcastReceiver`, avec le tag littéral `"YOUR TAG"`
 *   (`AlarmManagerBroadcastReceiver.java:20-21`), et réclamait donc la permission `WAKE_LOCK`.
 *   Inutile : le système garde déjà le processus éveillé pendant `onReceive`.
 */
abstract class ShadokWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Laisse Glance traiter APPWIDGET_UPDATE, LOCALE_CHANGED et ses propres actions.
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            -> {
                // Glance n'a pas rafraîchi pour ces actions : on le demande.
                redrawAsync(context)
                startTickingIfPlaced(context)
            }
        }
        // Aucune branche `else`, et c'est le correctif central de cette classe. Elle réarmait
        // l'alarme pour **n'importe quelle** action, y compris APPWIDGET_DISABLED :
        // `super.onReceive` appelait `onDisabled`, qui annulait, puis le `else` reposait
        // aussitôt l'alarme. Le dernier widget retiré, un réveil par minute survivait donc
        // pour toujours, en se réarmant à chaque tic et jusqu'à travers les redémarrages.
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ClockTickScheduler.scheduleNextMinute(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Le filet de sécurité : la mise à jour périodique déclarée par le widget passe par
        // ici, ce qui relance la chaîne de tics si elle avait été rompue. Sans cela, un seul
        // tic perdu figeait l'horloge définitivement.
        ClockTickScheduler.scheduleNextMinute(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // `onDisabled` ne vient qu'après le retrait de la dernière instance **de ce genre** ;
        // or l'alarme est partagée. On s'en remet donc au décompte global.
        stopTickingIfNonePlaced(context)
    }

    override fun onDisabled(context: Context) {
        stopTickingIfNonePlaced(context)
        super.onDisabled(context)
    }

    private fun startTickingIfPlaced(context: Context) {
        if (ClockTickScheduler.shouldKeepTicking(ShadokWidgets.placedCount(context))) {
            ClockTickScheduler.scheduleNextMinute(context)
        }
    }

    private fun stopTickingIfNonePlaced(context: Context) {
        if (!ClockTickScheduler.shouldKeepTicking(ShadokWidgets.placedCount(context))) {
            ClockTickScheduler.cancel(context)
        }
    }

    /** `goAsync` : le rendu Glance est suspendu, `onReceive` ne peut pas l'attendre. */
    private fun redrawAsync(context: Context) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                // `glanceAppWidget` et non une nouvelle instance : un seul objet par receiver.
                glanceAppWidget.updateAll(context)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    abstract override val glanceAppWidget: GlanceAppWidget
}
