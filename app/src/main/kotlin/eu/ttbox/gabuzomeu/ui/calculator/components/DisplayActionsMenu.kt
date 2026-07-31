package eu.ttbox.gabuzomeu.ui.calculator.components

import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.ClipboardNumber
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.ui.ActionTags
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorUiState
import eu.ttbox.gabuzomeu.ui.calculator.StackLevel
import eu.ttbox.gabuzomeu.ui.shadok.ShadokGlyphText
import kotlinx.coroutines.launch

/** Une des quatre écritures d'une valeur, telle qu'on peut la copier. */
enum class ValueWriting {
    GLYPHS,
    LABELS,
    BASE4,
    DECIMAL,
}

/**
 * Une valeur affichée, dans ses quatre écritures.
 *
 * Le même type sert à X et à un niveau de pile : ce qui se copie ne dépend pas de l'endroit
 * d'où on le copie.
 */
data class CopyableValue(
    val glyphs: String,
    val labels: String,
    val base4: String,
    val decimal: String,
) {

    fun textFor(writing: ValueWriting): String = when (writing) {
        ValueWriting.GLYPHS -> glyphs
        ValueWriting.LABELS -> labels
        ValueWriting.BASE4 -> base4
        ValueWriting.DECIMAL -> decimal
    }

    /** Rien à copier : l'afficheur est vide. Coller, en revanche, reste possible. */
    val isEmpty: Boolean get() = decimal.isEmpty() && glyphs.isEmpty()

    companion object {
        fun of(state: CalculatorUiState): CopyableValue = CopyableValue(
            glyphs = state.glyphs,
            labels = state.labels,
            base4 = state.base4,
            decimal = state.decimal,
        )

        fun of(level: StackLevel): CopyableValue = CopyableValue(
            glyphs = level.glyphs,
            labels = level.labels,
            base4 = level.base4,
            decimal = level.decimal,
        )
    }
}

/**
 * Rend [content] copiable : un appui — long ou simple — ouvre le menu des actions.
 *
 * L'appui long est le geste attendu pour copier ; le simple appui ouvre le même menu parce
 * qu'un afficheur en lecture seule n'a rien d'autre à faire d'un tapotement, et que ça rend
 * la fonctionnalité trouvable sans la deviner.
 *
 * `onLongClickLabel` n'est pas décoratif : c'est lui qui fait annoncer l'action par TalkBack
 * et la rend atteignable autrement qu'au doigt. La ligne de glyphes appliquant
 * `clearAndSetSemantics`, le geste se pose ici, sur l'ancêtre — jamais sur elle.
 *
 * @param onPaste `null` là où coller n'a pas de sens : on ne colle pas dans un niveau de pile
 *   enfoui, alors l'item disparaît au lieu d'être grisé.
 */
@Composable
fun DisplayActions(
    value: CopyableValue,
    notation: NumberNotation,
    tag: String,
    modifier: Modifier = Modifier,
    onPaste: ((String) -> Unit)? = null,
    onCopied: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .testTag(tag)
            .combinedClickable(
                onClick = { expanded = true },
                onLongClick = { expanded = true },
                onLongClickLabel = stringResource(R.string.display_actions),
            ),
    ) {
        content()
        ActionsMenu(
            expanded = expanded,
            value = value,
            notation = notation,
            onPaste = onPaste,
            onCopied = onCopied,
            onDismiss = { expanded = false },
        )
    }
}

@Composable
private fun ActionsMenu(
    expanded: Boolean,
    value: CopyableValue,
    notation: NumberNotation,
    onPaste: ((String) -> Unit)?,
    onCopied: () -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        CopyItems(value, onDismiss, onCopied)

        HorizontalDivider()

        // Absent, et non grisé, là où coller n'a pas de sens : on n'écrit pas au milieu
        // d'une pile.
        if (onPaste != null) PasteItem(expanded, notation, onPaste, onDismiss)

        ShareItem(value, onDismiss)
    }
}

/** Les quatre écritures, chacune avec l'aperçu de ce qu'elle copiera. */
@Composable
private fun CopyItems(value: CopyableValue, onDismiss: () -> Unit, onCopied: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipLabel = stringResource(R.string.app_name)

    ValueWriting.entries.forEach { writing ->
        val text = value.textFor(writing)
        DropdownMenuItem(
            text = { Text(stringResource(writing.labelRes())) },
            trailingIcon = { WritingPreview(writing, text) },
            enabled = text.isNotEmpty(),
            onClick = {
                onDismiss()
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, text)))
                    // Android 13 affiche lui-même une confirmation de copie : en ajouter une
                    // ferait doublon sur la grande majorité des appareils.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) onCopied()
                }
            },
            modifier = Modifier.testTag(ActionTags.copy(writing)),
        )
    }
}

/**
 * Coller — actif seulement si le presse-papiers contient un nombre.
 *
 * Le contenu est relu à chaque ouverture du menu, et non au clic : c'est ce qui permet de
 * griser l'item. Un texte illisible ne peut donc jamais atteindre le ViewModel, et il n'y a
 * pas d'échec silencieux à expliquer une fois le doigt levé.
 */
@Composable
private fun PasteItem(
    expanded: Boolean,
    notation: NumberNotation,
    onPaste: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    var pastable by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(expanded, notation) {
        val text = if (expanded) clipboard.plainTextOrNull() else null
        pastable = text?.takeIf { ClipboardNumber.parseOrNull(it, notation) != null }
    }

    val pasted = pastable
    DropdownMenuItem(
        text = { Text(stringResource(R.string.paste)) },
        enabled = pasted != null,
        onClick = {
            onDismiss()
            if (pasted != null) onPaste(pasted)
        },
        modifier = Modifier.testTag(ActionTags.PASTE),
    )
}

/** Partager — les **quatre** écritures d'un coup, là où copier n'en donne qu'une. */
@Composable
private fun ShareItem(value: CopyableValue, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.share_title)
    val shared = stringResource(
        R.string.share_value,
        value.decimal,
        value.labels,
        value.glyphs,
        value.base4,
    )

    DropdownMenuItem(
        text = { Text(stringResource(R.string.share)) },
        enabled = !value.isEmpty,
        onClick = {
            onDismiss()
            context.startActivity(Intent.createChooser(shareIntent(shared), shareTitle))
        },
        modifier = Modifier.testTag(ActionTags.SHARE),
    )
}

/**
 * L'aperçu de ce que l'item copiera — le menu s'explique ainsi tout seul, et « base 4 »
 * cesse d'être une abstraction.
 *
 * Les glyphes passent par [ShadokGlyphText] et non par un `Text` : `⅃` (U+2143) n'est pas
 * garanti dans les polices système, et l'aperçu s'afficherait en tofu là où l'afficheur, lui,
 * dessine ses vecteurs. C'est aussi la raison d'être de l'écriture en base 4 : elle, elle se
 * colle partout.
 */
@Composable
private fun WritingPreview(writing: ValueWriting, text: String) {
    val style = MaterialTheme.typography.labelLarge
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val modifier = Modifier.widthIn(max = PREVIEW_MAX_WIDTH)

    if (writing == ValueWriting.GLYPHS) {
        ShadokGlyphText(
            expression = text,
            // L'item porte déjà son libellé : l'aperçu n'a rien à annoncer de plus.
            semanticsLabel = "",
            style = style,
            color = color,
            operatorColor = MaterialTheme.colorScheme.tertiary,
            modifier = modifier,
        )
    } else {
        Text(text = text, style = style, color = color, maxLines = 1, modifier = modifier)
    }
}

private fun ValueWriting.labelRes(): Int = when (this) {
    ValueWriting.GLYPHS -> R.string.copy_glyphs
    ValueWriting.LABELS -> R.string.copy_labels
    ValueWriting.BASE4 -> R.string.copy_base4
    ValueWriting.DECIMAL -> R.string.copy_decimal
}

private fun shareIntent(text: String): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, text)
}

/**
 * Le presse-papiers en texte brut, ou `null`.
 *
 * `Clipboard` n'offre aucun raccourci texte — son unique extension est `firstUriOrNull` —
 * d'où le passage par `ClipData`. Un item sans texte (une image, un URI seul) rend `null`, ce
 * qui grise « Coller » : exactement ce qu'on veut, sans avoir à distinguer les types.
 */
private suspend fun Clipboard.plainTextOrNull(): String? {
    val data = getClipEntry()?.clipData ?: return null
    if (data.itemCount == 0) return null
    return data.getItemAt(0).text?.toString()
}

private val PREVIEW_MAX_WIDTH = 140.dp
