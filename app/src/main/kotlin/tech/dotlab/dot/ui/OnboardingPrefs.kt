package tech.dotlab.dot.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "dot_onboarding")

/** Tracks whether the intro flow has been shown, so it appears only on first launch. */
class OnboardingPrefs(private val context: Context) {

    suspend fun hasSeen(): Boolean = context.onboardingDataStore.data.first()[SEEN] ?: false

    suspend fun markSeen() {
        context.onboardingDataStore.edit { it[SEEN] = true }
    }

    private companion object {
        val SEEN = booleanPreferencesKey("seen")
    }
}
