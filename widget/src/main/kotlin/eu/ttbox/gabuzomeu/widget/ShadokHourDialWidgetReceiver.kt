package eu.ttbox.gabuzomeu.widget

import androidx.glance.appwidget.GlanceAppWidget

/**
 * Point d'entrée système du cadran **aux douze heures**, écrites en glyphes Shadok.
 *
 * L'horloge la plus classique des trois par sa forme, et la plus dépaysante par son écriture :
 * midi s'y lit `MeuGa`, puisque 12 vaut 30 en base 4.
 */
class ShadokHourDialWidgetReceiver : ShadokWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ShadokWidgets.hourDial()
}
