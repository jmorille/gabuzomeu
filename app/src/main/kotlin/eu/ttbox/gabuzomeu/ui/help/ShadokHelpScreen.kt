package eu.ttbox.gabuzomeu.ui.help

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.shadok.Rational
import eu.ttbox.gabuzomeu.core.shadok.ShadokConverter
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation
import eu.ttbox.gabuzomeu.ui.HelpTags
import eu.ttbox.gabuzomeu.ui.shadok.ShadokGlyphs

/**
 * « Comprendre les Shadoks » — la leçon, dans l'application.
 *
 * Elle manquait : rien n'expliquait comment lire `◿⅃`, alors que `docs/shadok-reference.md`
 * contient tout depuis le début — hors de l'application, donc invisible à qui l'installe.
 *
 * Le principe de cet écran : **ne rien écrire à la main de ce que le moteur sait calculer.**
 * La table des entiers est produite par [ShadokConverter], si bien qu'elle ne peut pas mentir
 * sur ce que la calculatrice fera. C'est la discipline déjà appliquée aux miniatures des
 * widgets, engendrées depuis les mêmes tracés que le rendu.
 */
// `TopAppBar` reste marqué expérimental dans Material 3, comme la classe de taille de fenêtre
// dont MainActivity dépend déjà. L'alternative serait de réimplémenter à la main la hauteur, les
// encarts système et la sémantique d'une barre de titre : plus de code, et moins juste.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadokHelpScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(HelpTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag(HelpTags.CLOSE),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.help_close),
                        )
                    }
                },
            )
        },
    ) { insets ->
        HelpContent(modifier = Modifier.padding(insets))
    }
}

/** La leçon elle-même, défilante. Séparée de l'échafaudage : chaque bloc reste lisible. */
@Composable
private fun HelpContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.help_quote),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.help_radix),
            style = MaterialTheme.typography.bodyMedium,
        )

        Section(R.string.help_digits_title)
        ShadokDigit.entries.forEach { DigitRow(it) }

        Section(R.string.help_reading_title)
        Text(
            text = stringResource(R.string.help_reading_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        Section(R.string.help_table_title)
        IntegerTable()

        Section(R.string.help_fractions_title)
        Text(
            text = stringResource(R.string.help_fractions_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        Section(R.string.help_clock_title)
        Text(
            text = stringResource(R.string.help_clock_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        Section(R.string.help_source_title)
        SourceLink()
    }
}

@Composable
private fun Section(titleRes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Un chiffre : son dessin, son nom, sa valeur, son chiffre en base 4. */
@Composable
private fun DigitRow(digit: ShadokDigit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ShadokGlyphs.of(digit),
            // La ligne entière s'annonce par le nom : le dessin n'a rien à ajouter.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(GLYPH_SIZE),
        )
        Text(
            text = digit.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(LABEL_WIDTH),
        )
        Text(
            text = stringResource(R.string.help_digit_value, digit.value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Les premiers entiers, **calculés** et non recopiés.
 *
 * Une table écrite à la main finirait par contredire le moteur ; celle-ci ne peut pas. Elle
 * n'a d'ailleurs aucune chaîne à traduire — des chiffres et des noms propres.
 */
@Composable
private fun IntegerTable() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Sans en-tête, les deux colonnes de chiffres — « 6 » et « 12 » — se lisent comme un
        // seul nombre, ou comme une erreur.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCell(stringResource(R.string.mode_decimal), CELL_DECIMAL)
            HeaderCell(stringResource(R.string.help_table_base4), CELL_BASE4)
            HeaderCell(stringResource(R.string.help_table_name), width = null)
        }

        for (value in 0..TABLE_LAST) {
            val base4 = ShadokConverter.toBase4(Rational.of(value))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TableCell("$value", CELL_DECIMAL)
                TableCell(ShadokFormatter.format(base4, ShadokNotation.BASE4), CELL_BASE4)
                Text(
                    text = ShadokFormatter.format(base4, ShadokNotation.LABELS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** L'intitulé d'une colonne. La dernière n'a pas de largeur : elle prend ce qui reste. */
@Composable
private fun HeaderCell(text: String, width: Dp?) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (width == null) Modifier else Modifier.width(width),
    )
}

/** Une colonne de largeur fixe : sans elle, rien ne s'aligne d'une ligne à l'autre. */
@Composable
private fun TableCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        // Chiffre à chasse fixe : c'est ce qui fait tenir les colonnes.
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(width),
    )
}

/**
 * Le lien vers la leçon d'origine.
 *
 * Un lien sortant plutôt qu'une illustration embarquée : il ne pèse rien et pointe vers la
 * chaîne de l'ayant droit, qui diffuse la vidéo lui-même. Le nom de la chaîne est visible sur
 * le bouton — personne ne doit quitter l'application sans le savoir.
 */
@Composable
private fun SourceLink() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.help_source_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_VIDEO_URL.toUri()))
            },
            modifier = Modifier.testTag(HelpTags.VIDEO),
        ) {
            Text(stringResource(R.string.help_source_link))
        }
    }
}

/** « Comment compter comme les Shadoks ? », chaîne Archive INA. */
private const val SOURCE_VIDEO_URL = "https://www.youtube.com/watch?v=lP9PaDs2xgQ"

/** Jusqu'à 16 : le premier nombre à trois chiffres en base 4, donc la table est complète. */
private const val TABLE_LAST = 16

private val GLYPH_SIZE = 28.dp
private val LABEL_WIDTH = 48.dp
private val CELL_DECIMAL = 36.dp
private val CELL_BASE4 = 56.dp
