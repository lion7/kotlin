/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.GenerateArtifactPodspecTask.ArtifactType.*
import org.jetbrains.kotlin.gradle.utils.appendLine
import javax.inject.Inject

abstract class GenerateArtifactPodspecTask @Inject constructor(
    private val projectLayout: ProjectLayout,
) : DefaultTask() {

    enum class ArtifactType {
        Library, Framework, FatFramework, XCFramework
    }

    class PodspecDependency(
        @get:Input
        val name: String,

        @get:Optional
        @get:Input
        val version: String?,
    )

    @get:Input
    abstract val specName: Property<String>

    @get:Input
    abstract val attributes: MapProperty<String, String>

    @get:Input
    abstract val rawStatements: ListProperty<String>

    @get:Nested
    abstract val dependencies: ListProperty<PodspecDependency>

    @get:Input
    abstract val artifactType: Property<ArtifactType?>

    @get:OutputFile
    val outputFile: RegularFile
        get() = projectLayout.buildDirectory.file("out/podspec/${specName.get()}.podspec").get()

    @TaskAction
    fun generate() {
        outputFile.asFile.writeText(buildString {

            appendLine("Pod::Spec.new do |spec|")

            appendAttributes()
            appendDependencies()
            appendRawStatements()

            appendLine("end")

        })
    }

    private fun Appendable.appendAttributes() {
        for (entry in (attributes.get().asSequence() + buildAdditionalAttrs().asSequence())) {
            append("    spec.")
            append(entry.key)

            repeat(24 - entry.key.length) { append(' ') }
            append(" = ")

            append(entry.value.wrapInSingleQuotesIfNeeded())

            appendLine()
        }
    }

    private fun Appendable.appendDependencies() {
        if (dependencies.get().isNotEmpty()) {
            appendLine()
        }

        for (dependency in dependencies.get()) {
            append("    spec.dependency ")
            append(dependency.name.wrapInSingleQuotes())
            if (dependency.version != null) {
                append(", ")
                append(dependency.version.wrapInSingleQuotes())
            }
            appendLine()
        }
    }

    private fun Appendable.appendRawStatements() {
        if (rawStatements.get().isNotEmpty()) {
            appendLine()
        }

        for (statement in rawStatements.get()) {
            appendLine(statement)
        }
    }

    private fun buildAdditionalAttrs(): Map<String, String> {
        val artifactTypeValue = artifactType.orNull

        return mutableMapOf<String, String>().apply {
            if (!attributes.get().containsKey(specNameKey)) {
                put(specNameKey, specName.get())
            }

            if (vendoredKeys.none { attributes.get().containsKey(it) } && artifactTypeValue != null) {
                val vendoredKey = when (artifactTypeValue) {
                    Library -> vendoredLibrary
                    Framework, FatFramework, XCFramework -> vendoredFrameworks
                }

                val vendoredValue = specName.get() + "." + when (artifactTypeValue) {
                    Library -> "dylib"
                    Framework, FatFramework -> "framework"
                    XCFramework -> "xcframework"
                }

                put(vendoredKey, vendoredValue)
            }
        }
    }

    private fun String.wrapInSingleQuotesIfNeeded(): String {
        return when {
            startsWith('{') ||
                    startsWith('[') ||
                    startsWith("<<-") ||
                    startsWith('\'') ||
                    startsWith('"') ||
                    equals("true") ||
                    equals("false") -> this

            else -> wrapInSingleQuotes()
        }
    }

    private fun String.wrapInSingleQuotes() = "'" + replace("'", "\\'") + "'"

    companion object {
        private const val specNameKey = "name"
        private const val vendoredLibrary = "vendored_library"
        private const val vendoredLibraries = "vendored_libraries"
        private const val vendoredFrameworks = "vendored_frameworks"

        private val vendoredKeys = listOf(vendoredLibrary, vendoredLibraries, vendoredFrameworks)
    }
}