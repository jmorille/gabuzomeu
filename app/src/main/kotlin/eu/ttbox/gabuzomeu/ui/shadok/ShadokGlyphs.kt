package eu.ttbox.gabuzomeu.ui.shadok

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit

/**
 * Les quatre glyphes Shadok, dessinés en vectoriel.
 *
 * Le projet d'origine chargeait `assets/dejavu_serif.ttf` — et le rechargeait **à chaque
 * appel** de `GabuzomeuConverter.getSymbolFont()` — pour afficher quatre caractères
 * Unicode : ◯ U+25EF, _ U+005F, ⅃ U+2143, ◿ U+25FF.
 *
 * Les redessiner ici évite d'embarquer 328 Ko de police pour quatre signes, supprime la
 * nécessité de distribuer le texte de la licence DejaVu, et garantit surtout un rendu
 * **identique sur tous les appareils** : la couverture de U+2143 par les polices système
 * n'est pas acquise. Les tracés sont ceux de l'icône de lanceur, ramenés à un carré de
 * 24×24.
 *
 * Le rendu passe par `Icon`, qui applique un `ColorFilter.tint` : la couleur déclarée
 * dans les tracés est donc sans importance, seule la silhouette compte.
 */
object ShadokGlyphs {

    private const val VIEWPORT = 24f

    private val vectors: Map<ShadokDigit, ImageVector> by lazy {
        ShadokDigit.entries.associateWith { digit ->
            when (digit) {
                ShadokDigit.GA -> ga()
                ShadokDigit.BU -> bu()
                ShadokDigit.ZO -> zo()
                ShadokDigit.MEU -> meu()
            }
        }
    }

    fun of(digit: ShadokDigit): ImageVector = vectors.getValue(digit)

    /** ◯ — un cercle évidé, tracé en deux demi-arcs. */
    private fun ga(): ImageVector = builder("Ga").apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f) {
            moveTo(3f, 12f)
            arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, 18f, 0f)
            arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, -18f, 0f)
        }
    }.build()

    /** _ — une barre basse. */
    private fun bu(): ImageVector = builder("Bu").apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(2f, 16f)
            horizontalLineToRelative(20f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(-20f)
            close()
        }
    }.build()

    /** ⅃ — un L inversé : hampe à droite, pied vers la gauche. */
    private fun zo(): ImageVector = builder("Zo").apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17f, 4f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(16f)
            horizontalLineToRelative(-16f)
            verticalLineToRelative(-3f)
            horizontalLineToRelative(13f)
            close()
        }
    }.build()

    /** ◿ — un triangle plein, angle droit en bas à droite. */
    private fun meu(): ImageVector = builder("Meu").apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 4f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
        }
    }.build()

    private fun builder(name: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    )
}
