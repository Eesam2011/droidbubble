package com.droidbubble.app.ui.main

import com.droidbubble.app.island.layout.Calibration
import com.droidbubble.app.island.layout.CameraGeometry
import com.droidbubble.app.island.layout.CameraSide
import com.droidbubble.app.island.layout.CutoutKind
import com.droidbubble.app.island.layout.effective
import com.droidbubble.app.model.IslandSettings
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The Dimensions page's position sliders (V1.4). With "Fit to camera" on and a usable camera they
 * edit small fine-tune deltas around the lens; otherwise the absolute values, as before.
 */
object DimensionBindings {

    enum class Field { HEIGHT, TOP, HORIZONTAL }

    data class Slider(val label: String, val value: Float, val range: ClosedFloatingPointRange<Float>, val signed: Boolean)

    /** True when the sliders are fine-tune deltas. */
    fun calibrating(s: IslandSettings, cam: CameraGeometry) = s.autoCalibrate && s.calibrationMigrated && Calibration.applies(cam)

    fun slider(field: Field, s: IslandSettings, cam: CameraGeometry): Slider = if (calibrating(s, cam)) {
        when (field) {
            Field.HEIGHT -> Slider("Height fine-tune", s.heightDeltaDp, -Calibration.HEIGHT_DELTA_RANGE..Calibration.HEIGHT_DELTA_RANGE, true)
            Field.TOP -> Slider("Vertical fine-tune", s.topOffsetDeltaDp, -Calibration.TOP_DELTA_RANGE..Calibration.TOP_DELTA_RANGE, true)
            Field.HORIZONTAL -> Slider("Horizontal fine-tune", s.horizontalOffsetDeltaDp, -Calibration.HORIZONTAL_DELTA_RANGE..Calibration.HORIZONTAL_DELTA_RANGE, true)
        }
    } else {
        when (field) {
            Field.HEIGHT -> Slider("Height", s.heightDp.toFloat(), 12f..56f, false)
            Field.TOP -> Slider("Top Offset (Notch Line-up)", s.topOffsetDp.toFloat(), 0f..60f, false)
            Field.HORIZONTAL -> Slider("Horizontal Offset", s.horizontalOffsetDp.toFloat(), -50f..50f, false)
        }
    }

    /** Writes a slider value: whole dp, into the delta field when calibrating, else the absolute one. */
    fun apply(field: Field, s: IslandSettings, cam: CameraGeometry, value: Float): IslandSettings {
        val v = value.roundToInt()
        return if (calibrating(s, cam)) {
            when (field) {
                Field.HEIGHT -> s.copy(heightDeltaDp = v.toFloat())
                Field.TOP -> s.copy(topOffsetDeltaDp = v.toFloat())
                Field.HORIZONTAL -> s.copy(horizontalOffsetDeltaDp = v.toFloat())
            }
        } else {
            when (field) {
                Field.HEIGHT -> s.copy(heightDp = v)
                Field.TOP -> s.copy(topOffsetDp = v)
                Field.HORIZONTAL -> s.copy(horizontalOffsetDp = v)
            }
        }
    }

    /**
     * Turns "Fit to camera" on or off without moving the pill: on, the deltas are set to hold the
     * current position; off, the current calibrated position becomes the absolute values (to the
     * nearest dp). With no usable camera only the switch changes.
     */
    fun setFitToCamera(s: IslandSettings, on: Boolean, cam: CameraGeometry): IslandSettings {
        if (!Calibration.applies(cam)) return s.copy(autoCalibrate = on)
        return if (on) {
            if (calibrating(s, cam)) return s
            val dH = s.heightDp - Calibration.autoHeight(cam)
            s.copy(
                autoCalibrate = true,
                calibrationMigrated = true,
                heightDeltaDp = dH,
                topOffsetDeltaDp = s.topOffsetDp - Calibration.autoTop(cam, s.heightDp.toFloat()),
                horizontalOffsetDeltaDp = s.horizontalOffsetDp - Calibration.autoHorizontal(cam),
            )
        } else {
            val e = s.effective(cam)
            s.copy(
                autoCalibrate = false,
                topOffsetDp = e.topDp.roundToInt(),
                heightDp = e.heightDp.roundToInt(),
                horizontalOffsetDp = e.horizontalDp.roundToInt(),
            )
        }
    }

    /** "+2 dp", "−3 dp", "0 dp" for deltas; "36 dp" otherwise. */
    fun valueText(value: Float, signed: Boolean): String {
        val n = value.roundToInt()
        return when {
            !signed || n == 0 -> "$n dp"
            n > 0 -> "+$n dp"
            else -> "−${-n} dp"
        }
    }

    /** What TalkBack reads for the value: "plus 2 dp". */
    fun spokenValue(value: Float, signed: Boolean): String {
        val n = value.roundToInt()
        return when {
            !signed || n == 0 -> "$n dp"
            n > 0 -> "plus $n dp"
            else -> "minus ${-n} dp"
        }
    }

    /** The switch's supporting line: what was detected. */
    fun detectedText(cam: CameraGeometry): String {
        if (!Calibration.applies(cam)) return "No camera cutout found, so manual position is used"
        val kind = if (cam.kind == CutoutKind.WATERDROP) "Waterdrop" else "Punch-hole"
        val side = when (cam.side) {
            CameraSide.CENTER -> "centre"
            CameraSide.START -> "left"
            CameraSide.END -> "right"
        }
        return String.format(Locale.US, "%s camera, %d dp, %s", kind, cam.diameterDp.roundToInt(), side)
    }
}
