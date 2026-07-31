package eu.ttbox.gabuzomeu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Bleu de l'icône de lanceur, base du thème de repli. */
private val ShadokBlue = Color(0xFF1D3557)
private val ShadokBlueLight = Color(0xFF4A6D97)
private val ShadokSand = Color(0xFFE8C547)

private val FallbackLight = lightColorScheme(
    primary = ShadokBlue,
    secondary = ShadokBlueLight,
    tertiary = ShadokSand,
)

private val FallbackDark = darkColorScheme(
    primary = ShadokBlueLight,
    secondary = ShadokBlue,
    tertiary = ShadokSand,
)

/**
 * Thème Material 3 de l'application.
 *
 * Les couleurs dynamiques (Material You) sont utilisées par défaut. Aucun test de
 * version n'est nécessaire : elles existent depuis l'API 31, qui est notre `minSdk` —
 * une conséquence directe du choix de plancher, là où le projet d'origine était figé sur
 * le thème Holo de l'API 16.
 *
 * @param dynamicColor à `false`, on retombe sur la palette de marque dérivée du bleu de
 *   l'icône. Utile aux captures d'écran et aux tests, qui ont besoin d'un rendu stable.
 */
@Composable
fun GabuzomeuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GabuzomeuTypography,
        content = content,
    )
}
