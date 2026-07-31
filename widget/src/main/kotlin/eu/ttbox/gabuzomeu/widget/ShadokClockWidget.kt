package eu.ttbox.gabuzomeu.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import eu.ttbox.gabuzomeu.core.shadok.ShadokClock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

/**
 * L'horloge Shadok sur l'écran d'accueil.
 *
 * Ne dépend que de `:core:shadok` : le widget n'affiche que les **noms** (Ga/Bu/Zo/Meu),
 * jamais les glyphes, donc il n'a besoin ni des dessins vectoriels ni d'une police.
 * C'était déjà le choix du code d'origine (`ClockWidgetProvider.updateRemoveViews`).
 */
class ShadokClockWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // L'activité de lanceur est résolue dynamiquement : cela évite au module :widget
        // de dépendre de :app, ce qui créerait un cycle.
        val launchComponent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.component

        // Locale, heure et date sont lues ICI, hors du composable — provideGlance est
        // rappelé à chaque mise à jour, donc à chaque minute.
        //
        // On passe par la configuration du Context et non par Locale.getDefault() : cela
        // respecte la langue choisie pour cette application seule (android:localeConfig).
        // L'original figeait les noms de jours et de mois dans des initialiseurs
        // statiques (ClockWidgetProvider.java:22-24), donc au chargement de la classe :
        // un changement de langue ne se voyait qu'après redémarrage du processus.
        val locale = context.resources.configuration.locales[0]
        val time = LocalTime.now()
        val date = LocalDate.now()

        provideContent {
            GlanceTheme {
                ClockContent(
                    time = time,
                    date = date,
                    locale = locale,
                    launchComponent = launchComponent,
                )
            }
        }
    }
}

@Composable
private fun ClockContent(
    time: LocalTime,
    date: LocalDate,
    locale: Locale,
    launchComponent: ComponentName?,
) {
    val base = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(16.dp)
        .padding(all = 12.dp)

    Column(
        modifier = if (launchComponent == null) {
            base
        } else {
            base.clickable(
                actionStartActivity(launchComponent),
            )
        },
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = ShadokClock.format(time.hour, time.minute),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = date.dayOfWeek
                .getDisplayName(JavaTextStyle.FULL, locale)
                .uppercase(locale),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = DATE_PATTERN.withLocale(locale).format(date),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp,
            ),
        )
    }
}

/** Jour, mois en clair, année — comme le widget d'origine, secondes en moins. */
private val DATE_PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
