package org.graphwalker.views

import javafx.geometry.Orientation
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import org.graphwalker.controller.PropertyController
import org.slf4j.LoggerFactory
import tornadofx.*

class PropertiesView : View() {
    private val logger = LoggerFactory.getLogger(this::class.java)
    val controller: PropertyController by inject()

    var modelName: TextField by singleAssign()
    var modelActions: TextArea by singleAssign()

    init {
//        find(PropertyController::class)
    }

    override val root = scrollpane {
        maxWidth = 300.0
        form {
            maxWidth = 280.0
            fieldset("MODEL") {
                field("Name") {
                    labelPosition = Orientation.VERTICAL
                    modelName = textfield {

                    }
                }
                field("Actions") {
                    labelPosition = Orientation.VERTICAL
                    modelActions = textarea {

                    }
                }
            }
            fieldset("ELEMENT") {
                disableProperty().bind(controller.dataForElementEnable)
                field("Name") {
                    labelPosition = Orientation.VERTICAL
                    textfield(controller.dataForElementName)
                }
                field("Shared Name") {
                    labelPosition = Orientation.VERTICAL
                    textfield {

                    }
                }
                field("Guard") {
                    labelPosition = Orientation.VERTICAL
                    textfield {

                    }
                }
                field("Actions") {
                    labelPosition = Orientation.VERTICAL
                    textarea {

                    }
                }
                field("Requirements") {
                    labelPosition = Orientation.VERTICAL
                    textarea {

                    }
                }
                togglebutton("Start Element") {
                }
            }
            fieldset("EXECUTION") {
                field("Generator") {
                    labelPosition = Orientation.VERTICAL
                    textfield {

                    }
                }
                var l = label {
                    text = "Delay 0 ms"
                }
                var s = slider(0, 1000) {
                    isShowTickMarks = true
                    isShowTickLabels = true
                    valueProperty().addListener { observable, oldValue, newValue ->
                        l.text = "Delay ${newValue.toInt()} ms"
                    }
                }
            }
        }
    }
}
