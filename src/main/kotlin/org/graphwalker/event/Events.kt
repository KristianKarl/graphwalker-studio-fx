package org.graphwalker.event

import org.graphwalker.views.ModelEditorView
import tornadofx.*
import java.io.File

class LoadModelsFromFileEvent(val modelFile: File) : FXEvent()
class LoadedModelsFromFileEvent : FXEvent()

class NevModelEditorEvent(val modelEditorView: ModelEditorView) : FXEvent()

class RunModelsEvent : FXEvent()
class RunModelsDoneEvent : FXEvent()
class RunModelsStopEvent : FXEvent()

class ModelsAreChangedEvent : FXEvent()
class ModelsAreSavedEvent : FXEvent()
class ClearAllModelsEvent : FXEvent()

class OpenPropertiesView : FXEvent()
