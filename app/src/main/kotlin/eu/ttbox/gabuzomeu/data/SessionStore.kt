package eu.ttbox.gabuzomeu.data

import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import kotlinx.coroutines.flow.Flow

/** La dernière session, telle que restaurée au lancement. */
data class StoredSession(
    val keys: String = "",
    val notation: NumberNotation = NumberNotation.DECIMAL,
)

/**
 * Persistance de la session.
 *
 * L'interface existe pour que [eu.ttbox.gabuzomeu.ui.calculator.CalculatorViewModel] ne
 * dépende pas d'un `Context` : ses tests tournent ainsi sur la JVM, sans Robolectric ni
 * émulateur.
 */
interface SessionStore {
    val session: Flow<StoredSession>
    suspend fun save(keys: String, notation: NumberNotation)
}
