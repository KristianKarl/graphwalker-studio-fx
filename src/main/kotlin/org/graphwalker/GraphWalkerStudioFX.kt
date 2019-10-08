package org.graphwalker

import javafx.application.Application
import org.graphwalker.views.GraphWalkerStudioView
import tornadofx.*

class GraphWalkerStudioFX : App(GraphWalkerStudioView::class, Styles::class) {
    init {
        importStylesheet(Styles::class)
        reloadStylesheetsOnFocus()
    }
}

fun main(args: Array<String>) {
    Application.launch(GraphWalkerStudioFX::class.java, *args)
}
