package com.pleosautography.app.debug

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pleosautography.app.AppGraph
import com.pleosautography.app.domain.DriveMode
import kotlin.math.roundToInt

/**
 * 디버그 빌드 전용: 시연 영상용 ‘내비’ 대체 화면.
 * 에뮬레이터의 Pleos 내비 지도가 그려지지 않아서(타일 없음), 직접 그린 지도 그림으로 대신한다.
 * - 지도 레이어(841×1810dp)를 창 너비에 맞춰 그리고, 그 위에 고정 UI 오버레이(841×1210dp)를 덮는다.
 * - 주행 중에는 레이어 오프셋을 600dp → 0으로 초당 약 15dp씩 옮긴다(지도가 아래로 흘러 차가 앞으로 가는 모습). 0에 닿으면 멈춘다.
 * - 정차 중에는 그 자리에 멈춘다. 주행/정차는 앱의 주행 상태(데모 이벤트 drive/park 포함)를 따른다.
 * - 터치는 받지 않는다. 그림(assets/nav)은 저장소에 넣지 않는다(video-kit에서 복사).
 */
class NavStandInActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = AppGraph.get(this)
        val layer = loadAsset("nav/nav_map_layer.png")
        val overlay = loadAsset("nav/nav_ui_overlay.png")
        setContent {
            val driveMode by graph.driveMode.collectAsState()
            val offset = remember { mutableFloatStateOf(offsetDp) }
            // 주행 중에만 오프셋을 줄인다. 화면을 다시 만들어도 이어지게 같은 값을 쓴다.
            LaunchedEffect(driveMode) {
                if (driveMode != DriveMode.DRIVING) return@LaunchedEffect
                var last = withFrameNanos { it }
                while (offset.floatValue > 0f) {
                    val now = withFrameNanos { it }
                    offset.floatValue = (offset.floatValue - SPEED_DP_PER_SEC * (now - last) / 1e9f).coerceAtLeast(0f)
                    offsetDp = offset.floatValue
                    last = now
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFFE9EBEE))
                // 그림 1px = 0.5dp(@2x). 창 너비(약 841dp)에 맞춰 늘이거나 줄인다.
                val scale = size.width / ASSET_WIDTH_PX
                layer?.let {
                    val top = -offset.floatValue * ASSET_PX_PER_DP * scale
                    drawImage(
                        it,
                        dstOffset = IntOffset(0, top.roundToInt()),
                        dstSize = IntSize(size.width.roundToInt(), (it.height * scale).roundToInt()),
                    )
                }
                overlay?.let {
                    drawImage(it, dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.roundToInt(), (it.height * scale).roundToInt()))
                }
                if (layer == null) drawCircle(Color(0xFF1E6FE8), radius = 18f * scale * ASSET_PX_PER_DP, center = Offset(size.width * 0.66f, size.height * 0.64f))
            }
        }
    }

    private fun loadAsset(path: String): ImageBitmap? =
        runCatching { assets.open(path).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }) } }
            .getOrNull()?.asImageBitmap()

    companion object {
        private const val ASSET_WIDTH_PX = 1682f
        private const val ASSET_PX_PER_DP = 2f
        private const val START_OFFSET_DP = 600f
        private const val SPEED_DP_PER_SEC = 15f

        /** 지금 레이어 오프셋(dp). 앱 프로세스가 살아 있는 동안 이어진다(demo.sh --reset이 프로세스를 지우면 600dp부터). */
        @Volatile private var offsetDp = START_OFFSET_DP
    }
}
