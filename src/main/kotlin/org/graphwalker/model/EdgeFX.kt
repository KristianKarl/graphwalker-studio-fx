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

    init {
        val start: VertexFX
        val end = vertices.filter { it.element.id == element.targetVertex.id }[0]
        start = if (element.sourceVertex != null) {
            vertices.filter { it.element.id == element.sourceVertex.id }[0]
        } else {
            end
        }

        startElement.xProperty().bind(start.layoutXProperty().add(start.translateXProperty()).add(start.widthProperty().divide(2)))
        startElement.yProperty().bind(start.layoutYProperty().add(start.translateYProperty()).add(start.heightProperty().divide(2)))

        endElement.xProperty().bind(end.layoutXProperty().add(end.translateXProperty()).add(end.widthProperty().divide(2)))
        endElement.yProperty().bind(end.layoutYProperty().add(end.translateYProperty()).add(end.heightProperty().divide(2)))

        path.elements.addAll(startElement, endElement)
        add(path)

        text = label {
            text = element.name
            font = Font.font("courier", FontWeight.THIN, FontPosture.REGULAR, 16.0)
            layoutXProperty().bind(endElement.xProperty().subtract(startElement.xProperty()).divide(2.0).add(startElement.xProperty()))
            layoutYProperty().bind(endElement.yProperty().subtract(startElement.yProperty()).divide(2.0).add(startElement.yProperty()))
        }
    }
}
