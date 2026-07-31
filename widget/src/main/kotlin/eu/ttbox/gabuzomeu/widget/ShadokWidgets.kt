package eu.ttbox.gabuzomeu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget

/**
 * Le registre des widgets de l'application : la source unique.
 *
 * Trois genres coexistent — l'heure en glyphes, le cadran à quatre quartiers, le cadran aux
 * douze heures — et ils partagent **une seule alarme**. Deux conséquences, toutes deux servies
 * ici :
 *
 * - le décompte porte sur les trois ensemble, car le tic doit vivre tant qu'au moins un widget,
 *   de n'importe quel genre, est posé, et s'éteindre dès qu'il n'en reste aucun ;
 * - le tic doit rafraîchir les trois. Les énumérer à un seul endroit évite d'en oublier un le
 *   jour où un quatrième s'ajoute — c'est exactement le genre d'oubli qui fige une horloge.
 */
internal object ShadokWidgets {

    private val receivers = listOf(
        ShadokClockWidgetReceiver::class.java,
        ShadokDialWidgetReceiver::class.java,
        ShadokHourDialWidgetReceiver::class.java,
    )

    /** Le cadran des quarts : Ga, Bu, Zo, Meu aux quatre points cardinaux. */
    fun quarterDial(): ShadokDialWidget = ShadokDialWidget(
        markers = ShadokDialGeometry.quarterMarkers,
        markerBoxRatio = ShadokDialRenderer.QUARTER_MARKER_BOX_RATIO,
    )

    /** Le cadran d'horloge classique, ses douze heures écrites en base 4. */
    fun hourDial(): ShadokDialWidget = ShadokDialWidget(
        markers = ShadokDialGeometry.hourMarkers,
        markerBoxRatio = ShadokDialRenderer.HOUR_MARKER_BOX_RATIO,
    )

    /** Tous les genres, pour un rafraîchissement complet au tic de minute. */
    fun all(): List<GlanceAppWidget> = listOf(
        ShadokClockWidget(),
        quarterDial(),
        hourDial(),
    )

    /** Le nombre d'instances posées, tous genres confondus. */
    fun placedCount(context: Context): Int {
        val manager = AppWidgetManager.getInstance(context) ?: return 0
        return receivers.sumOf { receiver ->
            manager.getAppWidgetIds(ComponentName(context, receiver))?.size ?: 0
        }
    }
}
