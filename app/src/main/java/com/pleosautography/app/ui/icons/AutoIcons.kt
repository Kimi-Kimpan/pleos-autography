package com.pleosautography.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 시안(design/tokens.md, design/html)의 SVG 아이콘. 모두 viewBox 0 0 24 24, 검정으로 그리고
 * 쓰는 쪽에서 ColorFilter.tint로 색을 입힌다.
 */
object AutoIcons {
    val Play = icon("Play") { fill("M8 5l11 7-11 7z") }
    val Pause = icon("Pause") { fill("M6 5h4v14H6z M14 5h4v14h-4z") }
    val Next = icon("Next") { fill("M6 5l9 7-9 7z M16 5h2.6v14H16z") }
    val Plus = icon("Plus") { stroke("M12 5v14M5 12h14", 2.4f) }
    val Pencil = icon("Pencil") { stroke("M4 20h4l11-11-4-4L4 16z", 2.4f, join = StrokeJoin.Round) }
    val Document = icon("Document") { stroke("M6 3h9l4 4v14H6z M9 11h7 M9 15h7", 2f) }
    val ChevronUp = icon("ChevronUp") { stroke("M6 15l6-6 6 6", 2.6f) }
    val ChevronDown = icon("ChevronDown") { stroke("M6 9l6 6 6-6", 2.6f) }
    val ChevronLeft = icon("ChevronLeft") { stroke("M15 6l-6 6 6 6", 2.6f) }
    val Clip = icon("Clip") {
        stroke("M16.5 7v8.5a4.5 4.5 0 0 1-9 0V6a3 3 0 0 1 6 0v9.5a1.5 1.5 0 0 1-3 0V7.5", 2f, cap = StrokeCap.Round)
    }
    val Settings = icon("Settings") {
        stroke("M15.4 12a3.4 3.4 0 1 1-6.8 0a3.4 3.4 0 1 1 6.8 0z", 2f)
        stroke(
            "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1",
            2f,
            cap = StrokeCap.Round,
        )
    }
    /** 큰 마이크·이어서 하기 버튼용: 몸통을 채운 마이크. */
    val MicFilled = icon("MicFilled") {
        fill("M12 3a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z")
        stroke("M6 11a6 6 0 0 0 12 0M12 17v4", 2f)
    }
    val Note = icon("Note") {
        stroke("M9 18V6l11-2v12", 2f)
        fill("M4 18a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z M15 16a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z")
    }
    val Mic = icon("Mic") {
        stroke("M12 3a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z", 2f)
        stroke("M6 11a6 6 0 0 0 12 0M12 17v4", 2f)
    }
}

private fun icon(name: String, paths: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(paths).build()

private fun ImageVector.Builder.fill(pathData: String) {
    addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
}

private fun ImageVector.Builder.stroke(
    pathData: String,
    width: Float,
    cap: StrokeCap = StrokeCap.Butt,
    join: StrokeJoin = StrokeJoin.Miter,
) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = cap,
        strokeLineJoin = join,
    )
}
