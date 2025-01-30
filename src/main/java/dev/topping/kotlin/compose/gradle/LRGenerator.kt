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

    @Suppress("unused") // Invoked by Gradle.
    @TaskAction
    fun brewJava() {
        brewJava(rFile!!.singleFile, outputDir!!, packageName!!, className!!, buildDir!!, variant!!)
    }
}

fun brewJava(
    rFile: File,
    outputDir: File,
    packageName: String,
    className: String,
    buildDir: String,
    variant: String
) {
    /*FinalLRClassBuilder(packageName, className)
        .also { ResourceSymbolListReader(it).readSymbolTable(rFile) }
        .build()
        .writeTo(outputDir)*/
    //${projectDir}/parser.jar 2 ${buildDir}/intermediates/compile_and_runtime_not_namespaced_r_class_jar/" + outputType + "/R.jar ${buildDir}/generated/toppingresource/ " + applicationId + ".R"
    //DocumentParser.ResourceLoader("${buildDir}/intermediates/compile_and_runtime_not_namespaced_r_class_jar/${variant}/R.jar", true, "${packageName}.R")
    //println("path " + "${buildDir}/${variant}/R.jar")
    val intermediates = File(buildDir).parent
    val build = File(intermediates).parent
    val possibleAndroidModuleDir = File(build).parent
    val possibleAndroidModuleOutput = Paths.get(possibleAndroidModuleDir, "build", "generated", "toppingresource")
    val possibleAndroidModule = Paths.get(possibleAndroidModuleDir).fileName
    //println("classname " + "${packageName}.${possibleAndroidModule}.R")
    //DocumentParser.outputFolder = outputDir.absolutePath
    DocumentParser.outputFolder = possibleAndroidModuleOutput.absolutePathString()
    //println("outputpath " + DocumentParser.outputFolder)
    DocumentParser.ResourceLoader("${buildDir}/${variant}/R.jar", true, "${packageName}.${possibleAndroidModule}.R")
}