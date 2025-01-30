package dev.topping.kotlin.compose.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.FeaturePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.res.GenerateLibraryRFileTask
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.builder.model.AndroidProject
import groovy.util.XmlSlurper
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

val IS_DEBUG = false

class HelperPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.all {
            when (it) {
                is FeaturePlugin -> {
                    project.extensions[FeatureExtension::class].run {
                        configureLRGeneration(project, featureVariants)
                        configureLRGeneration(project, libraryVariants)
                    }
                }
                is LibraryPlugin -> {
                    project.extensions[LibraryExtension::class].run {
                        configureLRGeneration(project, libraryVariants)
                    }
                }
                is AppPlugin -> {
                    project.extensions[AppExtension::class].run {
                        configureLRGeneration(project, applicationVariants)
                    }
                }
            }
        }
    }

    // Parse the variant's main manifest file in order to get the package id which is used to create
    // R.java in the right place.
    private fun getPackageName(variant : BaseVariant) : String {
        val slurper = XmlSlurper(false, false)
        val list = variant.sourceSets.map { it.manifestFile }

        // According to the documentation, the earlier files in the list are meant to be overridden by the later ones.
        // So the first file in the sourceSets list should be main.
        val result = slurper.parse(list[0])
        return result.getProperty("@package").toString()
    }

    private fun configureLRGeneration(project: Project, variants: DomainObjectSet<out BaseVariant>) {
        variants.all { variant ->
            val rPackage = variant.mergedFlavor.applicationId
            val packagePath = rPackage.replace(".", File.separator)
            val outputDir = project.buildDir.resolve(
                "generated/source/lr/${variant.dirName}/$packagePath")
            val once = AtomicBoolean()
            variant.outputs.all { output ->
                // Though there might be multiple outputs, their R files are all the same. Thus, we only
                // need to configure the task once with the R.java input and action.
                if (once.compareAndSet(false, true)) {
                    val processResources = output.processResourcesProvider.get() // TODO lazy

                    // TODO: switch to better API once exists in AGP (https://issuetracker.google.com/118668005)
                    val rFile =
                        project.files(
                            when (processResources) {
                                is GenerateLibraryRFileTask -> processResources.textSymbolOutputFile
                                is LinkApplicationAndroidResourcesTask -> processResources.textSymbolOutputFile
                                else -> throw RuntimeException(
                                    "Minimum supported Android Gradle Plugin is 3.3.0")
                            })
                            .builtBy(processResources)

                    if(IS_DEBUG)
                        println("projectdir ${project.projectDir}")
                    val projectDir = project.projectDir
                    var foundPath = ""
                    projectDir.walk().forEach {
                        if(it.absolutePath.contains("compile_and_runtime_not_namespaced_r_class_jar")
                            && it.absolutePath.contains(variant.baseName + File.separator + "R.jar")){
                            foundPath = it.absolutePath
                            return@forEach
                        }
                    }

                    if(foundPath == "")
                        return@all

                    val file = File(foundPath)
                    val parent = File(file.parent)
                    val root = File(parent.parent)

                    val generate = project.tasks.create("generate${variant.name.capitalize()}LR", LRGenerator::class.java) {
                        it.buildDir = root.absolutePath
                        it.outputDir = outputDir
                        it.rFile = rFile
                        it.packageName = rPackage
                        it.className = "LR"
                        it.variant = variant.baseName
                        it.projectDir = projectDir.absolutePath
                    }
                    variant.registerJavaGeneratingTask(generate, outputDir)
                }
            }
        }
    }

    private operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
        return getByType(type.java)
    }
}