/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices

class KLibForJvmBackendFacade(
    testServices: TestServices,
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        return when (inputArtifact) {
            is IrBackendInput.JvmIrBackendInput -> JvmKLibArtifact(
                inputArtifact.state,
                inputArtifact.codegenFactory,
                inputArtifact.backendInput,
                inputArtifact.sourceFiles
            )
            else -> error("KLibForJvmBackendFacade expects IrBackendInput.JvmIrBackendInput as input")
        }
    }
}