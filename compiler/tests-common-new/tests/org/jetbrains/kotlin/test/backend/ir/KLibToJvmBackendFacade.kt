/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.IrActualizer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.dependencyProvider

class KLibToJvmBackendFacade(
    val testServices: TestServices,
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Jvm>() {
    private val jvmFacadeHelper = JvmBackendFacadeHelper(testServices)

    override val inputKind = ArtifactKinds.KLib
    override val outputKind = ArtifactKinds.Jvm

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        val incomingDependencies = testServices.dependencyProvider.getIncomingDependencies(module)
        return incomingDependencies.none { it.relation == DependencyRelation.DependsOnDependency }
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Jvm? {
        require(inputArtifact is JvmKLibArtifact) {
            "KLibToJvmBackendFacade expects BinaryArtifacts.KLib as input"
        }

        val dependencyProvider = testServices.dependencyProvider
        val dependencyFragments = module.dependsOnDependencies.mapNotNull { dependency ->
            val testModule = dependencyProvider.getTestModule(dependency.moduleName)
            val artifact = dependencyProvider.getArtifact(testModule, ArtifactKinds.KLib)
            (artifact as? JvmKLibArtifact)?.backendInput?.irModuleFragment
        }
        IrActualizer.actualize(inputArtifact.backendInput.irModuleFragment, dependencyFragments)

        return jvmFacadeHelper.transform(
            inputArtifact.state,
            inputArtifact.codegenFactory,
            inputArtifact.backendInput,
            inputArtifact.sourceFiles,
            module
        )
    }
}