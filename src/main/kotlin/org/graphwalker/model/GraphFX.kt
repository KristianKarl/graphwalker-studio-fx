package org.graphwalker.model

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.shape.Line
import org.graphwalker.application.GraphWalkerAppFX
import org.graphwalker.core.model.Edge
import org.graphwalker.core.model.Model
import org.graphwalker.core.model.Vertex
import org.graphwalker.util.ModelDialogs
import org.graphwalker.util.MouseGestures
import org.graphwalker.util.ZoomableScrollPaneFX
import org.slf4j.LoggerFactory
import java.util.*

class GraphFX(val graphWalkerAppFX: GraphWalkerAppFX) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val root: StackPane
    val scrollPane: ZoomableScrollPaneFX
    internal var mouseGestures: MouseGestures
    internal var elements: MutableList<ElementFX> = ArrayList<ElementFX>()
    var model: Model? = Model()

    var rubberLine: Line
        internal set
    internal lateinit var startVertex: VertexFX
    internal var modelName: SimpleStringProperty
    internal var generator: SimpleStringProperty

    /**
     * the pane wrapper is necessary or else the scrollpane would always align
     * the top-most and left-most child to the top and left eg when you drag the
     * top child down, the entire scrollpane would move down
     */
    var contentPane: Pane
        internal set

    val rootPane: Pane
        get() = this.root

    val scale: Double
        get() = this.scrollPane.getScaleValue()

    init {
        root = StackPane()

        rubberLine = Line()
        rubberLine.isDisable = true
        rubberLine.isMouseTransparent = true

        mouseGestures = MouseGestures(this)

        contentPane = Pane()
        contentPane.prefWidth = 1e6
        contentPane.prefHeight = 1e6

        contentPane.setOnMouseClicked { mouseEvent ->
            logger.debug("ContentPane clicked")
            if (!rubberLine.isDisabled) {
                return@contentPane.setOnMouseClicked
            }

            if (mouseEvent.button == MouseButton.PRIMARY && mouseEvent.isControlDown) {
                logger.debug("Left click and Control @: " + mouseEvent.x + ", " + mouseEvent.y)
                logger.debug("                   scale: $scale")

                val vertex = Vertex().setName("New State").setId(UUID.randomUUID().toString())
                vertex.setProperty("x", mouseEvent.x)
                vertex.setProperty("y", mouseEvent.y)
                addVertex(VertexFX(this, vertex))
            } else if (mouseEvent.button == MouseButton.PRIMARY) {
                logger.debug("Deselect all")
                for (element in elements) {
                    element.selected(false)
                }
            } else if (mouseEvent.button == MouseButton.SECONDARY) {
                logger.debug("Right mouse button clicked on the graph")
            }
        }

        contentPane.setOnMouseMoved { mouseEvent ->
            if (!rubberLine.isDisabled) {
                rubberLine.endX = mouseEvent.x
                rubberLine.endY = mouseEvent.y
            }
        }

        generator = SimpleStringProperty(DEFAULT_GRAPH_GENERATOR)

        scrollPane = ZoomableScrollPaneFX(contentPane)
        root.children.addAll(scrollPane)

        modelName = SimpleStringProperty()

        val contextMenu = ContextMenu()

        val dataMenu = MenuItem("Model properties...")
        dataMenu.setOnAction { e ->
            if (ModelDialogs().runModel(this).isPresent()) {
                graphWalkerAppFX.setTabLabel(this, graphWalkerAppFX.getTabPane().getSelectionModel().getSelectedItem())
            }
        }

        contextMenu.items.add(dataMenu)

        contentPane.setOnContextMenuRequested { event ->
            contextMenu.show(contentPane, event.screenX, event.screenY)
            event.consume()
        }
    }

    fun addVertex(vertex: VertexFX) {
        logger.debug("Adding vertex: $vertex")
        elements.add(vertex)
        contentPane.children.add(vertex)
        mouseGestures.makeDraggable(vertex)
        mouseGestures.logHover(vertex)
    }

    fun addEdge(edgeFX: EdgeFX) {
        elements.add(edgeFX)
        contentPane.children.add(edgeFX)
        mouseGestures.logHover(edgeFX)
    }

    fun getElements(): List<ElementFX> {
        return elements
    }

    fun setGenerator(generator: String) {
        this.generator.set(generator)
    }

    fun getGenerator(): String {
        return generator.value
    }

    fun removeAllElementFXs() {
        logger.debug("Removing all UI elements from graph")
        contentPane.children.clear()
    }

    fun clear() {
        setGenerator("")
        elements.clear()
        model = null
        removeAllElementFXs()
    }

    fun fitGraphInWindow() {
        scrollPane.zoomToFit()
    }

    fun startCreateEdge(startVertex: VertexFX) {
        this.startVertex = startVertex
        rubberLine.startXProperty().bind(startVertex.getX())
        rubberLine.startYProperty().bind(startVertex.getY())
        rubberLine.endX = startVertex.getX().getValue()
        rubberLine.endY = startVertex.getY().getValue()
        rubberLine.isDisable = false
        contentPane.children.add(rubberLine)
    }

    fun endCreateEdge(endVertex: VertexFX) {
        contentPane.children.remove(rubberLine)
        rubberLine.isDisable = true
        val edge = EdgeFX(graphWalkerAppFX,
                startVertex,
                endVertex,
                Edge()
                        .setSourceVertex(startVertex.getVertex())
                        .setTargetVertex(endVertex.getVertex())
                        .setName("New Transition")
                        .setId(UUID.randomUUID().toString()))
        addEdge(edge)
        GraphWalkerLayout(this).doEdge(edge)
    }

    companion object {
        private val DEFAULT_GRAPH_GENERATOR = "random(edge_coverage(100))"
    }
}
