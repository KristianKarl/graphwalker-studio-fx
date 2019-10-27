package org.graphwalker.model

import javafx.geometry.Point2D
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.graphwalker.io.factory.json.JsonVertex
import org.graphwalker.views.PropertiesView
import tornadofx.*

class VertexFX(vertex: JsonVertex) : StackPane() {
    val jsonVertex = vertex
    var text: Label
    var rect: Rectangle
    var gvid: Int

    init {
        gvid = -1

        rect = rectangle {
            fill = Color.LIGHTBLUE
            height = 20.0
            width = 50.0
            strokeWidth = 1.0
            stroke = Color.BLACK
        }

        text = label {
            text = vertex.vertex.name
            font = Font.font("DejaVu Sans Mono", 16.0)

            textProperty().addListener { obs, old, new ->
                jsonVertex.vertex.name = new
            }
        }

        if (jsonVertex.vertex.hasProperty("x") && jsonVertex.vertex.hasProperty("y")) {
            layoutX = jsonVertex.vertex.getProperty("x").toString().toDouble()
            layoutY = jsonVertex.vertex.getProperty("y").toString().toDouble()
        } else {
            layoutX = 0.0
            layoutY = 0.0
        }

        layoutXProperty().addListener { obs, old, new ->
            jsonVertex.vertex.setProperty("x", new)
        }
        layoutYProperty().addListener { obs, old, new ->
            jsonVertex.vertex.setProperty("y", new)
        }
    }

    fun getCenterPoint(): Point2D {
        return Point2D(layoutX + rect.width.div(2),
                layoutY + rect.height.div(2))
    }

    fun select() {
        rect.strokeWidth = 3.0
        text.font = Font.font("DejaVu Sans Mono", FontWeight.BOLD, 16.0)
    }

    fun unselect() {
        rect.strokeWidth = 1.0
        text.font = Font.font("DejaVu Sans Mono", FontWeight.NORMAL, 16.0)
    }

    fun bindElementPropertyData(propertyView: PropertiesView) {
        propertyView.elementName.bind(text.toProperty())
    }
}