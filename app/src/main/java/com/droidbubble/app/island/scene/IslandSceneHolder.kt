package com.droidbubble.app.island.scene

import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The island scene as one flow (F1.5). It only emits when what the island shows changes, so a
 * media position tick, a timer second or a new album art bitmap of the same song don't reach the
 * window at all. Replaces the service's hand-written five-way `combine` and its `shapeKey` list.
 *
 * Main-thread confined, like [IslandStateManager].
 */
class IslandSceneHolder(
    scope: CoroutineScope,
    inputs: Flow<IslandInputs>,
    settings: StateFlow<IslandSettings>,
    env: StateFlow<IslandEnv>,
    initialInputs: IslandInputs,
) {
    val scene: StateFlow<IslandScene> =
        combine(inputs, settings, env) { i, s, e -> deriveIslandScene(i, s, e) }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, deriveIslandScene(initialInputs, settings.value, env.value))

    companion object {
        /** Every state flow the scene is derived from, as one flow of inputs. */
        fun inputsFromStateManager(): Flow<IslandInputs> {
            val m = IslandStateManager
            val sources: List<Flow<*>> = listOf(
                m.isExpanded, m.expandedPriority, m.isPopInActive, m.activeMedia, m.activeNotification,
                m.activeProgress, m.screenRecordingState, m.activeCharging, m.isUnlocked,
                m.isBluetoothConnected, m.connectionAlert, m.activeRingerMode, m.isFlashlightOn,
                m.activeCallState, m.foregroundPackage, m.userHidden, m.userMinimized,
                m.isDeviceLocked, m.flashlightSolo,
            )
            return combine(sources) { Unit }.map { m.snapshotInputs() }
        }

        fun fromStateManager(scope: CoroutineScope, settings: StateFlow<IslandSettings>, env: StateFlow<IslandEnv>) =
            IslandSceneHolder(scope, inputsFromStateManager(), settings, env, IslandStateManager.snapshotInputs())
    }
}
