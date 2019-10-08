package org.graphwalker.views

import javafx.scene.paint.Color
import tornadofx.*
import tornadofx.Stylesheet.Companion.tab

class ModelEditor(title: String) : View(title) {
    override val root = borderpane {
        center {
            style {
                backgroundColor += Color.WHITESMOKE
            }
        }
    }
}