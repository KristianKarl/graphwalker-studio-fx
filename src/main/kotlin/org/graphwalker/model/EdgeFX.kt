package org.graphwalker.model

import javafx.application.Platform
import javafx.geometry.Point2D
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.CubicCurve
import javafx.scene.shape.Polygon
import javafx.scene.transform.Rotate
import org.graphwalker.application.GraphWalkerAppFX
import org.graphwalker.control.LabelTextField
import org.graphwalker.core.common.Objects.isNotNullOrEmpty
import org.graphwalker.core.model.Edge
import org.graphwalker.util.ModelDialogs
import org.slf4j.LoggerFactory

class EdgeFX(private val GraphWalkerAppFX: GraphWalkerAppFX, var source: VertexFX?, var target: VertexFX?, val edge: Edge) : Pane(), ElementFX {
    private val logger = LoggerFactory.getLogger(this::class.java)

    var curve: CubicCurve? = null
        private set
    private var arrowHead: ArrowHead? = null
    var labelName: Label? = null
        private set
    private var guardLabel: Label? = null
    private var actionLabel: Label? = null
    private var startElementMenu: CheckMenuItem? = null

    private val strokeWidthVisited = 1
    private val strokeWidthNormal = 2
    private val strokeWidthSelected = 4
    private val strokeWidthHighLighted = 5

    override var isSelected = false
        private set
    private var visited = false
    private var highLighted = false

    override val isVisited: Boolean
        get() = false

    override val elementId: String
        get() = edge.id

    override val elementName: String
        get() = if (isNotNullOrEmpty(edge.name)) edge.name else ""

    init {
        init()
    }

    private fun init() {
        Platform.runLater { this.toBack() }

        isPickOnBounds = false

        setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY) {
                mouseEvent.consume()
                selected(true)
            }
            if (mouseEvent.button == MouseButton.PRIMARY) {
                if (mouseEvent.clickCount == 2) {
                    logger.debug("Edge label is double clicked")
                    val labelTextField = LabelTextField(labelName)
                    labelTextField.layoutXProperty().bind(labelName!!.layoutXProperty())
                    labelTextField.layoutYProperty().bind(labelName!!.layoutYProperty())
                    children.add(0, labelTextField)
                }
            }
        }

        labelName = Label(edge.name)
        labelName!!.textProperty().addListener { observable, oldValue, newValue -> edge.name = newValue }

        var guardStr = ""
        if (edge.guard != null && edge.guard.script != null) {
            guardStr = edge.guard.script.toString()
        }
        guardLabel = Label("Guard: $guardStr")

        var actionStr = ""
        if (edge.actions != null) {
            for (action in edge.actions) {
                if (action.script != null) {
                    actionStr += action.script.toString()
                }
            }
        }
        actionLabel = Label("Action: $actionStr")

        curve = CubicCurve()

        curve!!.stroke = Color.BLACK
        curve!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthNormal; -fx-fill: null;"

        curve!!.startXProperty().bind(source!!.getX())
        curve!!.startYProperty().bind(source!!.getY())

        curve!!.endXProperty().bind(target!!.getX())
        curve!!.endYProperty().bind(target!!.getY())

        target!!.widthProperty().addListener({ observable, oldValue, newValue -> update() })
        target!!.heightProperty().addListener({ observable, oldValue, newValue -> update() })

        curve!!.startXProperty().addListener { observable, oldValue, newValue -> update() }
        curve!!.startYProperty().addListener { observable, oldValue, newValue -> update() }
        curve!!.endXProperty().addListener { observable, oldValue, newValue -> update() }
        curve!!.endYProperty().addListener { observable, oldValue, newValue -> update() }

        curve!!.controlX1Property().addListener { observable, oldValue, newValue -> update() }
        curve!!.controlY1Property().addListener { observable, oldValue, newValue -> update() }
        curve!!.controlX2Property().addListener { observable, oldValue, newValue -> update() }
        curve!!.controlY2Property().addListener { observable, oldValue, newValue -> update() }

        val arrowShape = doubleArrayOf(0.0, 0.0, 5.0, 10.0, -5.0, 10.0)
        arrowHead = ArrowHead(curve, 0.1f, *arrowShape)

        children.addAll(curve, arrowHead, labelName, guardLabel, actionLabel)

        val contextMenu = ContextMenu()

        startElementMenu = CheckMenuItem("Set as start element")
        startElementMenu!!.selectedProperty().addListener { observable, oldValue, newValue ->
            logger.debug("Current start id changed for: " + labelName!!.text + ", " + newValue)
            if (newValue!!) {
                setStartElement(true)
                GraphWalkerAppFX.setElementStartId(this)
            } else {
                setStartElement(false)
            }
        }

        val dataMenu = MenuItem("Edge properties...")
        dataMenu.setOnAction { e ->
            if (ModelDialogs().runEdge(edge).isPresent()) {
                labelName!!.text = edge.name
                guardLabel!!.text = "Guard: " + edge.guard.script
                var str = ""
                if (edge.actions != null) {
                    for (action in edge.actions) {
                        if (action.script != null) {
                            str += action.script.toString()
                        }
                    }
                }
                actionLabel!!.text = "Action: $str"
                update()
            }
        }

        contextMenu.items.addAll(startElementMenu, dataMenu)

        setOnContextMenuRequested { event ->
            contextMenu.show(this@EdgeFX, event.screenX, event.screenY)
            event.consume()
        }

        setElementStyle()
    }

    fun update() {
        val pt = arrowHead!!.eval(curve!!, .5f)
        labelName!!.layoutX = pt.x
        labelName!!.layoutY = pt.y

        var posY = pt.y + labelName!!.height
        if (guardLabel!!.text != "Guard: ") {
            guardLabel!!.isVisible = true
            guardLabel!!.layoutX = pt.x
            guardLabel!!.layoutY = posY
            posY += guardLabel!!.height
        } else {
            guardLabel!!.isVisible = false
        }
        if (actionLabel!!.text != "Action: ") {
            actionLabel!!.isVisible = true
            actionLabel!!.layoutX = pt.x
            actionLabel!!.layoutY = posY
        } else {
            actionLabel!!.isVisible = false
        }

        arrowHead!!.t = findT(1f)
        arrowHead!!.update()
    }

    private fun findT(t: Float): Float {
        var t = t
        val pt = arrowHead!!.eval(curve!!, t)
        if (target!!.boundsInParent.contains(pt)) {
            t -= .001f
            if (t > 1e-4) {
                return findT(t)
            }
        }
        return t
    }

    fun deselect() {
        isSelected = false
        curve!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthNormal; -fx-fill: null;"
        arrowHead!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthNormal; -fx-fill: black;"
        labelName!!.style = "-fx-font-weight: normal;"
        guardLabel!!.style = "-fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
        actionLabel!!.style = "-fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
    }

    class ArrowHead(internal var curve: CubicCurve, var t: Float, vararg arg0: Double) : Polygon(arg0) {
        internal var rz: Rotate

        init {
            init()
        }

        private fun init() {
            fill = Color.BLACK
            rz = Rotate()
            rz.axis = Rotate.Z_AXIS
            transforms.addAll(rz)
            update()
        }

        fun update() {
            val size = Math.max(curve.boundsInLocal.width, curve.boundsInLocal.height)
            val scale = size / 4.0

            val ori = eval(curve, t)
            val tan = evalDt(curve, t).normalize().multiply(scale)

            translateX = ori.x
            translateY = ori.y

            var angle = Math.atan2(tan.y, tan.x)

            angle = Math.toDegrees(angle)

            // arrow origin is top => apply offset
            var offset = -90.0
            if (t > 0.5) {
                offset = +90.0
            }

            rz.angle = angle + offset
        }

        /**
         * Evaluate the cubic curve at a parameter 0<=t<=1, returns a Point2D
         *
         * See: https://pomax.github.io/bezierinfo/#projections
         *
         * @param c the CubicCurve
         * @param t param between 0 and 1
         * @return a Point2D
         */
        fun eval(c: CubicCurve, t: Float): Point2D {
            return Point2D(Math.pow((1 - t).toDouble(), 3.0) * c.startX +
                    3.0 * t.toDouble() * Math.pow((1 - t).toDouble(), 2.0) * c.controlX1 +
                    3.0 * (1 - t).toDouble() * t.toDouble() * t.toDouble() * c.controlX2 +
                    Math.pow(t.toDouble(), 3.0) * c.endX,
                    Math.pow((1 - t).toDouble(), 3.0) * c.startY +
                            3.0 * t.toDouble() * Math.pow((1 - t).toDouble(), 2.0) * c.controlY1 +
                            3.0 * (1 - t).toDouble() * t.toDouble() * t.toDouble() * c.controlY2 +
                            Math.pow(t.toDouble(), 3.0) * c.endY)
        }

        /**
         * Evaluate the tangent of the cubic curve at a parameter 0<=t<=1, returns a Point2D
         *
         * @param c the CubicCurve
         * @param t param between 0 and 1
         * @return a Point2D
         */
        fun evalDt(c: CubicCurve, t: Float): Point2D {
            return Point2D((-3.0 * Math.pow((1 - t).toDouble(), 2.0) * c.startX +
                    3.0 * (Math.pow((1 - t).toDouble(), 2.0) - 2f * t * (1 - t)) * c.controlX1 +
                    3.0 * ((1 - t) * 2f * t - t * t).toDouble() * c.controlX2 +
                    3.0 * Math.pow(t.toDouble(), 2.0) * c.endX),
                    (-3.0 * Math.pow((1 - t).toDouble(), 2.0) * c.startY +
                            3.0 * (Math.pow((1 - t).toDouble(), 2.0) - 2f * t * (1 - t)) * c.controlY1 +
                            3.0 * ((1 - t) * 2f * t - t * t).toDouble() * c.controlY2 +
                            3.0 * Math.pow(t.toDouble(), 2.0) * c.endY))
        }
    }

    fun setStartElement(setElement: Boolean) {
        startElementMenu!!.isSelected = setElement
        setElementStyle()
    }

    private fun setElementStyle() {
        if (highLighted) {
            curve!!.style = "-fx-stroke: lightblue; -fx-stroke-width: $strokeWidthHighLighted; -fx-fill: null;"
            arrowHead!!.style = "-fx-stroke: lightblue; -fx-stroke-width: $strokeWidthHighLighted; -fx-fill: lightblue;"
            labelName!!.style = "-fx-text-fill: lightblue; -fx-font-weight: bold;"
            guardLabel!!.style = "-fx-text-fill: lightblue; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;"
            actionLabel!!.style = "-fx-text-fill: lightblue; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;"
            return
        }

        if (isSelected) {
            if (startElementMenu!!.isSelected) {
                curve!!.style = "-fx-stroke: lightgreen; -fx-stroke-width: $strokeWidthSelected; -fx-fill: null;"
                arrowHead!!.style = "-fx-stroke: lightgreen; -fx-stroke-width: $strokeWidthSelected; -fx-fill: lightgreen;"
                labelName!!.style = "-fx-text-fill: black; -fx-font-weight: bold;"
                guardLabel!!.style = "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;"
                actionLabel!!.style = "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;"
            } else {
                curve!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthSelected; -fx-fill: null;"
                arrowHead!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthSelected; -fx-fill: black;"
                labelName!!.style = "-fx-text-fill: black; -fx-font-weight: bold;"
                guardLabel!!.style = "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;"
                actionLabel!!.style = "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 8;"
            }
            return
        }

        if (visited) {
            if (startElementMenu!!.isSelected) {
                curve!!.style = "-fx-stroke: #CEF6CE; -fx-stroke-width: $strokeWidthVisited; -fx-fill: null;"
                arrowHead!!.style = "-fx-stroke: #CEF6CE; -fx-stroke-width: $strokeWidthVisited; -fx-fill: #CEF6CE;"
                labelName!!.style = "-fx-text-fill: lightgrey; -fx-font-weight: normal;"
                guardLabel!!.style = "-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
                actionLabel!!.style = "-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
            } else {
                curve!!.style = "-fx-stroke: lightgrey; -fx-stroke-width: $strokeWidthVisited; -fx-fill: null;"
                arrowHead!!.style = "-fx-stroke: lightgrey; -fx-stroke-width: $strokeWidthVisited; -fx-fill: lightgrey;"
                labelName!!.style = "-fx-text-fill: lightgrey; -fx-font-weight: normal;"
                guardLabel!!.style = "-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
                actionLabel!!.style = "-fx-text-fill: lightgrey; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
            }
            return
        }

        if (startElementMenu!!.isSelected) {
            curve!!.style = "-fx-stroke: lightgreen; -fx-stroke-width: $strokeWidthNormal; -fx-fill: null;"
            arrowHead!!.style = "-fx-stroke: lightgreen; -fx-stroke-width: $strokeWidthNormal; -fx-fill: lightgreen;"
            labelName!!.style = "-fx-text-fill: black; -fx-font-weight: normal;"
            guardLabel!!.style = "-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
            actionLabel!!.style = "-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
        } else {
            curve!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthNormal; -fx-fill: null;"
            arrowHead!!.style = "-fx-stroke: black; -fx-stroke-width: $strokeWidthNormal; -fx-fill: black;"
            labelName!!.style = "-fx-text-fill: black; -fx-font-weight: normal;"
            guardLabel!!.style = "-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
            actionLabel!!.style = "-fx-text-fill: black; -fx-font-weight: normal; -fx-font-style: italic; -fx-font-size: 8;"
        }
    }

    override fun highlight(highLight: Boolean) {
        this.highLighted = highLight
        setElementStyle()
    }

    override fun visited(visit: Boolean) {
        this.visited = visit
        setElementStyle()
    }

    override fun selected(select: Boolean) {
        this.isSelected = select
        setElementStyle()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(EdgeFX::class.java)
        private val defaultArrowHeadSize = 10.0
    }
}
