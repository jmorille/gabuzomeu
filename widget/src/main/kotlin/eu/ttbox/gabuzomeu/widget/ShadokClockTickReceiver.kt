package eu.ttbox.gabuzomeu.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Le propriétaire du tic de minute, pour les deux widgets à la fois.
 *
 * Un receiver dédié plutôt que le tic porté par l'un des deux providers : avec une alarme par
 * genre de widget, en poser un de chaque doublait les réveils, et faire porter l'alarme
 * partagée par l'un des deux créait une dépendance absurde — le cadran aurait cessé d'avancer
 * le jour où l'on retire le widget numérique.
 *
 * Ce n'est **pas** un `AppWidgetProvider` : il ne reçoit qu'une action privée, par intention
 * explicite, et n'a donc besoin d'aucun filtre dans le manifeste.
 */
class ShadokClockTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Rien d'autre ne nous concerne. Un receiver qui agirait sur une action inattendue est
        // exactement ce qui ressuscitait l'alarme dans la version précédente.
        if (intent.action != ClockTickScheduler.ACTION_TICK) return

        if (!ClockTickScheduler.shouldKeepTicking(ShadokWidgets.placedCount(context))) {
            // Plus aucun widget : l'alarme s'éteint d'elle-même. C'est le garde-fou qui rend
            // impossible une alarme orpheline survivant indéfiniment.
            ClockTickScheduler.cancel(context)
            return
        }

        redrawAsync(context)
        ClockTickScheduler.scheduleNextMinute(context)
    }

    /** `goAsync` : le rendu Glance est suspendu, `onReceive` ne peut pas l'attendre. */
    private fun redrawAsync(context: Context) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                // Tous les genres sont rafraîchis, énumérés par le registre. `updateAll` ne fait
                // rien pour un genre absent de l'écran d'accueil : inutile de le vérifier ici.
                ShadokWidgets.all().forEach { widget -> widget.updateAll(context) }
            } finally {
                pendingResult.finish()
                // Le scope ne survit pas à la diffusion : sans cela son Job resterait vivant.
                scope.cancel()
            }
        }
    }
}
