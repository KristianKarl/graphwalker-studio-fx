package org.graphwalker.views

import javafx.geometry.Orientation
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import tornadofx.*

class PropertiesView() : View("PROPERTIES") {
    var modelName : TextField by singleAssign()
    var modelActions : TextArea by singleAssign()
    var elementName : TextField by singleAssign()

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
                field("Name") {
                    labelPosition = Orientation.VERTICAL
                    elementName= textfield {

                    }
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
                var l = label() {
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
