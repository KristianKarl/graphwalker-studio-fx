package org.graphwalker.control

import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Pane
import org.slf4j.LoggerFactory

class LabelTextField(labelName: Label) : TextField() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        labelName.isVisible = false
        text = labelName.text

        addEventHandler(KeyEvent.KEY_RELEASED) { event -> event.consume() }

        addEventFilter(KeyEvent.KEY_RELEASED) { keyEvent ->
            if (keyEvent.code == KeyCode.ENTER) {
                logger.debug("Old text: " + labelName.text)
                logger.debug("New text: $text")
                labelName.text = text
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
                (this@LabelTextField.parent as Pane).children.remove(this@LabelTextField)
            }
        }
        Platform.runLater { this.requestFocus() }
    }
}
