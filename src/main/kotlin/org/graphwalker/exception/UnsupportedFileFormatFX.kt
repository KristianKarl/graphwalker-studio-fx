package org.graphwalker.exception

class UnsupportedFileFormatFX(path: String) : Throwable("File format for file: $path, is not supported")
