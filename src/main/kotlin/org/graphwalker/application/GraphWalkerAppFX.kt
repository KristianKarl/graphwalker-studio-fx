package org.graphwalker.application

import com.beust.jcommander.ParameterException
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.geometry.Bounds
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.graphwalker.core.event.EventType
import org.graphwalker.core.event.Observer
import org.graphwalker.core.machine.Context
import org.graphwalker.core.machine.ExecutionContext
import org.graphwalker.core.machine.Machine
import org.graphwalker.core.machine.SimpleMachine
import org.graphwalker.core.model.Element
import org.graphwalker.core.model.Vertex
import org.graphwalker.dsl.antlr.DslException
import org.graphwalker.dsl.antlr.generator.GeneratorFactory
import org.graphwalker.exception.UnsupportedFileFormatFX
import org.graphwalker.io.factory.ContextFactory
import org.graphwalker.io.factory.ContextFactoryException
import org.graphwalker.io.factory.ContextFactoryScanner
import org.graphwalker.io.factory.dot.DotContextFactory
import org.graphwalker.io.factory.json.JsonContextFactory
import org.graphwalker.io.factory.json.JsonModel
import org.graphwalker.io.factory.yed.YEdContextFactory
import org.graphwalker.layout.GraphWalkerLayoutFX
import org.graphwalker.model.EdgeFX
import org.graphwalker.model.ElementFX
import org.graphwalker.model.GraphFX
import org.graphwalker.model.VertexFX
import org.graphwalker.util.LogEntry
import org.graphwalker.util.LoggerUtil
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

class GraphWalkerAppFX : Application(), Observer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal val CURSOR_DEFAULT: ObjectProperty<Cursor> = SimpleObjectProperty(Cursor.DEFAULT)
    internal val CURSOR_WAIT: ObjectProperty<Cursor> = SimpleObjectProperty(Cursor.WAIT)
    private val pool = Executors.newFixedThreadPool(4)
    internal var topVBox = VBox()
    internal var currentDir: String? = null
    internal var currentFile: File? = null
    var scene: Scene? = null
        internal set
    internal var primaryStage: Stage? = null
    var tabPane: TabPane
        internal set
    internal var ordinal: Int? = null
    var executionWindow: TableView<LogEntry>? = null
        internal set
    internal var messagesWindow: TextFlow? = null
    internal var outputTabPane: TabPane? = null
    internal var executingGraphTask: Future<*>? = null
    internal var executionLock = Any()
    internal var executionSpeedTimer: Timer? = null
    internal var executionDelay: SimpleDoubleProperty
    internal var playPause: Button
    internal var stepFwd: Button
    internal var stop: Button
    internal var slider: Slider
    internal var speed: SimpleDoubleProperty

    val graphs: MutableList<GraphFX> = ArrayList<GraphFX>()
    private var listOfSharedStateLabels: MutableSet<String> = HashSet()
    private var currentGraphIndex: Int = 0
    private var startElementId: String? = null
    private var isSliderValueChanged = false

    internal var graphWalkerRunningState = GraphWalkerRunningState.stopped

    private val logEntries = FXCollections.observableArrayList<LogEntry>()

    val currentGraph: GraphFX
        get() = graphs[currentGraphIndex]

    internal enum class GraphWalkerRunningState {
        stopped,
        paused,
        running
    }

    @Throws(IOException::class)
    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        val root = BorderPane()
        scene = Scene(root, 600.0, 500.0)
        scene!!.stylesheets.add(GraphWalkerAppFX::class.java.getResource("/graphwalker-studio-fx.css").toExternalForm())
        scene!!.addEventHandler(KeyEvent.KEY_RELEASED) { keyEvent ->
            if (keyEvent.code == KeyCode.L) {
                logger.debug("Key L is pressed")
                GraphWalkerLayoutFX(currentGraph).execute()
                currentGraph.fitGraphInWindow()
            } else if (keyEvent.code == KeyCode.E) {
                logger.debug("Key E is pressed")
                GraphWalkerLayoutFX(currentGraph).doEdges()
            } else if (keyEvent.code == KeyCode.DELETE) {
                logger.debug("Key DELETE is pressed")

                val elementsToBeRemoved = HashSet<ElementFX>()
                for (vertex in currentGraph.getElements()) {
                    if (vertex.isSelected() && vertex is VertexFX) {
                        for (edge in currentGraph.getElements()) {
                            if (edge is EdgeFX && edge.getSource().getVertex().getId().equals(vertex.getElementId())) {
                                elementsToBeRemoved.add(edge)
                            }
                            if (edge is EdgeFX && edge.getTarget().getVertex().getId().equals(vertex.getElementId())) {
                                elementsToBeRemoved.add(edge)
                            }
                        }
                        elementsToBeRemoved.add(vertex)
                        currentGraph.getModel().deleteVertex(vertex.getVertex())
                    }
                }

                for (edge in currentGraph.getElements()) {
                    if (edge.isSelected() && edge is EdgeFX) {
                        elementsToBeRemoved.add(edge)
                        currentGraph.getModel().deleteEdge(edge.getEdge())
                    }
                }

                currentGraph.getElements().removeAll(elementsToBeRemoved)
                currentGraph.getContentPane().getChildren().removeAll(elementsToBeRemoved)
            }
        }

        primaryStage.scene = scene
        primaryStage.setOnCloseRequest { t ->
            Platform.exit()
            System.exit(0)
        }
        primaryStage.show()

        topVBox.children.add(addMenus())
        topVBox.children.add(addToolbar())

        tabPane = TabPane()
        tabPane.selectionModel.selectedItemProperty().addListener { ov, oldTab, newTab ->
            currentGraphIndex = tabPane.selectionModel.selectedIndex
            if (currentGraphIndex < 0) {
                return@tabPane.getSelectionModel().selectedItemProperty().addListener
            }
            executionDelay.bind(slider.valueProperty())
        }

        executionWindow = TableView<LogEntry>()
        executionWindow!!.setItems(logEntries)
        executionWindow!!.selectionModel.selectedItemProperty().addListener { obs, oldSelection, newSelection ->
            if (oldSelection != null) {
                if (oldSelection.getElementNode() is ElementFX) {
                    (oldSelection.getElementNode() as ElementFX).highlight(false)
                }
            }
            if (newSelection != null) {
                highLightElement(newSelection.getId())
            }
        }

        ordinal = 0
        val ordinalCol = TableColumn("#")
        ordinalCol.setCellValueFactory(PropertyValueFactory<LogEntry, Int>("ordinal"))
        ordinalCol.setSortable(false)
        ordinalCol.setPrefWidth(40.0)

        val modelNameCol = TableColumn("Model Name")
        modelNameCol.setCellValueFactory(PropertyValueFactory<LogEntry, String>("modelName"))
        modelNameCol.setSortable(false)
        modelNameCol.setPrefWidth(200.0)

        val elementNameCol = TableColumn("Element Name")
        elementNameCol.setCellValueFactory(PropertyValueFactory<LogEntry, String>("elementName"))
        elementNameCol.setSortable(false)
        elementNameCol.setPrefWidth(200.0)

        val dataCol = TableColumn("Data")
        dataCol.setCellValueFactory(PropertyValueFactory<LogEntry, String>("data"))
        dataCol.setSortable(false)
        dataCol.setPrefWidth(200.0)

        executionWindow!!.columns.addAll(ordinalCol, modelNameCol, elementNameCol, dataCol)

        messagesWindow = TextFlow()

        val executionOutput = Tab("Execution output")
        executionOutput.isClosable = false
        executionOutput.content = executionWindow

        val messagesOutput = Tab("Messages")
        messagesOutput.isClosable = false
        messagesOutput.content = messagesWindow

        outputTabPane = TabPane()
        outputTabPane!!.tabs.addAll(executionOutput, messagesOutput)

        val splitPane = SplitPane()
        splitPane.orientation = Orientation.VERTICAL
        splitPane.items.addAll(tabPane, outputTabPane)

        root.top = topVBox
        root.center = splitPane

        executionDelay = SimpleDoubleProperty()
        executionDelay.addListener { observable, oldValue, newValue ->
            if (graphWalkerRunningState == GraphWalkerRunningState.running) {
                logger.debug("New delay value: " + newValue.toLong())
                isSliderValueChanged = true
            }
        }
        executionDelay.bind(slider.valueProperty())

        if (modelFiles != null) {
            for (modelFile in modelFiles!!) {
                val file = File(modelFile)
                loadFile(file)
                currentFile = file
            }
        }
    }

    private fun createTab(graph: GraphFX): Tab {
        logger.debug("Creating graph: " + graph.getModel().getName())
        val tab = Tab()
        setTabLabel(graph, tab)

        tab.setOnClosed { t: Event ->
            val removeGraphIndex = graphs.indexOf(graph)
            graphs.remove(graph)
            currentGraphIndex = removeGraphIndex - 1
        }

        tab.content = graph.getRootPane()
        tabPane.tabs.add(tab)
        return tab
    }

    fun setTabLabel(graph: GraphFX, tab: Tab) {
        val label = Label(graph.getModel().getName())
        tab.graphic = label

        val textField = TextField()
        label.setOnMouseClicked { event ->
            logger.debug("Clicked")
            if (event.clickCount == 2) {
                textField.text = label.text
                tab.graphic = textField
                textField.selectAll()
                textField.requestFocus()
            }
        }

        textField.addEventHandler(KeyEvent.KEY_RELEASED) { event -> event.consume() }

        textField.setOnAction { event ->
            label.text = textField.text
            tab.graphic = label
        }

        textField.focusedProperty().addListener { observable, oldValue, newValue ->
            if (!newValue) {
                label.text = textField.text
                graph.getModel().setName(textField.text)
                tab.graphic = label
            }
        }
    }

    private fun addToolbar(): ToolBar {
        val toolBar = ToolBar()
        val layout = Button()
        val fitLayout = Button()
        playPause = Button()
        stepFwd = Button()
        stop = Button()
        slider = Slider()

        layout.id = "layout"
        fitLayout.id = "fitLayout"
        playPause.id = "playPause"
        stepFwd.id = "stepFwd"
        stop.id = "stop"

        layout.graphic = ImageView("/img/layout.png")
        fitLayout.graphic = ImageView("/img/fit.png")
        playPause.graphic = ImageView("/img/play.png")
        stepFwd.graphic = ImageView("/img/stepFwd.png")
        stop.graphic = ImageView("/img/stop.png")
        stop.isDisable = true

        layout.setOnAction { event ->
            if (currentGraphIndex >= 0) {
                GraphWalkerLayoutFX(currentGraph).execute()
                currentGraph.fitGraphInWindow()
            }
        }

        fitLayout.setOnAction { event ->
            if (currentGraphIndex < 0) {
                return@fitLayout.setOnAction
            }
            currentGraph.fitGraphInWindow()
        }

        playPause.setOnAction { event ->
            if (currentGraphIndex < 0) {
                return@playPause.setOnAction
            }
            when (graphWalkerRunningState) {
                GraphWalkerAppFX.GraphWalkerRunningState.stopped -> {
                    resettingGraphs()
                    stop.isDisable = false
                    stepFwd.isDisable = true
                    playPause.graphic = ImageView("/img/pause.png")
                    startExecutionTimerContinuously(slider.value.toLong())
                    runGraphWalker()
                    graphWalkerRunningState = GraphWalkerRunningState.running
                }

                GraphWalkerAppFX.GraphWalkerRunningState.paused -> {
                    stop.isDisable = false
                    stepFwd.isDisable = true
                    playPause.graphic = ImageView("/img/pause.png")
                    startExecutionTimerContinuously(slider.value.toLong())
                    graphWalkerRunningState = GraphWalkerRunningState.running
                }

                GraphWalkerAppFX.GraphWalkerRunningState.running -> {
                    stopExecutionTimer()
                    stop.isDisable = false
                    stepFwd.isDisable = false
                    playPause.graphic = ImageView("/img/play.png")
                    graphWalkerRunningState = GraphWalkerRunningState.paused
                }

                else -> {
                }
            }
        }

        stepFwd.setOnAction { event ->
            graphWalkerRunningState = GraphWalkerRunningState.paused
            if (currentGraphIndex < 0) {
                return@stepFwd.setOnAction
            }
            when (graphWalkerRunningState) {
                GraphWalkerAppFX.GraphWalkerRunningState.stopped -> {
                    resettingGraphs()
                    stop.isDisable = false
                    stepFwd.isDisable = false
                    playPause.graphic = ImageView("/img/play.png")
                    startExecutionTimerOnce()
                    runGraphWalker()
                }

                GraphWalkerAppFX.GraphWalkerRunningState.paused -> {
                    stop.isDisable = false
                    stepFwd.isDisable = false
                    playPause.graphic = ImageView("/img/pause.png")
                    startExecutionTimerOnce()
                }

                GraphWalkerAppFX.GraphWalkerRunningState.running -> {
                }

                else -> {
                }
            }
        }

        stop.setOnAction { event ->
            graphWalkerRunningState = GraphWalkerRunningState.stopped
            if (currentGraphIndex < 0) {
                return@stop.setOnAction
            }
            when (graphWalkerRunningState) {
                GraphWalkerAppFX.GraphWalkerRunningState.stopped, GraphWalkerAppFX.GraphWalkerRunningState.paused, GraphWalkerAppFX.GraphWalkerRunningState.running -> {
                    stopExecutionTimer()
                    stepFwd.isDisable = false
                    stop.isDisable = true
                    executingGraphTask!!.cancel(true)
                    playPause.graphic = ImageView("/img/play.png")
                    resettingGraphs()
                }

                else -> {
                }
            }
        }

        slider.min = 0.0
        slider.max = 1000.0
        slider.value = 500.0
        slider.isShowTickLabels = true
        slider.isShowTickMarks = true
        slider.majorTickUnit = 200.0
        slider.minorTickCount = 100
        slider.blockIncrement = 100.0
        slider.tooltip = Tooltip("Execution step delay in ms")

        speed = SimpleDoubleProperty()
        speed.bind(slider.valueProperty())

        val separator = Separator()
        separator.orientation = Orientation.VERTICAL

        toolBar.items.addAll(layout, fitLayout, separator, playPause, stepFwd, stop, slider)
        return toolBar
    }

    private fun runGraphWalker() {
        executionWindow!!.items.clear()
        messagesWindow!!.children.clear()
        if (currentGraphIndex < 0) {
            return
        }
        val task = object : Task<Void>() {
            @Throws(Exception::class)
            override fun call(): Void? {
                logger.debug("Running the graph!")

                val contexts = ArrayList<Context>()
                for (graph in graphs) {
                    graph.getModel().getEdges().clear()
                    graph.getModel().getVertices().clear()
                    for (element in graph.getElements()) {
                        if (element is VertexFX) {
                            graph.getModel().addVertex(element.getVertex())
                        }
                        if (element is EdgeFX) {
                            graph.getModel().addEdge(element.getEdge())
                        }
                    }
                    val context = UIExecutionContext(graph.getModel().build(),
                            GeneratorFactory.parse(graph.getGenerator()))
                    val startElement = getElement(graph, startElementId)
                    if (startElement != null) {
                        context.setNextElement(startElement)
                    }
                    contexts.add(context)
                }

                val machine = SimpleMachine(contexts)
                machine.addObserver(this@GraphWalkerAppFX)
                while (machine.hasNextStep()) {
                    machine.nextStep
                }
                return null
            }
        }
        task.setOnFailed { handle ->
            val throwable = task.exception
            addMessage(throwable.message)
            throwable.printStackTrace()
            stopExecutionTimer()
            graphWalkerRunningState = GraphWalkerRunningState.stopped
            stepFwd.isDisable = false
            playPause.graphic = ImageView("/img/play.png")
        }
        task.setOnSucceeded { handle ->
            logger.debug("Done running the graph")
            stopExecutionTimer()
            graphWalkerRunningState = GraphWalkerRunningState.stopped
            stepFwd.isDisable = false
            playPause.graphic = ImageView("/img/play.png")
        }
        executingGraphTask = pool.submit(task)
    }

    private fun stopExecutionTimer() {
        logger.debug("Stopping execution timer")
        if (executionSpeedTimer != null) {
            executionSpeedTimer!!.cancel()
            executionSpeedTimer = null
        }
    }

    private fun startExecutionTimerContinuously(delay: Long) {
        var delay = delay
        if (delay < 1) {
            delay = 1
        }
        logger.debug("Start continuously running execution timer")
        executionSpeedTimer = Timer()
        executionSpeedTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                synchronized(executionLock) {
                    executionLock.notify()
                }
            }
        }, 0, delay)
    }

    private fun startExecutionTimerOnce() {
        executionSpeedTimer = Timer()
        executionSpeedTimer!!.schedule(object : TimerTask() {
            override fun run() {
                synchronized(executionLock) {
                    logger.debug(Thread.currentThread().name + " notifier runGuardAndAction")
                    executionLock.notify()
                }
            }
        }, 0)
    }

    private fun getElement(graph: GraphFX, elementId: String?): Element? {
        for (vertex in graph.getModel().getVertices()) {
            if (vertex.getId() != null && vertex.getId() == elementId) {
                return vertex.build()
            }
        }
        for (edge in graph.getModel().getEdges()) {
            if (edge.getId() == elementId) {
                return edge.build()
            }
        }
        return null
    }

    private fun resettingGraphs() {
        ordinal = 0
        executionWindow!!.items.clear()
        messagesWindow!!.children.clear()
        for (graph in graphs) {
            for (element in graph.getElements()) {
                if (element is VertexFX) {
                    if (isNotNullOrEmpty(element.getVertex().getName()) && !element.getVertex().getName().equals("Start")) {
                        element.selected(false)
                        element.visited(false)
                        element.highlight(false)
                        if (isNotNullOrEmpty(startElementId) && startElementId == element.getElementId()) {
                            element.setStartElement(true)
                        }
                    }
                }
            }
            for (element in graph.getElements()) {
                if (element is EdgeFX) {
                    element.selected(false)
                    element.visited(false)
                    element.highlight(false)
                    if (isNotNullOrEmpty(startElementId) && startElementId == element.getElementId()) {
                        element.setStartElement(true)
                    }
                }
            }
        }
    }

    private fun addMenus(): MenuBar {
        val menuBar = MenuBar()
        menuBar.menus.addAll(addFileMenu(menuBar), addModelMenu(menuBar))
        return menuBar
    }

    private fun addModelMenu(menuBar: MenuBar): Menu {
        val menu = Menu("Model")
        menu.id = "modelMenu"

        val addModelItem = MenuItem("Add model")
        addModelItem.id = "addModel"

        addModelItem.setOnAction { Event: ActionEvent ->
            val graph = GraphFX(this)
            graph.getModel().setName("New Model")
            graphs.add(graph)
            createTab(graph)
            currentGraphIndex = graphs.indexOf(graph)
            tabPane.selectionModel.select(currentGraphIndex)
        }

        menu.items.addAll(addModelItem)

        return menu
    }

    private fun addFileMenu(menuBar: MenuBar): Menu {
        val menu = Menu("File")
        menu.id = "fileMenu"

        val newItem = MenuItem("New")
        newItem.id = "fileNew"

        val openItem = MenuItem("Open...")
        openItem.id = "fileOpen"

        val saveItem = MenuItem("Save")
        saveItem.id = "saveOpen"

        val saveAsItem = MenuItem("Save as...")
        saveAsItem.id = "saveAsOpen"

        val exitItem = MenuItem("Exit")
        exitItem.id = "fileExit"

        menu.items.addAll(newItem, openItem, saveItem, saveAsItem, exitItem)

        newItem.setOnAction { Event: ActionEvent -> clearAll() }

        openItem.setOnAction { Event: ActionEvent ->
            if (currentDir == null) {
                currentDir = System.getProperty("user.dir") + File.separator
            }
            val fileChooser = FileChooser()
            fileChooser.title = "Open from file"
            fileChooser.initialDirectory = File(currentDir!!)
            fileChooser.extensionFilters.addAll(
                    FileChooser.ExtensionFilter("Graphwalker Files", "*.gw3", "*.json"),
                    FileChooser.ExtensionFilter("Graphml Files", "*.graphml"),
                    FileChooser.ExtensionFilter("Grapviz Dot Files", "*.dot"),
                    FileChooser.ExtensionFilter("All Files", "*.*"))
            val selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage)
            if (selectedFiles != null && selectedFiles.size > 0) {
                currentDir = selectedFiles[0].parent
                logger.debug("Number of files to open: " + selectedFiles.size)

                for (file in selectedFiles) {
                    loadFile(file)
                    currentFile = file
                }
            }
        }

        saveItem.setOnAction { Event: ActionEvent ->
            if (currentFile != null && currentFile!!.canWrite()) {
                saveFile(currentFile)
            } else {
                askForFileToSave()
            }
        }

        saveAsItem.setOnAction { Event: ActionEvent -> askForFileToSave() }

        exitItem.setOnAction { Event -> System.exit(0) }

        return menu
    }

    private fun askForFileToSave() {
        if (currentDir == null) {
            currentDir = System.getProperty("user.dir") + File.separator
        }
        val fileChooser = FileChooser()
        fileChooser.title = "Save to file"
        fileChooser.initialDirectory = File(currentDir!!)
        currentFile = fileChooser.showSaveDialog(primaryStage)
        if (currentFile != null) {
            currentDir = currentFile!!.parent
            saveFile(currentFile)
        }
    }

    private fun loadFile(modelFile: File) {
        scene!!.cursor = Cursor.WAIT
        val graphsLoadedByFile = ArrayList<GraphFX>()

        val task = object : Task<Void>() {

            @Throws(Exception::class)
            override fun call(): Void? {
                logger.debug("Open from file: " + modelFile.path)
                val factory: ContextFactory

                try {
                    /**
                     * Faster load times than using the FactoryScanner
                     */
                    if (YEdContextFactory().accept(modelFile.toPath())) {
                        factory = YEdContextFactory()
                    } else if (JsonContextFactory().accept(modelFile.toPath())) {
                        factory = JsonContextFactory()
                    } else if (DotContextFactory().accept(modelFile.toPath())) {
                        factory = DotContextFactory()
                    } else {
                        throw UnsupportedFileFormatFX(modelFile.path)
                    }

                    val localContexts = factory.create(modelFile.toPath())
                    for (context in localContexts) {
                        val jsonModel = JsonModel()
                        jsonModel.setModel(context.model)
                        val graph = GraphFX(this@GraphWalkerAppFX)
                        graph.setModel(jsonModel.model)
                        if (context.pathGenerator != null) {
                            Platform.runLater { graph.setGenerator(context.pathGenerator.toString()) }
                        }
                        graphs.add(graph)
                        graphsLoadedByFile.add(graph)
                        if (context.nextElement != null) {
                            setElementStartId(context.nextElement.id)
                        }

                    }
                } catch (e: DslException) {
                    addMessage(e.message)
                    e.printStackTrace()
                } catch (e: IOException) {
                    addMessage(e.message)
                    e.printStackTrace()
                } catch (unsupportedFileFormatFX: UnsupportedFileFormatFX) {
                    addMessage(unsupportedFileFormatFX.getMessage())
                    unsupportedFileFormatFX.printStackTrace()
                } catch (e: ContextFactoryException) {
                    addMessage(e.message)
                    e.printStackTrace()
                }

                return null
            }
        }

        task.setOnFailed { handle ->
            scene!!.cursor = Cursor.DEFAULT
            val throwable = task.exception
            addMessage(throwable.message)
            throwable.printStackTrace()
        }

        task.setOnSucceeded { event ->
            for (graph in graphsLoadedByFile) {
                createTab(graph)
                addGraphComponents(graph)
                GraphWalkerLayoutFX(graph).doEdges()
                graph.fitGraphInWindow()
                graph.getScrollPane().viewportBoundsProperty().addListener(object : ChangeListener<Bounds> {
                    override fun changed(observable: ObservableValue<out Bounds>, oldValue: Bounds, newValue: Bounds) {
                        logger.debug("New bound: $newValue")
                        graph.fitGraphInWindow()
                        Platform.runLater { graph.getScrollPane().requestLayout() }
                        graph.getScrollPane().viewportBoundsProperty().removeListener(this)
                    }
                })
            }
            graphsLoadedByFile.clear()
            scene!!.cursor = Cursor.DEFAULT
        }

        val future = pool.submit(task)
        try {
            // Wait until loading of file is done.
            future.get()
        } catch (e: InterruptedException) {
            logger.error(e.message)
        } catch (e: ExecutionException) {
            logger.error(e.message)
        }

    }

    private fun addMessage(message: String) {
        val text = Text(message)
        messagesWindow!!.children.add(text)
        outputTabPane!!.selectionModel.select(1)
    }

    private fun clearAll() {
        startElementId = null
        tabPane.tabs.clear()
        graphs.clear()
        listOfSharedStateLabels.clear()
        executionWindow!!.items.clear()
        messagesWindow!!.children.clear()
    }

    private fun saveFile(modelFile: File) {
        scene!!.cursor = Cursor.WAIT
        val task = object : Task<Void>() {
            @Throws(Exception::class)
            override fun call(): Void? {
                logger.debug("Save to file: " + modelFile.path)

                val contexts = ArrayList<Context>()
                for (graph in graphs) {
                    val context = object : ExecutionContext() {
                    }
                    graph.getModel().getEdges().clear()
                    graph.getModel().getVertices().clear()
                    for (element in graph.getElements()) {
                        if (element is VertexFX) {
                            graph.getModel().addVertex(element.getVertex())
                        } else if (element is EdgeFX) {
                            graph.getModel().addEdge(element.getEdge())
                        }
                    }

                    context.model = graph.getModel().build()
                    context.pathGenerator = GeneratorFactory.parse(graph.getGenerator())
                    val startElement = getElement(graph, startElementId)
                    if (startElement != null) {
                        context.nextElement = startElement
                    }
                    contexts.add(context)
                }
                val outputFactory = ContextFactoryScanner.get(modelFile.toPath())

                PrintWriter(modelFile).use { out -> out.println(outputFactory.getAsString(contexts)) }

                return null
            }
        }

        task.setOnFailed { handle ->
            scene!!.cursor = Cursor.DEFAULT
            val throwable = task.exception
            addMessage(throwable.message)
            throwable.printStackTrace()
        }
        task.setOnSucceeded { event ->
            scene!!.cursor = Cursor.DEFAULT
            logger.debug("Saving file to: " + currentFile!!.path + ", successful")
        }
        pool.submit(task)
    }

    fun addGraphComponents(graph: GraphFX) {
        logger.debug("Adding graphical components")

        var minX = 0.0
        var minY = 0.0

        for (vertex in graph.getModel().getVertices()) {
            val VertexFX = VertexFX(graph, vertex)

            if (isNotNullOrEmpty(vertex.getSharedState())) {
                listOfSharedStateLabels.add(vertex.getSharedState())
            }

            graph.addVertex(VertexFX)
            if (isNotNullOrEmpty(startElementId) && startElementId == vertex.getId()) {
                VertexFX.setStartElement(true)
            }

            if (VertexFX.getX().getValue() < minX) {
                minX = VertexFX.getX().getValue()
            }
            if (VertexFX.getY().getValue() < minY) {
                minY = VertexFX.getY().getValue()
            }
        }

        for (element in graph.getElements()) {
            if (element is VertexFX) {
                element.relocate(element.getX().getValue() - minX + 5e5,
                        element.getY().getValue() - minY + 5e5)
            }
        }

        for (edge in graph.getModel().getEdges()) {
            val sourceVertex: VertexFX?
            val targetVertex = findVertex(graph, edge.getTargetVertex())
            if (edge.getSourceVertex() == null) {
                sourceVertex = UIStartVertex(graph)
                sourceVertex!!.relocate(targetVertex!!.getX().getValue(), targetVertex.getX().getValue() - 200)
                graph.addVertex(sourceVertex)
            } else {
                sourceVertex = findVertex(graph, edge.getSourceVertex())
            }
            val EdgeFX = EdgeFX(this,
                    sourceVertex,
                    targetVertex,
                    edge)
            if (isNotNullOrEmpty(startElementId) && startElementId == edge.getId()) {
                EdgeFX.setStartElement(true)
            }
            graph.addEdge(EdgeFX)
        }
    }

    private fun findVertex(graph: GraphFX, sourceVertex: Vertex): VertexFX? {
        for (element in graph.getElements()) {
            if (element is VertexFX) {
                if (sourceVertex.id == element.getElementId()) {
                    return element
                }
            }
        }
        logger.warn("Did not find vertex for: $sourceVertex")
        return null
    }

    fun highLightElement(id: String) {
        logger.debug("Higlight: $id")
        for (graph in graphs) {
            for (element in graph.getElements()) {
                if (id == element.getElementId()) {
                    currentGraphIndex = graphs.indexOf(graph)
                    executionDelay.bind(slider.valueProperty())
                    tabPane.selectionModel.select(currentGraphIndex)
                    element.highlight(true)
                    centerNodeInScrollPane(graph.getScrollPane(), element as Node)
                    return
                }
            }
        }
    }

    /**
     * This method is called by a worker thread. So calls to the UI thread needs to be wrapped
     * in Platform.runLater calls
     */
    override fun update(machine: Machine, element: Element, type: EventType) {
        logger.debug("Received an update from a GraphWalker machine")
        logger.debug("  Current model: " + machine.currentContext.model.name)
        logger.debug("   Element name: " + if (isNotNullOrEmpty(element.name)) element.name else "")
        logger.debug("     Event type: " + type.name)
        if (graphWalkerRunningState == GraphWalkerRunningState.stopped) {
            return
        }
        if (type == EventType.BEFORE_ELEMENT) {
            for (graph in graphs) {
                for (ElementFX in graph.getElements()) {
                    if (element.id == ElementFX.getElementId()) {
                        currentGraphIndex = graphs.indexOf(graph)
                        executionDelay.bind(slider.valueProperty())
                        tabPane.selectionModel.select(currentGraphIndex)

                        val jsonObject = JSONObject()
                        for ((key1, value) in machine.currentContext.keys) {
                            jsonObject.put(key1, value)
                        }

                        logEntries.add(LogEntry(ElementFX as Node,
                                ++ordinal,
                                ElementFX.getElementId(),
                                graph.getModel().getName(),
                                "Vertex",
                                ElementFX.getElementName(),
                                jsonObject.toString()))
                        Platform.runLater {
                            ElementFX.highlight(true)
                            executionWindow!!.scrollTo(logEntries.size - 1)
                            outputTabPane!!.selectionModel.select(0)
                        }
                    }
                }
            }
            synchronized(executionLock) {
                logger.debug(Thread.currentThread().name + " waiting to get notified")
                try {
                    executionLock.wait()
                    if (isSliderValueChanged) {
                        stopExecutionTimer()
                        startExecutionTimerContinuously(slider.value.toLong())
                        isSliderValueChanged = false
                    }
                } catch (e: InterruptedException) {
                    logger.debug(Thread.currentThread().name + " interrupted")
                    return
                }

                logger.debug(Thread.currentThread().name + " got notified")
            }
        } else if (type == EventType.AFTER_ELEMENT) {
            for (graph in graphs) {
                for (ElementFX in graph.getElements()) {
                    if (element.id == ElementFX.getElementId()) {
                        Platform.runLater {
                            currentGraphIndex = graphs.indexOf(graph)
                            executionDelay.bind(slider.valueProperty())
                            tabPane.selectionModel.select(currentGraphIndex)
                            ElementFX.highlight(false)
                            ElementFX.visited(true)
                        }
                    }
                }
            }
        }
    }

    fun setElementStartId(currentElement: Node) {
        for (graph in graphs) {
            for (node in graph.getContentPane().getChildren()) {
                if (node === currentElement) {
                    startElementId = (node as ElementFX).getElementId()
                    continue
                }
                (node as ElementFX).setStartElement(false)
            }
        }
    }

    fun centerNodeInScrollPane(scrollPane: ZoomableScrollPane, node: Node) {
        var x = 0.0
        var y = 0.0

        if (node is VertexFX) {
            x = node.boundsInParent.minX
            y = node.boundsInParent.minY
        } else if (node is EdgeFX) {
            val label = node.getLabelName()
            x = label.getBoundsInParent().getMinX()
            y = label.getBoundsInParent().getMinY()
        }

        scrollPane.setVvalue(y / 1e6)
        scrollPane.setHvalue(x / 1e6)
    }

    fun setElementStartId(id: String) {
        startElementId = id
        for (graph in graphs) {
            for (node in graph.getContentPane().getChildren()) {
                if ((node as ElementFX).getElementId().equals(startElementId)) {
                    (node as ElementFX).setStartElement(true)
                } else {
                    (node as ElementFX).setStartElement(false)
                }
            }
        }
    }

    fun getListOfSharedStateLabels(): Set<String> {
        return listOfSharedStateLabels
    }

    fun setListOfSharedStateLabels(listOfSharedStateLabels: MutableSet<String>) {
        this.listOfSharedStateLabels = listOfSharedStateLabels
    }

    companion object {

        private var modelFiles: List<String>? = ArrayList()

        @JvmStatic
        fun main(args: Array<String>) {
            var options = Options()
            var jc = JCommander(options)
            jc.setProgramName("java -jar graphwalker.jar")
            try {
                jc.parseWithoutValidation(args)
            } catch (e: Exception) {
                // ignore
            }

            try {
                setLogLevel(options)

                if (options.help) {
                    options = Options()
                    jc = JCommander(options)
                    jc.parse(args)
                    jc.usage()
                    System.exit(0)
                } else if (options.version) {
                    println(printVersionInformation())
                    System.exit(0)
                } else if (!options.modelFiles.isEmpty()) {
                    modelFiles = options.modelFiles
                }

                // Need to instantiate options again to avoid
                // ParameterException "Can only specify option --debug once."
                options = Options()
                jc = JCommander(options)
                jc.parse(args)
                Application.launch(*args)

            } catch (e: MissingCommandException) {
                System.err.println(e.getMessage() + System.lineSeparator())
            } catch (e: ParameterException) {
                System.err.println("An error occurred when running command: " + StringUtils.join(args, " "))
                System.err.println(e.getMessage() + System.lineSeparator())
                if (jc.getParsedCommand() != null) {
                    jc.usage(jc.getParsedCommand())
                }
            } catch (e: Exception) {
                System.err.println("An error occurred when running command: " + StringUtils.join(args, " "))
                System.err.println(e.message + System.lineSeparator())
                logger.error("An error occurred when running command: " + StringUtils.join(args, " "), e)
            }

        }

        private fun setLogLevel(options: Options) {
            // OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
            if (options.debug.equalsIgnoreCase("OFF")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.OFF)
            } else if (options.debug.equalsIgnoreCase("ERROR")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.ERROR)
            } else if (options.debug.equalsIgnoreCase("WARN")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.WARN)
            } else if (options.debug.equalsIgnoreCase("INFO")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.INFO)
            } else if (options.debug.equalsIgnoreCase("DEBUG")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.DEBUG)
            } else if (options.debug.equalsIgnoreCase("TRACE")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.TRACE)
            } else if (options.debug.equalsIgnoreCase("ALL")) {
                LoggerUtil.setLogLevel(LoggerUtil.Level.ALL)
            } else {
                throw ParameterException("Incorrect argument to --debug")
            }
        }

        private fun printVersionInformation(): String {
            var version = "org.graphwalker version: " + versionString + System.getProperty("line.separator")
            version += System.getProperty("line.separator")

            version += "org.graphwalker is open source software licensed under MIT license" + System.getProperty("line.separator")
            version += "The software (and it's source) can be downloaded from http://graphwalker.org" + System.getProperty("line.separator")
            version += "For a complete list of this package software dependencies, see http://graphwalker.org/archive/site/graphwalker-cli/dependencies.html" + System
                    .getProperty("line.separator")

            return version
        }

        private val versionString: String
            get() {
                val properties = Properties()
                val inputStream = GraphWalkerAppFX::class.java.getResourceAsStream("/version.properties")
                if (null != inputStream) {
                    try {
                        properties.load(inputStream)
                    } catch (e: IOException) {
                        logger.error("An error occurred when trying to get the version string", e)
                        return "unknown"
                    } finally {
                        IOUtils.closeQuietly(inputStream)
                    }
                }
                return properties.getProperty("graphwalker.version")
            }
    }
}
