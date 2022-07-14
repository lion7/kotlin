/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.coreCases.KpmTestCaseDescriptor
import org.jetbrains.kotlin.project.model.coreCases.instantiateCase
import java.io.File

/**
 * In this file you can find support for decorating an existing [KpmTestCase] with sources
 *
 * The low-level API is [addSources]
 *
 * For the convenience, the concept of "canonical sources file structure" is introduced.
 * You can ask to generate template of this structure in designated place by calling [generateTemplateCanonicalFileStructure],
 * which is useful when you have a [KpmTestCase] and want to add some manually-written sources (test data, usually) to it.
 *
 * After that, you can use [addSourcesFromCanonicalFileStructure] to automatically decorate an existing
 * [KpmTestCase] with sources pulled from the specified folder
 *
 * See [generateTemplateCanonicalFileStructure] KDoc for details on the canonical sources file structure
 */


/**
 * Warning! Mutates passed test case
 */
fun KpmTestCase.addSourcesFromCanonicalFileStructure(root: File): KpmTestCase {
    return addSources { fragment ->
        val canonicalFragmentTestdataFolder = fragment.canonicalSourceFolderAbsolute(root)
        require(canonicalFragmentTestdataFolder.exists()) {
            "Can't find testdata for fragment $fragment at ${canonicalFragmentTestdataFolder.absolutePath}"
        }

        canonicalFragmentTestdataFolder.listFiles()?.toList().orEmpty()
    }
}

/**
 * Warning! Mutates passed test case
 */
fun KpmTestCase.addSources(sourcesForFragment: (TestKpmFragment) -> Iterable<File>): KpmTestCase {
    val allFragments = projects.flatMap { it.modules.flatMap { it.fragments } }
    for (fragment in allFragments) {
        val sources = sourcesForFragment(fragment)
        fragment.kotlinSourceRoots.addAll(sources)
    }

    return this
}

/**
 * Generates a canonical source-files structure at designated [root] for a given [KpmTestCaseDescriptor]
 *
 * In general, each [TestKpmFragment] will have a folder formed as following:
 *    `root/$fragmentProject.name/$fragmentModule.name/$fragment.name`
 *
 * However, in order to reduce nesting for simple common cases, two amendments are applied:
 * - if a given [KpmTestCaseDescriptor] has only one single [TestKpmModuleContainer], then the project folder is omitted,
 *   and modules of that project are embedded straight into the [root]
 * - similarly, if a given [TestKpmModuleContainer] has only one single [TestKpmModule], then the module folder is omitted,
 *   and fragments of that module are embedded straight into the parent-directory.
 *   This rule is applied for each project separately.
 *
 * Both amendments can be applied simultaneously, so for a test case with a single project and single module, fragments will live
 * directly inside [root]
 */
fun KpmTestCaseDescriptor.generateTemplateCanonicalFileStructure(root: File) {
    val case = instantiateCase()
    val allFragments = case.projects.flatMap { it.modules.flatMap { it.fragments } }
    allFragments.forEach { fragment ->
        val canonicalFragmentTestdataFolder = fragment.canonicalSourceFolderAbsolute(root)
        canonicalFragmentTestdataFolder.mkdirs()

        val templateSources = canonicalFragmentTestdataFolder.resolve(fragment.fragmentName + ".kt")
        templateSources.writeText(PLACEHOLDER_SOURCES_TEXT)
    }
}

private fun TestKpmFragment.canonicalSourceFolderAbsolute(root: File): File {
    val pathRelativeToRoot = canonicalSourceFolderRelative()
    return root.resolve(pathRelativeToRoot)
}

private fun TestKpmFragment.canonicalSourceFolderRelative(): File {
    val module = containingModule
    val project = module.containingProject
    val case = project.containingCase

    val isSingleModule = project.modules.size == 1
    val isSingleProject = case.projects.size == 1

    val pathParts = listOfNotNull(
        project.name.takeIf { !isSingleProject },
        module.name.takeIf { !isSingleModule },
        fragmentName
    )

    return File(pathParts.joinToString(separator = File.separator))
}

private val PLACEHOLDER_SOURCES_TEXT = """
/**
 * Generated by [org.jetbrains.kotlin.project.model.infra.${KpmTestCaseDescriptor::generateTemplateCanonicalFileStructure.name}]
 * 
 * Write your testdata sources here, or remove the file, if it is not needed
 */
"""
