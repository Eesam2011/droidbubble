package com.droidbubble.app.service

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.layout.CameraGeometry
import com.droidbubble.app.island.layout.CutoutKind

/**
 * V1.3, debug builds: a 1 dp red ring where the camera was measured, drawn in the island window,
 * so a photo of the phone shows whether the pill sits on the lens. Switched from the Test panel.
 */
@Composable
internal fun CameraOutlineDebug(camera: CameraGeometry) {
    val show by IslandStateManager.showCameraOutline.collectAsState()
    if (!show || camera.kind == CutoutKind.NONE) return
    // The window isn't at the screen origin: place the ring in screen coordinates
    var origin by remember { mutableStateOf(Offset.Zero) }
    Canvas(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionOnScreen() }) {
        val centre = Offset(camera.centerXDp.dp.toPx(), camera.centerYDp.dp.toPx()) - origin
        drawCircle(Color.Red, radius = camera.diameterDp.dp.toPx() / 2f, center = centre, style = Stroke(1.dp.toPx()))
    }
}
