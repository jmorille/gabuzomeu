package eu.ttbox.gabuzomeu.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Les couleurs du pavé Simple, d'après l'affiche de la machine à calculer des Shadoks.
 *
 * **Ce pavé seul.** Les pavés Classique et NPI gardent les rôles Material du thème, couleur
 * dynamique comprise : une palette figée n'a de sens que là où elle porte une intention, et
 * ici elle en porte une — donner à chaque chiffre une identité, ce qui aide à les retenir.
 *
 * Deux précautions tirées de l'affiche elle-même :
 *
 * - **la teinte est sur le texte, pas sur le fond.** Les touches y sont blanches et les
 *   lettres colorées. C'est aussi ce qui expose le moins au risque de contraste : le fond
 *   reste celui du thème, et une seule couleur est à contrôler par touche ;
 * - **le C emprunte le rouge de Material** (`error`), qui existe déjà dans les deux thèmes
 *   et jusqu'en couleur dynamique, et qui dit « destructif » avant d'être lu. Le rouge
 *   **plein** et non `errorContainer` : à l'essai, le conteneur pâle était indiscernable du
 *   `tertiaryContainer` de la touche ⌫ voisine, et la touche qui efface tout se confondait
 *   avec celle qui efface un chiffre.
 *
 * Les teintes sombres ne sont pas les claires éclaircies au hasard : ce sont les mêmes
 * tons, remontés en luminosité jusqu'à repasser le seuil de 4.5:1 sur le fond de touche
 * sombre.
 *
 * La touche de calcul, elle, n'est **pas** ici : elle garde les rôles Material du thème,
 * exactement comme dans les pavés Classique et NPI. Un vert de marque lui avait été donné,
 * d'après l'affiche ; il est parti avec l'idée qu'un même geste doit garder la même
 * apparence d'un mode à l'autre.
 */
enum class KeyPalette {
    GA,
    BU,
    ZO,
    MEU,

    /** Effacer tout — le rouge, parce que c'est la touche qui détruit. */
    CLEAR,
    ;

    /**
     * Le couple conteneur / contenu de cette touche.
     *
     * Les deux sont toujours déclarés ensemble : ne fixer que le conteneur laisserait le
     * contenu sur le rôle par défaut d'un *autre* couple, et donc un symbole quasi illisible
     * sur son fond — la mésaventure de la rangée de fonctions en thème sombre.
     */
    @Composable
    fun colors(): ButtonColors {
        val dark = MaterialTheme.colorScheme.isDark
        return when (this) {
            CLEAR -> ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )

            // Les chiffres : fond du thème, teinte sur le glyphe et son nom.
            else -> ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = digitColor(dark),
            )
        }
    }

    private fun digitColor(dark: Boolean): Color = when (this) {
        GA -> if (dark) GaDark else GaLight
        BU -> if (dark) BuDark else BuLight
        ZO -> if (dark) ZoDark else ZoLight
        MEU -> if (dark) MeuDark else MeuLight
        CLEAR -> Color.Unspecified
    }
}

/**
 * Le thème actif est-il sombre — lu sur **la palette**, et non sur le système.
 *
 * `isSystemInDarkTheme()` répondait à côté : [GabuzomeuTheme] prend son `darkTheme` en
 * paramètre, si bien qu'un thème sombre demandé sur un appareil en clair recevait les teintes
 * claires sur des touches sombres. C'est exactement ce que montrait la première capture en
 * thème sombre. La luminance du fond, elle, ne peut pas se contredire — et elle vaut aussi en
 * couleur dynamique, où personne n'a écrit la palette.
 */
private val ColorScheme.isDark: Boolean get() = surface.luminance() < MID_LUMINANCE

private const val MID_LUMINANCE = 0.5f

private val GaLight = Color(0xFFB4530A)
private val GaDark = Color(0xFFFFB77C)

private val BuLight = Color(0xFF1B5FB8)
private val BuDark = Color(0xFF9FC6FF)

private val ZoLight = Color(0xFF14713E)
private val ZoDark = Color(0xFF7FDCA4)

private val MeuLight = Color(0xFF6B34AC)
private val MeuDark = Color(0xFFD3B4FF)
