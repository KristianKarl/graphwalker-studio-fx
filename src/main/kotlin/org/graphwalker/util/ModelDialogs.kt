package org.graphwalker.util

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import org.graphwalker.core.model.Action
import org.graphwalker.core.model.Edge
import org.graphwalker.core.model.Guard
import org.graphwalker.dsl.antlr.generator.GeneratorFactory
import org.graphwalker.model.GraphFX
import java.util.*

class ModelDialogs {

    fun runEdge(edge: Edge): Optional<Any>? {
        val dialog = Dialog()
        if (edge.name != null) {
            dialog.setTitle("Edge: " + edge.name)
        } else {
            dialog.setTitle("Edge properties")
        }
        dialog.setResizable(true)

        var guardStr = ""
        if (edge.guard != null && edge.guard.script != null) {
            guardStr = edge.guard.script.toString()
        }

        var actionStr = ""
        if (edge.actions != null) {
            for (action in edge.actions) {
                if (action.script != null) {
                    actionStr += action.script.toString()
                }
            }
        }

        val nameLabel = Label("Name: ")
        val guardLabel = Label("Guard: ")
        val actionLabel = Label("Action: ")
        val nameField = TextField(edge.name)
        val guardField = TextField(guardStr)
        val actionField = TextField(actionStr)

        val grid = GridPane()
        grid.add(nameLabel, 1, 1)
        grid.add(nameField, 2, 1)
        grid.add(guardLabel, 1, 2)
        grid.add(guardField, 2, 2)
        grid.add(actionLabel, 1, 3)
        grid.add(actionField, 2, 3)
        dialog.getDialogPane().setContent(grid)

        val buttonTypeCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        val buttonTypeOk = ButtonType("Okay", ButtonBar.ButtonData.OK_DONE)
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeCancel, buttonTypeOk)

        dialog.setResultConverter({ b ->
            if (b === buttonTypeOk) {
                edge.name = nameField.text
                edge.guard = Guard(guardField.text)
                val actions = ArrayList<Action>()
                actions.add(Action(actionField.text))
                edge.actions = actions
                return@dialog.setResultConverter Any ()
            }
            null
        })

        return dialog.showAndWait()
    }

    fun runModel(graph: GraphFX): Optional<Any>? {
        val dialog = Dialog<Any>()
        dialog.title = "Model: " + graph.getModel().getName()
        dialog.isResizable = true

        var actionStr = ""
        if (graph.getModel().getActions() != null) {
            for (action in graph.getModel().getActions()) {
                if (action.getScript() != null) {
                    actionStr += action.getScript().toString()
                }
            }
        }

        val nameLabel = Label("Name: ")
        val generatorLabel = Label("Generator: ")
        val actionLabel = Label("Action: ")
        val nameField = TextField(graph.getModel().getName())
        val generatorField = TextField(graph.getGenerator())
        val actionField = TextField(actionStr)

        val grid = GridPane()
        grid.add(nameLabel, 1, 1)
        grid.add(nameField, 2, 1)
        grid.add(generatorLabel, 1, 2)
        grid.add(generatorField, 2, 2)
        grid.add(actionLabel, 1, 3)
        grid.add(actionField, 2, 3)
        dialog.dialogPane.content = grid

        val buttonTypeCancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        val buttonTypeOk = ButtonType("Okay", ButtonBar.ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.addAll(buttonTypeCancel, buttonTypeOk)

        val okButton = dialog.dialogPane.lookupButton(buttonTypeOk) as Button
        //okButton.setDisable(true);
        generatorField.textProperty().addListener { observable, oldValue, newValue ->
            if (isValidGenerator(newValue.trim { it <= ' ' })) {
                okButton.isDisable = false
                generatorField.style = "-fx-text-fill: black;"
            } else {
                okButton.isDisable = true
                generatorField.style = "-fx-text-fill: red;"
            }
        }

        dialog.setResultConverter { b ->
            if (b == buttonTypeOk) {
                graph.getModel().setName(nameField.text)
                val actions = ArrayList<Action>()
                actions.add(Action(actionField.text))
                graph.getModel().setActions(actions)
                graph.setGenerator(generatorField.text)
                return@dialog.setResultConverter Any ()
            }
            null
        }

        return dialog.showAndWait()
    }

    private fun isValidGenerator(generator: String): Boolean {
        try {
            GeneratorFactory.parse(generator)
        } catch (e: Exception) {
            return false
        }

        return true
    }
}
