package org.graphwalker.views


import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.scene.control.TabPane
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import org.graphwalker.controller.TimeoutController
import org.graphwalker.core.machine.Context
import org.graphwalker.dsl.antlr.generator.GeneratorFactory
import org.graphwalker.exceptions.UnsupportedFileFormat
import org.graphwalker.io.factory.ContextFactory
import org.graphwalker.io.factory.ContextFactoryScanner
import org.graphwalker.io.factory.dot.DotContextFactory
import org.graphwalker.io.factory.json.JsonContextFactory
import org.graphwalker.io.factory.json.JsonModel
import org.graphwalker.io.factory.yed.YEdContextFactory
import org.graphwalker.java.test.TestExecutor
import org.graphwalker.observer.ExecutionObserver
import org.graphwalker.observer.ProgressEvent
import org.graphwalker.observer.SelectModelEditor
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.File
import java.io.PrintWriter

class LoadModelsFromFileEvent(val modelFile: File) : FXEvent()
class LoadedModelsFromFileEvent : FXEvent()

class NevModelEditorEvent(val modelEditor: ModelEditor) : FXEvent()

class RunModelsEvent : FXEvent()
class RunModelsDoneEvent : FXEvent()
class RunModelsStopEvent : FXEvent()

class ModelsAreChangedEvent : FXEvent()
class ModelsAreSavedEvent : FXEvent()
class ClearAllModelsEvent : FXEvent()

class GraphWalkerStudioView : View("GraphWalker Studio FX") {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var tabs: TabPane by singleAssign()
    private var modelEditors = ArrayList<ModelEditor>()
    private var startElementId = String()

    private val status: TaskStatus by inject()
    private val controller: TimeoutController by inject()

    override val root = borderpane {
        logger.debug(javafx.scene.text.Font.getFamilies().toString())

        prefHeight = 600.0
        prefWidth = 800.0

        var numOfModels = 0

        left = vbox {
            style {
                backgroundColor += Color.BLACK
            }

            /**
             * Add model button
             */
            button {
                graphic = icon(FontAwesomeIcon.PLUS)
                action {
                    logger.debug("Open a new model editor")
                    numOfModels++
                    logger.debug("NevModelEditorEvent fired")
                    fire(NevModelEditorEvent(ModelEditor("Untitled-$numOfModels")))
                }
                style {
                    backgroundColor += Color.BLACK
                }
            }

            /**
             * Open file button
             */
            button {
                graphic = icon(FontAwesomeIcon.FOLDER_OPEN_ALT)
                style {
                    backgroundColor += Color.BLACK
                }
                action {
                    logger.debug("ClearAllModelsEvent fired")
                    fire(ClearAllModelsEvent())

                    val fileNames = chooseFile(title = "Open GraphWalker model",
                            filters = arrayOf(FileChooser.ExtensionFilter("GraphWalker", "*.json"),
                                    FileChooser.ExtensionFilter("Graphml - yEd", "*.graphml")),
                            mode = FileChooserMode.Single)
                    if (fileNames.isNotEmpty()) {
                        logger.debug("LoadModelsFromFileEvent fired")
                        fire(LoadModelsFromFileEvent(fileNames[0]))
                    }
                }
            }

            /**
             * Save file button
             */
            button {
                graphic = icon(FontAwesomeIcon.SAVE)
                disableProperty().set(true)
                action {
                    val fileNames = chooseFile(title = "Save GraphWalker model as",
                            filters = arrayOf(FileChooser.ExtensionFilter("GraphWalker file", "*.json")),
                            mode = FileChooserMode.Save)
                    if (fileNames.isNotEmpty()) {
                        val contexts = createContexts()
                        val outputFactory = ContextFactoryScanner.get(fileNames[0].toPath())
                        PrintWriter(fileNames[0]).use { out -> out.println(outputFactory.getAsString(contexts)) }
                        logger.debug("ModelsAreSavedEvent fired")
                        fire(ModelsAreSavedEvent())
                    }
                }
                style {
                    backgroundColor += Color.BLACK
                }
                subscribe<ModelsAreChangedEvent> { event ->
                    logger.debug("ModelsAreChangedEvent received")
                    disableProperty().set(false)
                }
            }

            separator()

            /**
             * Play button
             */
            button {
                graphic = icon(FontAwesomeIcon.PLAY)
                tooltip("Dry run by GraphWalker")
                disableProperty().set(true)

                style {
                    backgroundColor += Color.BLACK
                }
                action {
                    logger.debug("Will run the model(s)")
                    graphic = icon(FontAwesomeIcon.PAUSE)
                    logger.debug("RunModelsEvent fired")
                    fire(RunModelsEvent())
                }
                subscribe<NevModelEditorEvent> { event ->
                    logger.debug("NevModelEditorEvent received")
                    disableProperty().set(false)
                }
                subscribe<LoadedModelsFromFileEvent> { event ->
                    logger.debug("LoadedModelsFromFileEvent received")
                    disableProperty().set(false)
                }
                subscribe<RunModelsDoneEvent> { event ->
                    logger.debug("RunModelsDoneEvent received")
                    graphic = icon(FontAwesomeIcon.PLAY)
                }
            }

            /**
             * Step button
             */
            button {
                graphic = icon(FontAwesomeIcon.STEP_FORWARD)
                disableProperty().set(true)
                style {
                    backgroundColor += Color.BLACK
                }
                subscribe<RunModelsEvent> { event ->
                    logger.debug("RunModelsEvent received")
                    disableProperty().set(false)
                }
                subscribe<RunModelsDoneEvent> { event ->
                    logger.debug("RunModelsDoneEvent received")
                    disableProperty().set(true)
                }
                subscribe<RunModelsStopEvent> { event ->
                    logger.debug("RunModelsStopEvent received")
                    disableProperty().set(true)
                }
            }

            /**
             * Stop button
             */
            button {
                graphic = icon(FontAwesomeIcon.STOP)
                disableProperty().set(true)
                style {
                    backgroundColor += Color.BLACK
                }
                action {
                    logger.debug("RunModelsStopEvent fired")
                    fire(RunModelsStopEvent())
                    resetModels()
                }
                subscribe<RunModelsEvent> { event ->
                    logger.debug("RunModelsEvent received")
                    disableProperty().set(false)
                }
                subscribe<RunModelsStopEvent> { event ->
                    logger.debug("RunModelsStopEvent received")
                    disableProperty().set(true)
                }
            }

            separator()
        }

        center = stackpane {
            group {
                svgpath("M48.94-22.41L48.94-4.09L48.94-4.09Q48.94-3.22 48.63-2.47L48.63-2.47L48.63-2.47Q48.31-1.72 47.77-1.17L47.77-1.17L47.77-1.17Q47.22-0.63 46.47-0.31L46.47-0.31L46.47-0.31Q45.72 0 44.84 0L44.84 0L15.53 0L15.53 0Q14.41 0 13.11-0.27L13.11-0.27L13.11-0.27Q11.81-0.53 10.55-1.09L10.55-1.09L10.55-1.09Q9.28-1.66 8.13-2.55L8.13-2.55L8.13-2.55Q6.97-3.44 6.08-4.70L6.08-4.70L6.08-4.70Q5.19-5.97 4.66-7.64L4.66-7.64L4.66-7.64Q4.13-9.31 4.13-11.41L4.13-11.41L4.13-33.41L4.13-33.41Q4.13-34.53 4.39-35.83L4.39-35.83L4.39-35.83Q4.66-37.13 5.22-38.39L5.22-38.39L5.22-38.39Q5.78-39.66 6.69-40.81L6.69-40.81L6.69-40.81Q7.59-41.97 8.86-42.86L8.86-42.86L8.86-42.86Q10.13-43.75 11.78-44.28L11.78-44.28L11.78-44.28Q13.44-44.81 15.53-44.81L15.53-44.81L48.56-44.81L48.56-36.69L15.53-36.69L15.53-36.69Q13.94-36.69 13.09-35.84L13.09-35.84L13.09-35.84Q12.25-35 12.25-33.34L12.25-33.34L12.25-11.41L12.25-11.41Q12.25-9.84 13.11-8.98L13.11-8.98L13.11-8.98Q13.97-8.13 15.53-8.13L15.53-8.13L40.81-8.13L40.81-18.31L19.19-18.31L19.19-26.50L44.84-26.50L44.84-26.50Q45.72-26.50 46.47-26.17L46.47-26.17L46.47-26.17Q47.22-25.84 47.77-25.28L47.77-25.28L47.77-25.28Q48.31-24.72 48.63-23.98L48.63-23.98L48.63-23.98Q48.94-23.25 48.94-22.41L48.94-22.41ZM86.06-33.78L86.06-25.66L67.78-25.66L67.78-25.66Q66.13-25.66 65.28-24.83L65.28-24.83L65.28-24.83Q64.44-24 64.44-22.41L64.44-22.41L64.44 0L56.31 0L56.31-22.41L56.31-22.41Q56.31-24.50 56.84-26.16L56.84-26.16L56.84-26.16Q57.38-27.81 58.27-29.08L58.27-29.08L58.27-29.08Q59.16-30.34 60.31-31.23L60.31-31.23L60.31-31.23Q61.47-32.13 62.73-32.69L62.73-32.69L62.73-32.69Q64-33.25 65.30-33.52L65.30-33.52L65.30-33.52Q66.59-33.78 67.72-33.78L67.72-33.78L86.06-33.78ZM123.66-22.59L123.66-11.19L123.66-11.19Q123.66-9.50 123.06-7.55L123.06-7.55L123.06-7.55Q122.47-5.59 121.14-3.92L121.14-3.92L121.14-3.92Q119.81-2.25 117.67-1.13L117.67-1.13L117.67-1.13Q115.53 0 112.47 0L112.47 0L97.81 0L97.81 0Q96.13 0 94.17-0.59L94.17-0.59L94.17-0.59Q92.22-1.19 90.55-2.52L90.55-2.52L90.55-2.52Q88.88-3.84 87.75-5.98L87.75-5.98L87.75-5.98Q86.63-8.13 86.63-11.19L86.63-11.19L86.63-11.19Q86.63-12.88 87.22-14.84L87.22-14.84L87.22-14.84Q87.81-16.81 89.14-18.48L89.14-18.48L89.14-18.48Q90.47-20.16 92.61-21.28L92.61-21.28L92.61-21.28Q94.75-22.41 97.81-22.41L97.81-22.41L112.47-22.41L112.47-14.66L97.81-14.66L97.81-14.66Q96.16-14.66 95.25-13.64L95.25-13.64L95.25-13.64Q94.34-12.63 94.34-11.13L94.34-11.13L94.34-11.13Q94.34-9.53 95.39-8.64L95.39-8.64L95.39-8.64Q96.44-7.75 97.88-7.75L97.88-7.75L112.47-7.75L112.47-7.75Q114.13-7.75 115.03-8.75L115.03-8.75L115.03-8.75Q115.94-9.75 115.94-11.25L115.94-11.25L115.94-22.59L115.94-22.59Q115.94-24.19 114.95-25.13L114.95-25.13L114.95-25.13Q113.97-26.06 112.47-26.06L112.47-26.06L94.63-26.06L94.63-33.78L112.47-33.78L112.47-33.78Q114.16-33.78 116.11-33.19L116.11-33.19L116.11-33.19Q118.06-32.59 119.73-31.27L119.73-31.27L119.73-31.27Q121.41-29.94 122.53-27.80L122.53-27.80L122.53-27.80Q123.66-25.66 123.66-22.59L123.66-22.59ZM167.84-22.41L167.84-11.41L167.84-11.41Q167.84-9.31 167.31-7.64L167.31-7.64L167.31-7.64Q166.78-5.97 165.91-4.70L165.91-4.70L165.91-4.70Q165.03-3.44 163.88-2.55L163.88-2.55L163.88-2.55Q162.72-1.66 161.45-1.09L161.45-1.09L161.45-1.09Q160.19-0.53 158.91-0.27L158.91-0.27L158.91-0.27Q157.63 0 156.47 0L156.47 0L141.81 0L141.81-8.13L156.47-8.13L156.47-8.13Q158.09-8.13 158.91-8.97L158.91-8.97L158.91-8.97Q159.72-9.81 159.72-11.41L159.72-11.41L159.72-22.34L159.72-22.34Q159.72-24.03 158.89-24.84L158.89-24.84L158.89-24.84Q158.06-25.66 156.47-25.66L156.47-25.66L141.88-25.66L141.88-25.66Q140.22-25.66 139.38-24.83L139.38-24.83L139.38-24.83Q138.53-24 138.53-22.41L138.53-22.41L138.53 10.56L130.41 10.56L130.41-22.41L130.41-22.41Q130.41-24.50 130.94-26.16L130.94-26.16L130.94-26.16Q131.47-27.81 132.36-29.08L132.36-29.08L132.36-29.08Q133.25-30.34 134.41-31.23L134.41-31.23L134.41-31.23Q135.56-32.13 136.83-32.69L136.83-32.69L136.83-32.69Q138.09-33.25 139.39-33.52L139.39-33.52L139.39-33.52Q140.69-33.78 141.81-33.78L141.81-33.78L156.47-33.78L156.47-33.78Q158.56-33.78 160.22-33.25L160.22-33.25L160.22-33.25Q161.88-32.72 163.14-31.84L163.14-31.84L163.14-31.84Q164.41-30.97 165.30-29.81L165.30-29.81L165.30-29.81Q166.19-28.66 166.75-27.39L166.75-27.39L166.75-27.39Q167.31-26.13 167.58-24.84L167.58-24.84L167.58-24.84Q167.84-23.56 167.84-22.41L167.84-22.41ZM212.34-22.41L212.34 0L204.22 0L204.22-22.41L204.22-22.41Q204.22-24 203.41-24.83L203.41-24.83L203.41-24.83Q202.59-25.66 200.97-25.66L200.97-25.66L186.31-25.66L186.31-33.78L200.97-33.78L200.97-33.78Q202.13-33.78 203.41-33.52L203.41-33.52L203.41-33.52Q204.69-33.25 205.95-32.69L205.95-32.69L205.95-32.69Q207.22-32.13 208.38-31.23L208.38-31.23L208.38-31.23Q209.53-30.34 210.41-29.08L210.41-29.08L210.41-29.08Q211.28-27.81 211.81-26.16L211.81-26.16L211.81-26.16Q212.34-24.50 212.34-22.41L212.34-22.41ZM183.03-48.06L183.03 0L174.91 0L174.91-48.06L183.03-48.06ZM269.66-44.81L277.91-44.81L270.41-2.81L270.41-2.81Q270.16-1.56 269.28-0.66L269.28-0.66L269.28-0.66Q268.41 0.25 267.16 0.50L267.16 0.50L267.16 0.50Q265.88 0.72 264.73 0.22L264.73 0.22L264.73 0.22Q263.59-0.28 262.94-1.34L262.94-1.34L248.06-25.78L233.16-1.34L233.16-1.34Q232.63-0.44 231.70 0.06L231.70 0.06L231.70 0.06Q230.78 0.56 229.72 0.56L229.72 0.56L229.72 0.56Q228.25 0.56 227.13-0.38L227.13-0.38L227.13-0.38Q226-1.31 225.75-2.81L225.75-2.81L218.19-44.81L226.44-44.81L231.81-15.22L244.63-35.66L244.63-35.66Q245.16-36.56 246.08-37.06L246.08-37.06L246.08-37.06Q247-37.56 248.06-37.56L248.06-37.56L248.06-37.56Q249.13-37.56 250.03-37.06L250.03-37.06L250.03-37.06Q250.94-36.56 251.53-35.66L251.53-35.66L264.28-15.22L269.66-44.81ZM317.63-22.59L317.63-11.19L317.63-11.19Q317.63-9.50 317.03-7.55L317.03-7.55L317.03-7.55Q316.44-5.59 315.11-3.92L315.11-3.92L315.11-3.92Q313.78-2.25 311.64-1.13L311.64-1.13L311.64-1.13Q309.50 0 306.44 0L306.44 0L291.78 0L291.78 0Q290.09 0 288.14-0.59L288.14-0.59L288.14-0.59Q286.19-1.19 284.52-2.52L284.52-2.52L284.52-2.52Q282.84-3.84 281.72-5.98L281.72-5.98L281.72-5.98Q280.59-8.13 280.59-11.19L280.59-11.19L280.59-11.19Q280.59-12.88 281.19-14.84L281.19-14.84L281.19-14.84Q281.78-16.81 283.11-18.48L283.11-18.48L283.11-18.48Q284.44-20.16 286.58-21.28L286.58-21.28L286.58-21.28Q288.72-22.41 291.78-22.41L291.78-22.41L306.44-22.41L306.44-14.66L291.78-14.66L291.78-14.66Q290.13-14.66 289.22-13.64L289.22-13.64L289.22-13.64Q288.31-12.63 288.31-11.13L288.31-11.13L288.31-11.13Q288.31-9.53 289.36-8.64L289.36-8.64L289.36-8.64Q290.41-7.75 291.84-7.75L291.84-7.75L306.44-7.75L306.44-7.75Q308.09-7.75 309-8.75L309-8.75L309-8.75Q309.91-9.75 309.91-11.25L309.91-11.25L309.91-22.59L309.91-22.59Q309.91-24.19 308.92-25.13L308.92-25.13L308.92-25.13Q307.94-26.06 306.44-26.06L306.44-26.06L288.59-26.06L288.59-33.78L306.44-33.78L306.44-33.78Q308.13-33.78 310.08-33.19L310.08-33.19L310.08-33.19Q312.03-32.59 313.70-31.27L313.70-31.27L313.70-31.27Q315.38-29.94 316.50-27.80L316.50-27.80L316.50-27.80Q317.63-25.66 317.63-22.59L317.63-22.59ZM339.41-8.13L339.41 0L335.78 0L335.78 0Q334.03 0 332.05-0.59L332.05-0.59L332.05-0.59Q330.06-1.19 328.34-2.55L328.34-2.55L328.34-2.55Q326.63-3.91 325.50-6.08L325.50-6.08L325.50-6.08Q324.38-8.25 324.38-11.41L324.38-11.41L324.38-48.06L332.50-48.06L332.50-11.41L332.50-11.41Q332.50-9.91 333.44-9.02L333.44-9.02L333.44-9.02Q334.38-8.13 335.78-8.13L335.78-8.13L339.41-8.13ZM365.19-18.44L384.31 0L372.59 0L356.22-15.81L356.22-15.81Q354.84-17.06 354.94-18.94L354.94-18.94L354.94-18.94Q355-19.81 355.39-20.58L355.39-20.58L355.39-20.58Q355.78-21.34 356.47-21.88L356.47-21.88L371.31-33.84L384.31-33.84L365.19-18.44ZM352.88-48.06L352.88 0L344.75 0L344.75-48.06L352.88-48.06ZM424.13-22.59L424.13-22.59L424.13-22.59Q424.13-20.91 423.53-18.95L423.53-18.95L423.53-18.95Q422.94-17 421.61-15.33L421.61-15.33L421.61-15.33Q420.28-13.66 418.14-12.53L418.14-12.53L418.14-12.53Q416-11.41 412.94-11.41L412.94-11.41L398.28-11.41L398.28-19.13L412.94-19.13L412.94-19.13Q414.59-19.13 415.50-20.14L415.50-20.14L415.50-20.14Q416.41-21.16 416.41-22.66L416.41-22.66L416.41-22.66Q416.41-24.25 415.39-25.16L415.39-25.16L415.39-25.16Q414.38-26.06 412.94-26.06L412.94-26.06L398.28-26.06L398.28-26.06Q396.63-26.06 395.72-25.05L395.72-25.05L395.72-25.05Q394.81-24.03 394.81-22.53L394.81-22.53L394.81-11.19L394.81-11.19Q394.81-9.56 395.83-8.66L395.83-8.66L395.83-8.66Q396.84-7.75 398.34-7.75L398.34-7.75L412.94-7.75L412.94 0L398.28 0L398.28 0Q396.59 0 394.64-0.59L394.64-0.59L394.64-0.59Q392.69-1.19 391.02-2.52L391.02-2.52L391.02-2.52Q389.34-3.84 388.22-5.98L388.22-5.98L388.22-5.98Q387.09-8.13 387.09-11.19L387.09-11.19L387.09-22.59L387.09-22.59Q387.09-24.28 387.69-26.23L387.69-26.23L387.69-26.23Q388.28-28.19 389.61-29.86L389.61-29.86L389.61-29.86Q390.94-31.53 393.08-32.66L393.08-32.66L393.08-32.66Q395.22-33.78 398.28-33.78L398.28-33.78L412.94-33.78L412.94-33.78Q414.63-33.78 416.58-33.19L416.58-33.19L416.58-33.19Q418.53-32.59 420.20-31.27L420.20-31.27L420.20-31.27Q421.88-29.94 423-27.80L423-27.80L423-27.80Q424.13-25.66 424.13-22.59ZM459.16-33.78L459.16-25.66L440.88-25.66L440.88-25.66Q439.22-25.66 438.38-24.83L438.38-24.83L438.38-24.83Q437.53-24 437.53-22.41L437.53-22.41L437.53 0L429.41 0L429.41-22.41L429.41-22.41Q429.41-24.50 429.94-26.16L429.94-26.16L429.94-26.16Q430.47-27.81 431.36-29.08L431.36-29.08L431.36-29.08Q432.25-30.34 433.41-31.23L433.41-31.23L433.41-31.23Q434.56-32.13 435.83-32.69L435.83-32.69L435.83-32.69Q437.09-33.25 438.39-33.52L438.39-33.52L438.39-33.52Q439.69-33.78 440.81-33.78L440.81-33.78L459.16-33.78Z") {
                    fill = Color.WHITESMOKE
                }
            }

            tabs = tabpane {
                subscribe<NevModelEditorEvent> { event ->
                    logger.debug("NevModelEditorEvent received")
                    modelEditors.add(event.modelEditor)
                    tab(event.modelEditor) {
                        text = event.modelEditor.title
                        subscribe<ClearAllModelsEvent> { event ->
                            logger.debug("ClearAllModelsEvent received")
                            close()
                        }
                    }
                    logger.debug("ModelsAreChangedEvent fired")
                    fire(ModelsAreChangedEvent())
                }

                subscribe<RunModelsEvent> { event ->
                    logger.debug("RunModelsEvent received")
                    resetModels()
                    runAsync {
                        subscribe<ProgressEvent> { event ->
                            logger.debug("ProgressEvent received")
                            updateProgress(event.completed, 1.0)
                        }
                        val executor = TestExecutor(createContexts())
                        executor.machine.addObserver(ExecutionObserver(modelEditors))
                        val result = executor.execute(true)
                        if (result.hasErrors()) {
                            for (error in result.errors) {
                                logger.error(error)
                            }
                        }
                        logger.debug("Done: [" + result.results.toString(2) + "]")
                    } ui {
                        logger.debug("RunModelsDoneEvent fired")
                        fire(RunModelsDoneEvent())
                    }
                }

                subscribe<LoadModelsFromFileEvent> { event ->
                    logger.debug("LoadModelsFromFileEvent received")

                    var factory: ContextFactory by singleAssign()
                    factory = when {
                        YEdContextFactory().accept(event.modelFile.toPath()) -> YEdContextFactory()
                        JsonContextFactory().accept(event.modelFile.toPath()) -> JsonContextFactory()
                        DotContextFactory().accept(event.modelFile.toPath()) -> DotContextFactory()
                        else -> throw UnsupportedFileFormat(event.modelFile.absolutePath)
                    }

                    val localContexts = factory.create(event.modelFile.toPath())
                    for (context in localContexts) {
                        val jsonModel = JsonModel()
                        jsonModel.setModel(context.model)
                        if (context.pathGenerator != null) {
                            jsonModel.generator = context.pathGenerator.toString()
                        }
                        if (context.nextElement != null) {
                            startElementId = context.nextElement.id
                        }

                        var modelEditor = ModelEditor(jsonModel)

                        modelEditors.add(modelEditor)
                        tab(modelEditor) {
                            text = jsonModel.name
                            subscribe<ClearAllModelsEvent> { event ->
                                logger.debug("ClearAllModelsEvent received")
                                close()
                            }
                        }
                    }

                    logger.debug("LoadedModelsFromFileEvent fired")
                    fire(LoadedModelsFromFileEvent())
                    logger.debug("ModelsAreChangedEvent fired")
                    fire(ModelsAreChangedEvent())
                }

                subscribe<SelectModelEditor> { event ->
                    logger.debug("SelectModelEditor received")
                    for (tab in tabs) {
                        if (tab.content == event.modelEditor.root) {
                            selectionModel.select(tab)
                            return@subscribe
                        }
                    }
                }

                runLater {
                    if (app.parameters.named["model-file"] != null) {
                        val fileName = app.parameters.named["model-file"]
                        logger.debug("LoadModelsFromFileEvent fired")
                        fire(LoadModelsFromFileEvent(File(fileName)))
                    }
                }
            }

            style {
                backgroundColor += c("#1E89B7")
            }
        }

        bottom = hbox(4.0) {
            runLater {
                progressbar(status.progress)
            }
            label(status.message)
            visibleWhen { status.running }
            paddingAll = 4
        }
    }

    private fun createContexts(): ArrayList<Context> {
        var contexts = ArrayList<Context>()
        for (modelEditor in modelEditors) {
            val context = ExecutionContextFX()
            context.model = modelEditor.model.model.build()
            context.pathGenerator = GeneratorFactory.parse(modelEditor.model.generator)
            val vList = context.model.vertices.filter { it.id == startElementId }
            if (vList.isNotEmpty()) {
                context.nextElement = vList.last()
            }
            val eList = context.model.edges.filter { it.id == startElementId }
            if (eList.isNotEmpty()) {
                context.nextElement = eList.last()
            }
            contexts.add(context)
        }
        return contexts
    }

    private fun resetModels() {
        for (modelEditor in modelEditors) {
            for (v in modelEditor.vertices) {
                v.rect.fill = Color.LIGHTBLUE
            }
            for (e in modelEditor.edges) {
                e.path.stroke = Color.BLACK
            }
        }
    }

    private fun icon(icon: FontAwesomeIcon) = FontAwesomeIconView(icon).apply {
        glyphSize = 18
        fill = Color.WHITESMOKE
    }
}

