package eu.ttbox.gabuzomeu.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.unit.ColorProvider

/**
 * Les couleurs effectives d'un widget, arrière-plan compris.
 *
 * Toutes résolues en [Color] concrètes plutôt qu'en `ColorProvider` : appliquer une opacité
 * demande d'accéder au canal alpha, ce qu'un fournisseur de couleur ne permet pas.
 */
internal data class WidgetPalette(
    val background: Color,
    val onSurface: Color,
    val variant: Color,
    val accent: Color,
) {
    val onSurfaceProvider: ColorProvider get() = ColorProvider(onSurface)
    val variantProvider: ColorProvider get() = ColorProvider(variant)
    val accentProvider: ColorProvider get() = ColorProvider(accent)
}

/**
 * Le calcul des couleurs, séparé de leur usage.
 *
 * Les deux fonctions publiques sont pures et se vérifient sur la JVM. C'est délibéré : une
 * erreur d'opacité ou d'inversion clair/sombre est invisible à la relecture, et la constater
 * demanderait de reposer le widget à chaque combinaison de réglages.
 */
internal object WidgetBackground {

    private const val OPAQUE_ALPHA = 255
    private const val PERCENT = 100

    /**
     * Le thème réellement appliqué.
     *
     * [WidgetBackgroundTheme.SYSTEM] s'en remet à l'appareil ; les deux autres l'ignorent, ce
     * qui est tout l'intérêt du réglage — un fond d'écran clair sous un téléphone en thème
     * sombre rendait les glyphes illisibles.
     */
    fun isDark(theme: WidgetBackgroundTheme, systemIsDark: Boolean): Boolean = when (theme) {
        WidgetBackgroundTheme.SYSTEM -> systemIsDark
        WidgetBackgroundTheme.LIGHT -> false
        WidgetBackgroundTheme.DARK -> true
    }

    /**
     * Applique une opacité en pourcentage au canal alpha d'une couleur ARGB.
     *
     * Seul l'alpha change : les composantes rouge, verte et bleue sont conservées telles quelles,
     * de sorte qu'un fond à 40 % reste la même teinte, simplement plus discrète.
     */
    fun applyOpacity(argb: Int, opacityPercent: Int): Int {
        val clamped = opacityPercent.coerceIn(
            WidgetBackgroundOptions.FULLY_TRANSPARENT,
            WidgetBackgroundOptions.FULLY_OPAQUE,
        )
        val alpha = OPAQUE_ALPHA * clamped / PERCENT
        return (argb and RGB_MASK) or (alpha shl ALPHA_SHIFT)
    }

    private const val RGB_MASK = 0x00FFFFFF
    private const val ALPHA_SHIFT = 24

    /** `true` si l'appareil est en thème sombre. */
    fun systemIsDark(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }
}

/**
 * La palette à utiliser, d'après les réglages d'arrière-plan.
 *
 * En mode « paramètre du téléphone », on garde [GlanceTheme] — donc les couleurs **dynamiques**
 * de l'appareil, celles qui s'accordent au fond d'écran. Forcer clair ou sombre impose au
 * contraire une palette fixe : les couleurs dynamiques n'existent pas en deux variantes
 * simultanément, et il faut bien choisir des teintes.
 */
@Composable
internal fun widgetPalette(options: WidgetBackgroundOptions): WidgetPalette {
    val context = LocalContext.current
    val dark = WidgetBackground.isDark(options.theme, WidgetBackground.systemIsDark(context))

    val base = if (options.theme == WidgetBackgroundTheme.SYSTEM) {
        WidgetPalette(
            background = GlanceTheme.colors.widgetBackground.getColor(context),
            onSurface = GlanceTheme.colors.onSurface.getColor(context),
            variant = GlanceTheme.colors.onSurfaceVariant.getColor(context),
            accent = GlanceTheme.colors.primary.getColor(context),
        )
    } else {
        WidgetPalette(
            background = context.color(
                if (dark) R.color.widget_surface_dark else R.color.widget_surface_light,
            ),
            onSurface = context.color(
                if (dark) R.color.widget_on_surface_dark else R.color.widget_on_surface_light,
            ),
            variant = context.color(
                if (dark) R.color.widget_variant_dark else R.color.widget_variant_light,
            ),
            accent = context.color(
                if (dark) R.color.widget_accent_dark else R.color.widget_accent_light,
            ),
        )
    }

    return base.copy(
        background = Color(
            WidgetBackground.applyOpacity(
                base.background.toArgb(),
                options.effectiveOpacityPercent,
            ),
        ),
    )
}

private fun Context.color(id: Int): Color = Color(ContextCompat.getColor(this, id))
