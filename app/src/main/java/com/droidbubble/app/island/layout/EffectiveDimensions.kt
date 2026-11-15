package com.droidbubble.app.island.layout

import com.droidbubble.app.model.IslandSettings
import kotlin.math.abs
import kotlin.math.max

/**
 * Where the pill really sits (V1.2), in dp: its top, its horizontal offset from the screen
 * centre, its height, and the width of the empty middle over the camera.
 */
data class EffectiveDimensions(val topDp: Float, val horizontalDp: Float, val heightDp: Float, val cameraZoneDp: Float)

/**
 * Camera calibration (V1.2). With "Fit to camera" on and a usable cutout, the pill is centred on
 * the lens, a ring of black around it, and the Dimensions sliders become small fine-tune deltas.
 * Otherwise the absolute settings apply exactly as before. Phones without a usable cutout, and
 * upgrades that haven't migrated yet, never move.
 */
object Calibration {
    /** Today's camera gap and pill height: the fallback with no usable cutout. */
    const val DEFAULT_ZONE_DP = 36f
    const val MIN_HEIGHT_DP = 36f
    /** Black around the lens: the pill is at least the lens plus this (6 dp each side). CP1 asks 12 or 16. */
    const val HEIGHT_RING_DP = 12f
    /** The camera gap is the lens plus this much each side (punch hole / waterdrop), at least [MIN_ZONE_DP]. */
    const val ZONE_RING_HOLE_DP = 6f
    const val ZONE_RING_DROP_DP = 4f
    const val MIN_ZONE_DP = 24f

    /** The fine-tune sliders' ranges, dp (±). */
    const val TOP_DELTA_RANGE = 12f
    const val HORIZONTAL_DELTA_RANGE = 24f
    const val HEIGHT_DELTA_RANGE = 8f

    /** The migration and the portrait rules need a usable camera on the top edge. */
    fun applies(cam: CameraGeometry): Boolean = cam.isUsable && cam.edge == CutoutEdge.TOP && cam.screenWidthDp > 0f

    /** V1.5: in landscape the camera is on a side edge; the pill stays on top, beside it. */
    fun appliesLandscape(cam: CameraGeometry): Boolean = cam.isUsable && cam.edge != CutoutEdge.TOP && cam.screenWidthDp > 0f

    /** Landscape: the pill's top, and the gap from the camera's inner edge to the pill's near end. */
    const val LANDSCAPE_TOP_DP = 6f
    const val LANDSCAPE_SIDE_GAP_DP = 8f
    /** Nothing physical sits under a landscape pill, so its middle gap is small. */
    const val LANDSCAPE_ZONE_DP = 8f

    /**
     * Landscape (V1.5): the pill's offset from the screen centre that puts its near end
     * [LANDSCAPE_SIDE_GAP_DP] from the camera's inner edge, on the camera's side.
     */
    fun landscapeHorizontal(cam: CameraGeometry, pillWidth: Float): Float = when (cam.edge) {
        CutoutEdge.LEFT -> (cam.centerXDp + cam.diameterDp / 2f + LANDSCAPE_SIDE_GAP_DP + pillWidth / 2f) - cam.screenWidthDp / 2f
        CutoutEdge.RIGHT -> (cam.centerXDp - cam.diameterDp / 2f - LANDSCAPE_SIDE_GAP_DP - pillWidth / 2f) - cam.screenWidthDp / 2f
        CutoutEdge.TOP -> autoHorizontal(cam)
    }

    fun cameraZone(cam: CameraGeometry): Float = when {
        !applies(cam) -> DEFAULT_ZONE_DP
        cam.kind == CutoutKind.WATERDROP -> max(cam.diameterDp + 2 * ZONE_RING_DROP_DP, MIN_ZONE_DP)
        else -> max(cam.diameterDp + 2 * ZONE_RING_HOLE_DP, MIN_ZONE_DP)
    }

    fun autoHeight(cam: CameraGeometry): Float = max(MIN_HEIGHT_DP, cam.diameterDp + HEIGHT_RING_DP)

    /** The top that centres a pill of [height] on the lens. */
    fun autoTop(cam: CameraGeometry, height: Float): Float = (cam.centerYDp - height / 2f).coerceAtLeast(0f)

    fun autoHorizontal(cam: CameraGeometry): Float = cam.centerXDp - cam.screenWidthDp / 2f

    /**
     * The one-time migration (V1.2), run when a usable camera first appears. A fresh install
     * ([hasStoredDims] false) starts centred on the lens. An existing install gets the deltas that
     * keep its pill exactly where it is; if one falls outside its slider range, calibration stays
     * off and the absolute values keep working. The absolute fields are never changed.
     */
    fun migrate(settings: IslandSettings, cam: CameraGeometry, hasStoredDims: Boolean): IslandSettings {
        if (settings.calibrationMigrated || !applies(cam)) return settings
        if (!hasStoredDims) {
            return settings.copy(autoCalibrate = true, topOffsetDeltaDp = 0f, horizontalOffsetDeltaDp = 0f, heightDeltaDp = 0f, calibrationMigrated = true)
        }
        val dH = settings.heightDp - autoHeight(cam)
        val dTop = settings.topOffsetDp - autoTop(cam, settings.heightDp.toFloat())
        val dX = settings.horizontalOffsetDp - autoHorizontal(cam)
        val inRange = abs(dTop) <= TOP_DELTA_RANGE && abs(dX) <= HORIZONTAL_DELTA_RANGE && abs(dH) <= HEIGHT_DELTA_RANGE
        return if (inRange) {
            settings.copy(autoCalibrate = true, topOffsetDeltaDp = dTop, horizontalOffsetDeltaDp = dX, heightDeltaDp = dH, calibrationMigrated = true)
        } else {
            settings.copy(autoCalibrate = false, calibrationMigrated = true)
        }
    }
}

/** The pill's real placement for this camera (V1.2): calibrated, or today's absolute values. */
fun IslandSettings.effective(cam: CameraGeometry): EffectiveDimensions {
    if (autoCalibrate && calibrationMigrated && Calibration.appliesLandscape(cam)) {
        // V1.5: on the top edge, beside the side camera; only the height fine-tune carries over
        return EffectiveDimensions(
            topDp = Calibration.LANDSCAPE_TOP_DP,
            horizontalDp = Calibration.landscapeHorizontal(cam, widthDp.toFloat()),
            heightDp = Calibration.autoHeight(cam) + heightDeltaDp,
            cameraZoneDp = Calibration.LANDSCAPE_ZONE_DP,
        )
    }
    if (autoCalibrate && calibrationMigrated && Calibration.applies(cam)) {
        val height = Calibration.autoHeight(cam) + heightDeltaDp
        return EffectiveDimensions(
            topDp = Calibration.autoTop(cam, height) + topOffsetDeltaDp,
            horizontalDp = Calibration.autoHorizontal(cam) + horizontalOffsetDeltaDp,
            heightDp = height,
            cameraZoneDp = Calibration.cameraZone(cam),
        )
    }
    return EffectiveDimensions(topOffsetDp.toFloat(), horizontalOffsetDp.toFloat(), heightDp.toFloat(), Calibration.DEFAULT_ZONE_DP)
}
