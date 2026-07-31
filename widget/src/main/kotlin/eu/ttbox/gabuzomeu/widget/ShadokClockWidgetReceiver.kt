package eu.ttbox.gabuzomeu.widget

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Point d'entrée système du widget **numérique** : l'heure en glyphes Shadok.
 *
 * Tout le cycle de vie est dans [ShadokWidgetReceiver], partagé avec le cadran.
 */
class ShadokClockWidgetReceiver : ShadokWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ShadokClockWidget()
}
