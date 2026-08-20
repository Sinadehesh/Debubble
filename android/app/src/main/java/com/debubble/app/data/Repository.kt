package com.debubble.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debubble.app.engine.AuditCatalogue
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.CampaignPool
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.LearnCurriculum
import com.debubble.app.engine.RemediationPool
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

    /** The Systems Audit catalogue and its remediation pools. */
    fun loadAudit(): AuditCatalogue = readAsset("audit/debuffs.json") { raw ->
        val cat = json.decodeFromString<AuditCatalogue>(raw)
        require(cat.debuffs.isNotEmpty()) { "the audit catalogue is empty" }
        require(cat.categories.isNotEmpty()) { "the audit has no categories" }
        val orphans = cat.debuffs.map { it.category }.toSet() - cat.categories.map { it.id }.toSet()
        require(orphans.isEmpty()) { "debuffs in unknown categories: $orphans" }
        cat
    }

    fun loadRemediation(catalogue: AuditCatalogue): RemediationPool =
        readAsset("audit/remediation.json") { raw ->
            val pool = json.decodeFromString<RemediationPool>(raw)
            // Every debuff must have somewhere to go, or ticking it does nothing at all.
            catalogue.debuffs.forEach { d ->
                require(pool.forDebuff(d.id).isNotEmpty()) { "${d.id} has no remediation" }
            }
            pool
        }

    /** Campaign-specific and bridge challenges. */
    fun loadCampaignPool(): CampaignPool = readAsset("campaigns/pool.json") { raw ->
        val pool = json.decodeFromString<CampaignPool>(raw)
        require(pool.challenges.isNotEmpty()) { "the campaign pool is empty" }
        // Every campaign needs free material, or a user on the free tier gets a dead tab.
        Goal.all.forEach { g ->
            val free = pool.forActive(setOf(g.name), BudgetTier.FREE)
            require(free.isNotEmpty()) { "${g.name} has nothing on the free tier" }
        }
        pool
    }

    /** The Learn curriculum: thirty course days plus the audit-triggered articles. */
    fun loadLearn(): LearnCurriculum = readAsset("learn/curriculum.json") { raw ->
        val c = json.decodeFromString<LearnCurriculum>(raw)
        val days = c.course.map { it.day }.toSet()
        require(days == (1..30).toSet()) { "the Learn course has gaps: ${(1..30).toSet() - days}" }
        c
    }

    private fun <T> readAsset(path: String, parse: (String) -> T): T {
        val raw = context.assets.open(path).bufferedReader().use { it.readText() }
        return parse(raw)
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
