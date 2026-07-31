package eu.ttbox.gabuzomeu.data

import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import kotlinx.coroutines.flow.Flow

/** La dernière session, telle que restaurée au lancement. */
data class StoredSession(
    val keys: String = "",
    val notation: NumberNotation = NumberNotation.DECIMAL,
)

/**
 * Ce que l'utilisateur choisit d'afficher.
 *
 * La ligne de **glyphes** Shadok n'est pas configurable : c'est l'identité de
 * l'application et la seule écriture toujours présente. Les deux autres lignes — les
 * noms prononcés et la traduction décimale — se masquent à volonté.
 */
data class DisplaySettings(val showShadokLabels: Boolean = true, val showDecimal: Boolean = true)

/**
 * Persistance de la session et des préférences d'affichage.
 *
 * L'interface existe pour que [eu.ttbox.gabuzomeu.ui.calculator.CalculatorViewModel] ne
 * dépende pas d'un `Context` : ses tests tournent ainsi sur la JVM, sans Robolectric ni
 * émulateur.
 */
interface SessionStore {
    val session: Flow<StoredSession>
    val settings: Flow<DisplaySettings>

    suspend fun save(keys: String, notation: NumberNotation)
    suspend fun saveSettings(settings: DisplaySettings)
}
