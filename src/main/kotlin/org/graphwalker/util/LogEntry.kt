package org.graphwalker.util

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import org.slf4j.LoggerFactory

class LogEntry(val elementNode: Node, ordinal: Int?, id: String, modelName: String, elementType: String, elementName: String, data: String) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val ordinal: SimpleIntegerProperty
    private val id: SimpleStringProperty
    private val modelName: SimpleStringProperty
    private val elementName: SimpleStringProperty
    private val elementType: SimpleStringProperty
    private val data: SimpleStringProperty

    init {
        this.ordinal = SimpleIntegerProperty(ordinal!!)
        this.id = SimpleStringProperty(id)
        this.modelName = SimpleStringProperty(modelName)
        this.elementName = SimpleStringProperty(elementName)
        this.elementType = SimpleStringProperty(elementType)
        this.data = SimpleStringProperty(data)
    }

    fun getOrdinal(): Int? {
        return ordinal.get()
    }

    fun getId(): String {
        return id.get()
    }

    fun getModelName(): String {
        return modelName.get()
    }

    fun getElementName(): String {
        return elementName.get()
    }

    fun getElementType(): String {
        return elementType.get()
    }

    fun getData(): String {
        return data.get()
    }
}
