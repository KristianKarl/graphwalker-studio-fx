package org.graphwalker.control

import javafx.application.Platform
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import org.slf4j.LoggerFactory

class LabelComboBox(labelName: Label, listOfSharedStates: MutableSet<String>) : ComboBox<String>() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        labelName.isVisible = false
        isEditable = true

        items.addAll(listOfSharedStates)
        for (sharedState in listOfSharedStates) {
            if (labelName.text == sharedState) {
                selectionModel.select(sharedState)
            }
        }

        addEventHandler(KeyEvent.KEY_RELEASED) { event -> event.consume() }

        addEventFilter(KeyEvent.KEY_RELEASED) { keyEvent ->
            if (keyEvent.code == KeyCode.ENTER) {
                logger.debug("Old text: " + labelName.text)
                logger.debug("New text: " + selectionModel.selectedItem)
                labelName.text = selectionModel.selectedItem
                listOfSharedStates.add(selectionModel.selectedItem)
                isFocused = false
            } else if (keyEvent.code == KeyCode.ESCAPE) {
                logger.debug("Escaped pressed- Keeping old text")
                isFocused = false
            }
        }

        focusedProperty().addListener { arg0, oldPropertyValue, newPropertyValue ->
            if (newPropertyValue!!) {
                logger.debug("Text field on focus")
            } else {
                println("Text field out focus")
                labelName.isVisible = true
                (this@LabelComboBox.parent as Pane).children.remove(this@LabelComboBox)
            }
        }
        Platform.runLater { this.requestFocus() }
    }
}

