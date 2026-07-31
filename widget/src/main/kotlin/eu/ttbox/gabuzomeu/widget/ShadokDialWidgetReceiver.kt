package eu.ttbox.gabuzomeu.widget

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Point d'entrée système du cadran **à quatre quartiers** : Ga, Bu, Zo, Meu.
 *
 * Un provider distinct des autres genres : les trois apparaissent séparément dans le sélecteur
 * de widgets. Ils partagent en revanche une seule alarme, portée par [ShadokClockTickReceiver].
 */
class ShadokDialWidgetReceiver : ShadokWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ShadokWidgets.quarterDial()
}
