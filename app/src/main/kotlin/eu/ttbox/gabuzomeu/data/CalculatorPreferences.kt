package eu.ttbox.gabuzomeu.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calculatorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calculator",
)

/**
 * Implémentation de [SessionStore] sur DataStore Preferences.
 *
 * Remplace `Persist.java` du projet d'origine : un format binaire maison écrit avec
 * `DataOutputStream` dans `calculator.data`, dont les contrôles de version étaient
 * inversés (`Persist.java:55-59` — la branche « version trop récente » était
 * inatteignable) et dont toutes les exceptions étaient avalées par un `log()` lui-même
 * désactivé (`Calculator.LOG_ENABLED = false`).
 *
 * L'historique des calculs n'est pas conservé : la fonctionnalité était déjà morte dans
 * le projet d'origine, `HistoryAdapter` n'étant branché sur aucune `ListView`.
 */
class CalculatorPreferences(context: Context) : SessionStore {

    private val dataStore = context.calculatorDataStore

    override val session: Flow<StoredSession> = dataStore.data.map { preferences ->
        StoredSession(
            keys = preferences[KEY_EXPRESSION].orEmpty(),
            notation = preferences[KEY_NOTATION]
                ?.let { stored -> NumberNotation.entries.firstOrNull { it.name == stored } }
                ?: NumberNotation.DECIMAL,
        )
    }

    override val settings: Flow<DisplaySettings> = dataStore.data.map { preferences ->
        val defaults = DisplaySettings()
        DisplaySettings(
            showShadokLabels = preferences[KEY_SHOW_LABELS] ?: defaults.showShadokLabels,
            showDecimal = preferences[KEY_SHOW_DECIMAL] ?: defaults.showDecimal,
        )
    }

    override suspend fun save(keys: String, notation: NumberNotation) {
        dataStore.edit { preferences ->
            preferences[KEY_EXPRESSION] = keys
            preferences[KEY_NOTATION] = notation.name
        }
    }

    override suspend fun saveSettings(settings: DisplaySettings) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_LABELS] = settings.showShadokLabels
            preferences[KEY_SHOW_DECIMAL] = settings.showDecimal
        }
    }

    private companion object {
        val KEY_EXPRESSION = stringPreferencesKey("expression")
        val KEY_NOTATION = stringPreferencesKey("notation")
        val KEY_SHOW_LABELS = booleanPreferencesKey("show-shadok-labels")
        val KEY_SHOW_DECIMAL = booleanPreferencesKey("show-decimal")
    }
}
