package org.graphwalker.views

import com.sun.javafx.tk.FontLoader
import com.sun.javafx.tk.Toolkit
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.get
import guru.nidi.graphviz.graph
import guru.nidi.graphviz.minus
import guru.nidi.graphviz.toGraphviz
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.util.Duration
import org.graphwalker.core.machine.Context
import org.graphwalker.core.model.Edge
import org.graphwalker.core.model.Vertex
import org.graphwalker.io.factory.json.JsonContext
import org.json.JSONObject
import tornadofx.*
import java.io.File

val vertices = mutableListOf<VertexFX>()
val edges = mutableListOf<EdgeFX>()
var fontLoader: FontLoader = Toolkit.getToolkit().fontLoader

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

class EdgeFX(edge: Edge.RuntimeEdge) : Group() {
    val element = edge
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

        //path.elements.addAll(startElement, endElement)
        add(path)

        text = label {
            text = edge.name
            font = Font.font("Consolas", FontWeight.THIN, FontPosture.REGULAR, 16.0)
            layoutXProperty().bind(endElement.xProperty().subtract(startElement.xProperty()).divide(2.0).add(startElement.xProperty()))
            layoutYProperty().bind(endElement.yProperty().subtract(startElement.yProperty()).divide(2.0).add(startElement.yProperty()))
        }
    }
}


class ModelEditor : View {

    private var context: Context by singleAssign()
    private var workArea: Pane by singleAssign()
    private var selectedVertex: StackPane? = null
    private var selectedOffset: Point2D? = null

    constructor(title: String) : super(title) {
        context = JsonContext()
    }

    constructor(cntxt: Context) : super() {
        context = cntxt
        for (vertex in context.model.vertices) {
            vertices.add(createVertex(vertex))
        }
        for (edge in context.model.edges) {
            val edgeFX = EdgeFX(edge)
            edges.add(edgeFX)
            workArea.add(edgeFX)
        }
        for (vertexFX in vertices) {
            workArea.add(vertexFX)
        }
    }

    override val root = scrollpane {
        workArea = pane {
            style {
                backgroundColor += Color.LIGHTYELLOW
            }

            contextmenu {
                item("Autolayout").action {
                    doAutoLayout()
                }
            }
        }

        addEventFilter(MouseEvent.MOUSE_PRESSED, ::startDrag)
        addEventFilter(MouseEvent.MOUSE_DRAGGED, ::drag)
        addEventFilter(MouseEvent.MOUSE_RELEASED, ::stopDrag)
    }

    private fun doAutoLayout() {
        val g = graph("GraphWalker", directed = true) {
            for (e in context.model.edges) {
                if (e.sourceVertex == null) {
                    (e.targetVertex.id[guru.nidi.graphviz.attribute.Label.of(e.targetVertex.name)] -
                            e.targetVertex.id[guru.nidi.graphviz.attribute.Label.of(e.targetVertex.name)])[guru.nidi.graphviz.attribute.Label.of(e.name)]
                } else {
                    (e.sourceVertex.id[guru.nidi.graphviz.attribute.Label.of(e.sourceVertex.name)] -
                            e.targetVertex.id[guru.nidi.graphviz.attribute.Label.of(e.targetVertex.name)])[guru.nidi.graphviz.attribute.Label.of(e.name)]
                }
            }
        }
        println(g.toGraphviz().render(Format.JSON0).toString())
        g.toGraphviz().render(Format.PNG).toFile(File("target/graphviz-model.png"))

        var timeline = Timeline()
        val obj = JSONObject(g.toGraphviz().render(Format.JSON0).toString())
        var arr = obj.getJSONArray("objects")
        for (node in arr) {
            var vertexFX = vertices.filter { it.element.id == node.getString("name") }[0]
            println(vertexFX.text)

            val pos = node.getString("pos").split(",")

            val xValue = KeyValue(vertexFX.layoutXProperty(), pos[0].toDouble())
            val yValue = KeyValue(vertexFX.layoutYProperty(), pos[1].toDouble())

            val keyFrame = KeyFrame(Duration.millis(250.0), xValue, yValue)
            timeline.keyFrames.add(keyFrame)
        }


        arr = obj.getJSONArray("edges")
        for (edge in arr) {
            var edgeFX = edges.filter { it.element.name == edge.getString("label") }[0]
            println(edgeFX.text)
            var labelPos = edge.getString("lp").split(",")
            //edgeFX.text.translateX= labelPos[0].toDouble()
            //edgeFX.text.translateY = labelPos[1].toDouble()

            // "e,305.99,532.4 305.99,547.6 320.67,546.8 331.24,544.27 331.24,540 331.24,536.8 325.29,534.58 316.13,533.33"
            var str = edge.getString("pos").replace("e,", "")
            val pairs = str.split(" ")
            for (pair in pairs) {
                val pos = pair.split(",")
                //edgeFX.path.elements.add(LineTo(pos[0].toDouble(), pos[1].toDouble()))
            }
        }

        timeline.play()
    }

    private fun createVertex(vertex: Vertex.RuntimeVertex): VertexFX {
        val vertexFX = VertexFX(vertex)
        vertexFX.rect.width = fontLoader.computeStringWidth(vertexFX.text.text, vertexFX.text.font).toDouble()
        vertexFX.rect.height = fontLoader.getFontMetrics(vertexFX.text.font).lineHeight.toDouble()
        return vertexFX
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

private fun Any.getString(s: String): String {
    if (this is JSONObject)
        return this.getString(s)
    return ""
}
