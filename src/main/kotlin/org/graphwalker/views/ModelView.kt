package org.graphwalker.views

import com.sun.javafx.tk.FontLoader
import com.sun.javafx.tk.Toolkit
import guru.nidi.graphviz.*
import guru.nidi.graphviz.attribute.GraphAttr
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.model.MutableGraph
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.geometry.Point2D
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.MoveTo
import javafx.util.Duration
import org.graphwalker.core.machine.Context
import org.graphwalker.core.model.Vertex
import org.graphwalker.io.factory.json.JsonContext
import org.graphwalker.model.EdgeFX
import org.graphwalker.model.VertexFX
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File
import javafx.scene.shape.LineTo as LineTo1


class ModelEditor : View {
    val vertices = mutableListOf<VertexFX>()
    val edges = mutableListOf<EdgeFX>()
    var fontLoader: FontLoader = Toolkit.getToolkit().fontLoader
    val ANIMATION_DURATION = 250.0

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var context: Context by singleAssign()
    private var workArea: Pane by singleAssign()
    private var selectedVertex: StackPane? = null
    private var selectedOffset: Point2D? = null

    private val PLOT_SIZE = 500
    private val N_SEGS = PLOT_SIZE / 10

    constructor(title: String) : super(title) {
        context = JsonContext()
    }

    constructor(cntxt: Context) : super() {
        context = cntxt
        for (vertex in context.model.vertices) {
            vertices.add(createVertex(vertex))
        }
        for (edge in context.model.edges) {
            val edgeFX = EdgeFX(edge, vertices)
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
        val g = getGraphVizDot()
        var timeline = Timeline()

        val graphJSONObject = JSONObject(g?.toGraphviz()?.render(Format.JSON0).toString())
        var dpi = graphJSONObject.getString("dpi").toInt()
        dpi = 72
        val boundingBox = graphJSONObject.getString("bb").split(",")
        logger.debug(workArea.toString())
        var r = workArea.setPrefSize(boundingBox[2].toDouble(), boundingBox[3].toDouble())
        logger.debug(r.toString())

        layoutVertices(graphJSONObject, dpi, timeline, boundingBox)
        layoutEdges(graphJSONObject, timeline, boundingBox)

        timeline.play()
    }

    private fun layoutEdges(graphJSONObject: JSONObject, timeline: Timeline, boundingBox: List<String>) {
        var arr = graphJSONObject.getJSONArray("edges")
        for (edge in arr) {
            logger.debug(edge.toString())
            var source_gvid: String by singleAssign()
            var target_gvid: String by singleAssign()
            if (edge is JSONObject) {
                source_gvid = edge.get("tail").toString()
                target_gvid = edge.get("head").toString()
            }

            val sourceFX = vertices.filter { it.gvid == source_gvid }[0]
            val targetFX = vertices.filter { it.gvid == target_gvid }[0]

            var edgeFX: EdgeFX by singleAssign()
            for (e in edges) {
                if (e.element.name == edge.getString("label")) {
                    if (e.element.sourceVertex != null) {
                        if (e.element.sourceVertex.id == sourceFX.element.id) {
                            if (e.element.targetVertex.id == targetFX.element.id) {
                                edgeFX = e
                                break
                            }
                        }
                    } else if (e.element.targetVertex.id == targetFX.element.id) {
                        edgeFX = e
                        break
                    }
                }
            }
            logger.debug(edgeFX.text.toString())

            var labelPos = edge.getString("lp").split(",")
            edgeFX.text.layoutXProperty().unbind()
            edgeFX.text.layoutYProperty().unbind()
            timeline.keyFrames.add(KeyFrame(Duration.millis(ANIMATION_DURATION),
                    KeyValue(edgeFX.text.layoutXProperty(), labelPos[0].toDouble() - fontLoader.computeStringWidth(edgeFX.text.text, edgeFX.text.font).toDouble() / 2.0),
                    KeyValue(edgeFX.text.layoutYProperty(), boundingBox[3].toDouble() - labelPos[1].toDouble() - fontLoader.getFontMetrics(edgeFX.text.font).lineHeight.toDouble())))

            edgeFX.path.elements.clear()
            var str = edge.getString("pos").replace("e,", "")
            val pairs = str.split(" ")
            var doMoveTo = 0
            for (pair in pairs) {
                doMoveTo++
                if (doMoveTo == 1) {
                    continue
                }
                val pos = pair.split(",")
                if (doMoveTo == 2) {
                    edgeFX.path.elements.add(MoveTo(pos[0].toDouble(), boundingBox[3].toDouble() - pos[1].toDouble()))
                } else {
                    edgeFX.path.elements.add(LineTo1(pos[0].toDouble(), boundingBox[3].toDouble() - pos[1].toDouble()))
                }
            }
            logger.debug(edgeFX.path.elements.toString())
        }
    }

    private fun layoutVertices(graphJSONObject: JSONObject, dpi: Int, timeline: Timeline, boundingBox: List<String>) {
        var arr = graphJSONObject.getJSONArray("objects")
        for (node in arr) {
            logger.debug(node.toString())
            var vertexFX = vertices.filter { it.element.id == node.getString("name") }[0]

            // Get the graphviz id, it's needed to uniquely identify edges later on
            if (node is JSONObject) {
                vertexFX.gvid = node.get("_gvid").toString()
            }

            // The new rectangle size
            vertexFX.rect.height = node.getString("height").toDouble() * dpi
            vertexFX.rect.width = node.getString("width").toDouble() * dpi

            // The new position
            val pos = node.getString("pos").split(",")
            timeline.keyFrames.add(KeyFrame(Duration.millis(ANIMATION_DURATION),
                    KeyValue(vertexFX.layoutXProperty(), pos[0].toDouble() - vertexFX.rect.width / 2.0),
                    KeyValue(vertexFX.layoutYProperty(), boundingBox[3].toDouble() - pos[1].toDouble() - vertexFX.rect.height / 2.0)))
        }
    }

    private fun getGraphVizDot(): MutableGraph? {
        val g = graph("GraphWalker", directed = true) {
            graph[GraphAttr.splines(GraphAttr.SplineMode.SPLINE),
                    guru.nidi.graphviz.attribute.Font.name("courier"),
                    guru.nidi.graphviz.attribute.Font.size(16)]
            node[guru.nidi.graphviz.attribute.Font.name("courier"),
                    guru.nidi.graphviz.attribute.Font.size(16),
                    Shape.RECTANGLE]
            edge[guru.nidi.graphviz.attribute.Font.name("courier"),
                    guru.nidi.graphviz.attribute.Font.size(16)]
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
        logger.debug(g.toGraphviz().render(Format.DOT).toString())
        logger.debug(g.toGraphviz().render(Format.JSON0).toString())
        g.toGraphviz().render(Format.PNG).toFile(File("target/graphviz-model.png"))
        return g
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
