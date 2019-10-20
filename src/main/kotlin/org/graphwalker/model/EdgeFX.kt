package org.graphwalker.model

import javafx.scene.Group
import javafx.scene.control.Label
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import org.graphwalker.core.model.Edge
import tornadofx.*

class EdgeFX(edge: Edge.RuntimeEdge, vertices: List<VertexFX>) : Group() {
    var element = edge
    var path = Path()
    var text = Label()
    var startElement = MoveTo()
    var endElement = LineTo()
    var startFX: VertexFX by singleAssign()
    var targetFX: VertexFX by singleAssign()

    init {
        targetFX = vertices.filter { it.element.id == element.targetVertex.id }[0]
        startFX = if (element.sourceVertex != null) {
            vertices.filter { it.element.id == element.sourceVertex.id }[0]
        } else {
            targetFX
        }

        startElement.xProperty().bind(startFX.layoutXProperty().add(startFX.translateXProperty()).add(startFX.widthProperty().divide(2)))
        startElement.yProperty().bind(startFX.layoutYProperty().add(startFX.translateYProperty()).add(startFX.heightProperty().divide(2)))

        endElement.xProperty().bind(targetFX.layoutXProperty().add(targetFX.translateXProperty()).add(targetFX.widthProperty().divide(2)))
        endElement.yProperty().bind(targetFX.layoutYProperty().add(targetFX.translateYProperty()).add(targetFX.heightProperty().divide(2)))

        path.elements.addAll(startElement, endElement)
        add(path)

        text = label {
            text = element.name
            font = Font.font("DejaVu Sans Mono", FontWeight.THIN, FontPosture.REGULAR, 16.0)
            layoutXProperty().bind(endElement.xProperty().subtract(startElement.xProperty()).divide(2.0).add(startElement.xProperty()))
            layoutYProperty().bind(endElement.yProperty().subtract(startElement.yProperty()).divide(2.0).add(startElement.yProperty()))
        }
    }
}
