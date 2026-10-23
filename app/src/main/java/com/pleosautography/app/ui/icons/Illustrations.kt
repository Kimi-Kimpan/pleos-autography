package com.pleosautography.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** 시안의 색이 있는 그림. 색을 그대로 쓰므로 tint하지 않는다. */
object Illustrations {
    /** ‘Driving Together’ 표지의 1톤 화물차(design/html/FullDrive.html의 SVG). */
    val Truck: ImageVector = ImageVector.Builder(
        name = "Truck",
        defaultWidth = 160.dp,
        defaultHeight = 56.dp,
        viewportWidth = 160f,
        viewportHeight = 56f,
    ).apply {
        part("M58 6h96v34H58z", 0xFFC49A6C)
        part("M58 38h98v6H58z", 0xFFD9DBDC)
        part("M6 44V22q2-10 12-10h30q8 0 8 8v24z", 0xFFECEAE4)
        part("M14 26q2-8 10-8h20q4 0 4 4v6H14z", 0xFF3E4B63)
        part("M18 46a8 8 0 1 0 16 0a8 8 0 1 0-16 0z", 0xFF17181B)
        part("M122 46a8 8 0 1 0 16 0a8 8 0 1 0-16 0z", 0xFF17181B)
    }.build()
}

private fun ImageVector.Builder.part(pathData: String, color: Long) {
    addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color(color)))
}
