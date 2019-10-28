package org.graphwalker.controller

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import org.graphwalker.event.DisableElementProperties
import org.graphwalker.event.EdgeSelectedEvent
import org.graphwalker.event.EnableElementProperties
import org.graphwalker.event.VertexSelectedEvent
import org.slf4j.LoggerFactory
import tornadofx.*

class PropertyController : Controller() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val dataForElementName = SimpleStringProperty()
    val dataForElementEnable = SimpleBooleanProperty()

    init {
        subscribe<EnableElementProperties> {
            logger.debug("DisableElementProperties received")
            dataForElementEnable.value = false
        }
        subscribe<DisableElementProperties> {
            logger.debug("DisableElementProperties received")
            dataForElementEnable.value = true
            dataForElementName.value = ""
        }
        subscribe<VertexSelectedEvent> { event ->
            logger.debug("VertexSelectedEvent received")
            dataForElementEnable.value = false
            dataForElementName.value = event.vertex.jsonVertex.vertex.name
        }
        subscribe<EdgeSelectedEvent> { event ->
            logger.debug("EdgeSelectedEvent received")
            dataForElementEnable.value = false
            dataForElementName.value = event.edge.jsonEdge.edge.name
        }
    }
}