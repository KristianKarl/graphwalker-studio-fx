package org.graphwalker.model

import javafx.scene.paint.Color
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import org.slf4j.LoggerFactory
import tornadofx.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ArrowHead(edgeFX: EdgeFX, arrowHeadSize: Double = defaultArrowHeadSize) : Path() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        strokeProperty().bind(fillProperty())
        fill = Color.BLACK

        var lastPathElement = edgeFX.path.elements.last() as LineTo

        // Line
        logger.debug("Arrow head for edge  " + edgeFX.jsonEdge.edge.name)
        logger.debug("Move to " + lastPathElement)
        elements.add(MoveTo(lastPathElement.x, lastPathElement.y))

        logger.debug("Center point " + edgeFX.targetFX.getCenterPoint())

        var angle = atan2(edgeFX.targetFX.getCenterPoint().y - lastPathElement.y,
                edgeFX.targetFX.getCenterPoint().x - lastPathElement.x) - Math.PI / 2.0
        logger.debug("Angle $angle")

        var sin = sin(angle)
        var cos = cos(angle)
        val x = lastPathElement.x - (-1.0 / 2.0 * cos + sqrt(3.0) / 2 * sin) * arrowHeadSize
        val y = lastPathElement.y - (-1.0 / 2.0 * sin - sqrt(3.0) / 2 * cos) * arrowHeadSize
        logger.debug("Line to $x, $y")

        elements.add(LineTo(x, y))

        // ArrowHead
        angle = atan2(y - lastPathElement.y, x - lastPathElement.x) - Math.PI / 2.0
        sin = sin(angle)
        cos = cos(angle)

        // point1
        val x1 = (-1.0 / 2.0 * cos + sqrt(3.0) / 2 * sin) * arrowHeadSize + x
        val y1 = (-1.0 / 2.0 * sin - sqrt(3.0) / 2 * cos) * arrowHeadSize + y

        // point2
        val x2 = (1.0 / 2.0 * cos + sqrt(3.0) / 2 * sin) * arrowHeadSize + x
        val y2 = (1.0 / 2.0 * sin - sqrt(3.0) / 2 * cos) * arrowHeadSize + y

        elements.add(LineTo(x1, y1))
        elements.add(LineTo(x2, y2))
        elements.add(LineTo(x, y))
    }

    companion object {
        private const val defaultArrowHeadSize = 10.0
    }
}