package org.graphwalker.views

import com.sun.javafx.tk.Toolkit
import javafx.geometry.Point2D
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import org.graphwalker.core.machine.Context
import org.graphwalker.core.model.Edge
import org.graphwalker.core.model.Vertex
import org.graphwalker.io.factory.json.JsonContextFactory
import tornadofx.*
import java.nio.file.Paths

val vertices = mutableListOf<VertexFX>()
var fontLoader = Toolkit.getToolkit().fontLoader


class VertexFX(vertex: Vertex.RuntimeVertex) : StackPane() {
    val element = vertex
    var text: Label by singleAssign()
    var rect: Rectangle by singleAssign()

    init {
        rect = rectangle {
            fill = Color.LIGHTBLUE
            height = 20.0
            width = 50.0
            strokeWidth = 1.0
            stroke = Color.BLACK
        }

        text = label {
            text = vertex.name
            font = Font.font("Consolas", FontWeight.THIN, FontPosture.REGULAR, 16.0)
        }

        if (element.hasProperty("x") && element.hasProperty("y")) {
            layoutX = element.getProperty("x").toString().toDouble()
            layoutY = element.getProperty("y").toString().toDouble()
        }
    }
}

class EdgeFX(edge: Edge.RuntimeEdge) {
    val element = edge

    fun getLine(): Line {
        var start: VertexFX
        val end = vertices.filter { it.element.id == element.targetVertex.id }[0]
        start = if (element.sourceVertex != null) {
            vertices.filter { it.element.id == element.sourceVertex.id }[0]
        } else {
            end
        }

        var line = Line()
        line.startXProperty().bind(start.layoutXProperty().add(start.translateXProperty()).add(start.widthProperty().divide(2)))
        line.startYProperty().bind(start.layoutYProperty().add(start.translateYProperty()).add(start.heightProperty().divide(2)))
        line.endXProperty().bind(end.layoutXProperty().add(end.translateXProperty()).add(end.widthProperty().divide(2)))
        line.endYProperty().bind(end.layoutYProperty().add(end.translateYProperty()).add(end.heightProperty().divide(2)))
        return line
    }
}


class ModelEditor(title: String) : View(title) {

    var workArea: Pane by singleAssign()
    var selectedVertex: StackPane? = null
    var selectedOffset: Point2D? = null

    override val root = scrollpane {
        workArea = pane {

            style {
                backgroundColor += Color.LIGHTYELLOW
            }

            fun createEdge(edge: Edge.RuntimeEdge): Line {
                var edgeFx = EdgeFX(edge)
                return edgeFx.getLine()
            }

            fun createVertex(vertex: Vertex.RuntimeVertex): VertexFX {
                var vertexFX = VertexFX(vertex)
                vertexFX.rect.width = fontLoader.computeStringWidth(vertexFX.text.text, vertexFX.text.font).toDouble()
                vertexFX.rect.height = fontLoader.getFontMetrics(vertexFX.text.font).lineHeight.toDouble()
                vertices.add(vertexFX)
                return vertexFX
            }

            fun readGraphWalkerModel(fileName: String): List<Context> {
                val factory = JsonContextFactory()
                return factory.create(Paths.get(fileName))
            }

            val contexts = readGraphWalkerModel("/home/krikar/dev/graphwalker/graphwalker-project/graphwalker-studio/src/test/resources/json/UC01.json")
            for (context in contexts) {
                for (vertex in context.model.vertices) {
                    createVertex(vertex)
                }
                for (edge in context.model.edges) {
                    add(createEdge(edge))
                }
            }
            for (vertexFX in vertices) {
                add(vertexFX)
            }
        }

        addEventFilter(MouseEvent.MOUSE_PRESSED, ::startDrag)
        addEventFilter(MouseEvent.MOUSE_DRAGGED, ::drag)
        addEventFilter(MouseEvent.MOUSE_RELEASED, ::stopDrag)
    }


    private fun startDrag(evt: MouseEvent) {
        vertices
                .filter {
                    val mousePt = it.sceneToLocal(evt.sceneX, evt.sceneY)
                    it.contains(mousePt)
                }
                .firstOrNull()
                .apply {
                    if (this != null) {
                        selectedVertex = this

                        val mp = this.parent.sceneToLocal(evt.sceneX, evt.sceneY)
                        val vizBounds = this.boundsInParent

                        selectedOffset = Point2D(
                                mp.x - vizBounds.minX - (vizBounds.width - this.boundsInLocal.width) / 2,
                                mp.y - vizBounds.minY - (vizBounds.height - this.boundsInLocal.height) / 2
                        )
                    }
                }
    }

    private fun drag(evt: MouseEvent) {
        val mousePt: Point2D = (evt.source as ScrollPane).sceneToLocal(evt.sceneX, evt.sceneY)
        if (selectedVertex != null && selectedOffset != null) {

            selectedVertex!!.relocate(
                    mousePt.x - selectedOffset!!.x,
                    mousePt.y - selectedOffset!!.y)
        }
    }

    private fun stopDrag(evt: MouseEvent) {
        selectedVertex = null
        selectedOffset = null
    }
}
