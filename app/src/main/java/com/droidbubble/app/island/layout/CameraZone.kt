package com.droidbubble.app.island.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.model.IslandSettings
import kotlin.math.roundToInt

/**
 * The width of the pill's empty middle over the camera (V1.3). The overlay service provides the
 * measured value; anywhere else (previews, tests) it is today's 36 dp.
 */
val LocalCameraZone = staticCompositionLocalOf<Dp> { IslandTokens.Shell.CameraZone }

/** The empty middle of a pill, over the camera lens. Nothing is ever drawn here. */
@Composable
fun CameraGap() {
    Spacer(modifier = Modifier.width(LocalCameraZone.current))
}

/**
 * The settings the island draws with (V1.3): top, height and horizontal offset replaced by their
 * calibrated values, rounded to whole dp (the island's fields are whole dp; a migrated pill's
 * values are already whole, so it doesn't move). Unchanged when calibration doesn't apply.
 */
fun IslandSettings.withCalibration(cam: CameraGeometry): IslandSettings {
    if (!(autoCalibrate && calibrationMigrated && (Calibration.applies(cam) || Calibration.appliesLandscape(cam)))) return this
    val e = effective(cam)
    return copy(
        topOffsetDp = e.topDp.roundToInt(),
        heightDp = e.heightDp.roundToInt(),
        horizontalOffsetDp = e.horizontalDp.roundToInt(),
    )
}
