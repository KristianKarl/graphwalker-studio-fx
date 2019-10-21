package org.graphwalker.model

import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.BoundingBox
import javafx.geometry.Pos
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import org.graphwalker.control.LabelComboBox
import org.graphwalker.control.LabelTextField
import org.graphwalker.core.common.Objects.isNotNullOrEmpty
import org.graphwalker.core.model.Vertex
import org.slf4j.LoggerFactory

class VertexFX : VBox, ElementFX {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val bounds = BoundingBox(0.0, 0.0, 50.0, 20.0)
    private var labelName: Label? = null
    private var sharedStateName: Label? = null
    var vertex: Vertex? = null
        private set
    var x: DoubleProperty? = null
        private set
    var y: DoubleProperty? = null
        private set
    private var graph: GraphFX? = null
    private var startElementMenu: CheckMenuItem? = null
    private var sharedStateMenu: CheckMenuItem? = null
    private val sharedStateLabels = ComboBox<String>()

    private val strokeWidthVisited = 1
    private val strokeWidthNormal = 2
    private val strokeWidthSelected = 4
    private val strokeWidthHighLighted = 5

    override var isSelected = false
        protected set
    override var isVisited = false
        protected set
    protected var highLighted = false

    override val elementId: String
        get() = vertex!!.id

    override val elementName: String
        get() = if (isNotNullOrEmpty(vertex!!.name)) vertex!!.name else ""


    constructor(graph: GraphFX, vertex: Vertex) {
        this.graph = graph
        this.vertex = vertex
        init()
    }

    constructor(graph: GraphFX, name: String) {
        this.graph = graph
        vertex = Vertex().setName(name)
        init()
    }

    private fun init() {
        Platform.runLater { this.toFront() }

        setOnMouseClicked { mouseEvent ->
            logger.debug("VertexFX Clicked")
            if (!graph!!.rubberLine.isDisabled) {
                graph!!.endCreateEdge(this@VertexFX)
                mouseEvent.consume()
                return@setOnMouseClicked
            }

            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.isShiftDown) {
                    graph!!.startCreateEdge(mouseEvent.source as VertexFX)
                    mouseEvent.consume()
                    return@setOnMouseClicked
                }
                logger.debug("Select vertex: " + vertex!!.name)
                selected(true)
                mouseEvent.consume()
            }
        }

        labelName = Label(vertex!!.name)
        labelName!!.textAlignment = TextAlignment.CENTER
        labelName!!.alignment = Pos.CENTER
        labelName!!.setOnMouseClicked { mouseEvent ->
            logger.debug("Label clicked")
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    logger.debug("Vertex label is double clicked")
                    children.add(0, LabelTextField(labelName))
                }
            }
        }
        labelName!!.textProperty().addListener { observable, oldValue, newValue -> vertex!!.name = newValue }

        children.addAll(StackPane(labelName))
        styleClass.add("node")
        minWidth = bounds.width
        minHeight = bounds.height

        x = SimpleDoubleProperty()
        x!!.bind(layoutXProperty().add(bounds.width / 2))

        y = SimpleDoubleProperty()
        y!!.bind(layoutYProperty().add(bounds.height / 2))

        if (vertex!!.hasProperty("x") && vertex!!.hasProperty("y")) {
            relocate(vertex!!.getProperty("x") as Double, vertex!!.getProperty("y") as Double)
        }

        val contextMenu = ContextMenu()

        startElementMenu = CheckMenuItem("Start element")
        startElementMenu!!.selectedProperty().addListener { observable, oldValue, newValue ->
            logger.debug("Current start id changed for: " + labelName!!.text + ", " + newValue)
            if (newValue!!) {
                setStartElement(true)
                graph!!.graphWalkerAppFX.setElementStartId(this)
            } else {
                setStartElement(false)
            }
        }

        sharedStateMenu = CheckMenuItem("Shared state")
        if (isNotNullOrEmpty(vertex!!.sharedState)) {
            sharedStateMenu!!.isSelected = true
            createSharedStateCombo()
        }

        sharedStateMenu!!.selectedProperty().addListener { observable, oldValue, newValue ->
            logger.debug("Shared state changed for: " + labelName!!.text + ", " + newValue)
            if (newValue!!) {
                createSharedStateCombo()
            } else {
                children.remove(sharedStateName)
            }
        }

        contextMenu.items.addAll(startElementMenu, sharedStateMenu)

        setOnContextMenuRequested { event ->
            contextMenu.show(this@VertexFX, event.screenX, event.screenY)
            event.consume()
        }
    }

    private fun createSharedStateCombo() {
        if (isNotNullOrEmpty(vertex!!.sharedState)) {
            sharedStateName = Label(vertex!!.sharedState)
        } else {
            sharedStateName = Label("New Shared State Name")
        }
        sharedStateName!!.textAlignment = TextAlignment.CENTER
        sharedStateName!!.alignment = Pos.CENTER
        sharedStateName!!.setOnMouseClicked { mouseEvent ->
            logger.debug("Clicked")
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    logger.debug("Vertex label is double clicked")
                    children.add(1, LabelComboBox(sharedStateName,
                            graph!!.graphWalkerAppFX.getListOfSharedStateLabels()))
                }
            }
        }
        sharedStateName!!.textProperty().addListener { o, oldV, newV -> vertex!!.sharedState = newV }
        children.add(1, sharedStateName)
    }

    override fun setStartElement(setElement: Boolean) {
        if (setElement) {
            style = "-fx-background-color: lightgreen;"
            startElementMenu!!.isSelected = true
        } else {
            style = "-fx-background-color: lightblue;"
            if (startElementMenu!!.isSelected) {
                startElementMenu!!.isSelected = false
            }
        }
    }

    override fun highlight(highLight: Boolean) {
        this.highLighted = highLight
        setElementStyle()
    }

    fun setElementStyle() {
        if (highLighted) {
            style = "-fx-border-color: lightblue;" +
                    "-fx-border-width: " + strokeWidthSelected + ";"
            labelName!!.style = "-fx-text-fill: black; -fx-font-weight: bold;"
            return
        }

        if (isSelected) {
            if (startElementMenu!!.isSelected) {
                style = "-fx-background-color: lightgreen;" +
                        "-fx-border-width: " + strokeWidthSelected + ";"
            } else {
                style = "-fx-background-color: lightblue;" +
                        "-fx-border-width: " + strokeWidthSelected + ";"
            }
            labelName!!.style = "-fx-text-fill: black; -fx-font-weight: bold;"
            return
        }

        if (isVisited) {
            if (startElementMenu!!.isSelected) {
                style = "-fx-border-color: black;" +
                        "-fx-background-color: #CEF6CE;" +
                        "-fx-border-width: " + strokeWidthVisited + ";"
            } else {
                style = "-fx-border-color: black;" +
                        "-fx-background-color: lightgrey;" +
                        "-fx-border-width: " + strokeWidthVisited + ";"
            }
            labelName!!.style = "-fx-text-fill: black; -fx-font-weight: normal;"
            return
        }

        if (startElementMenu!!.isSelected) {
            style = "-fx-border-color: black;" +
                    "-fx-background-color: lightgreen;" +
                    "-fx-border-width: " + strokeWidthNormal + ";"
        } else {
            style = "-fx-border-color: black;" +
                    "-fx-background-color: lightblue;" +
                    "-fx-border-width: " + strokeWidthNormal + ";"
        }
        labelName!!.style = "-fx-text-fill: black; -fx-font-weight: normal;"
    }

    override fun selected(selected: Boolean) {
        this.isSelected = selected
        setElementStyle()
    }

    override fun visited(visited: Boolean) {
        this.isVisited = visited
        setElementStyle()
    }
}
