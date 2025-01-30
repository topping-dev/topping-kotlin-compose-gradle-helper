package dev.topping.kotlin.compose.gradle

import dev.topping.kotlin.compose.gradle.documentparser.DocumentParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@CacheableTask
open class LRGenerator : DefaultTask() {
    @get:OutputDirectory
    var outputDir: File? = null

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    var rFile: FileCollection? = null

    @get:Input
    var packageName: String? = null

    @get:Input
    var className: String? = null

    @get:Input
    var buildDir: String? = null

    @get:Input
    var variant: String? = null

    @get:Input
    var projectDir: String? = null

    @Suppress("unused") // Invoked by Gradle.
    @TaskAction
    fun brewJava() {
        brewJava(rFile!!.singleFile, outputDir!!, packageName!!, className!!, buildDir!!, variant!!, projectDir!!)
    }
}

fun brewJava(
    rFile: File,
    outputDir: File,
    packageName: String,
    className: String,
    buildDir: String,
    variant: String,
    projectDir: String
) {
    /*FinalLRClassBuilder(packageName, className)
        .also { ResourceSymbolListReader(it).readSymbolTable(rFile) }
        .build()
        .writeTo(outputDir)*/
    if(IS_DEBUG) {
        println(buildDir)
        println(variant)
        println("path " + "${buildDir}/${variant}/R.jar")
    }
    val outputDir = Paths.get(projectDir, "build", "generated", "toppingresource")
    if(IS_DEBUG) {
        println("outputDir $outputDir")
        println("classname " + "${packageName}.R")
    }
    DocumentParser.outputFolder = outputDir.absolutePathString()
    DocumentParser.ResourceLoader("${buildDir}/${variant}/R.jar", true, "${packageName}.R")
}