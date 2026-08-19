package com.debubble.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Ladder
import com.debubble.app.engine.Pillar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "debubble")

/**
 * Single source of truth. Reads the authored curriculum out of assets once, and streams the
 * user's state out of DataStore.
 */
class Repository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("state_v1")

    val state: Flow<AppState> = context.dataStore.data.map { prefs ->
        prefs[stateKey]
            ?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() }
            ?: AppState()
    }

    suspend fun update(transform: (AppState) -> AppState) {
        context.dataStore.edit { prefs ->
            val current = prefs[stateKey]
                ?.let { runCatching { json.decodeFromString<AppState>(it) }.getOrNull() }
                ?: AppState()
            prefs[stateKey] = json.encodeToString(transform(current))
        }
    }

    /**
     * Loads all three ladders. Throws if an asset is missing or malformed — a corrupt
     * curriculum is not something to paper over with an empty list, because every screen
     * downstream assumes 100 real tiers per pillar.
     */
    fun loadCurriculum(): Curriculum {
        val ladders = Pillar.order.associateWith { pillar ->
            val raw = context.assets.open(pillar.asset).bufferedReader().use { it.readText() }
            val ladder = json.decodeFromString<Ladder>(raw)
            require(ladder.tiers.size == 100) {
                "${pillar.asset} has ${ladder.tiers.size} tiers, expected 100"
            }
            ladder
        }
        return Curriculum(ladders)
    }

    /**
     * Loads the five goal campaigns. Same contract as the curriculum: a malformed campaign is
     * a hard failure, because every screen downstream assumes a full thirty steps.
     */
    fun loadGoals(): Map<Goal, GoalTrack> = Goal.all.associateWith { goal ->
        val raw = context.assets.open(goal.asset).bufferedReader().use { it.readText() }
        val track = json.decodeFromString<GoalTrack>(raw)
        require(track.missions.size == Goals.CAMPAIGN_LENGTH) {
            "${goal.asset} has ${track.missions.size} missions, expected ${Goals.CAMPAIGN_LENGTH}"
        }
        require(track.reps.isNotEmpty()) { "${goal.asset} has no rep types" }
        track
    }
}
