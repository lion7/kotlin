/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.test.model.BinaryArtifacts

data class JvmKLibArtifact(
    val state: GenerationState,
    val codegenFactory: JvmIrCodegenFactory,
    val backendInput: JvmIrCodegenFactory.JvmIrBackendInput,
    val sourceFiles: List<KtSourceFile>
) : BinaryArtifacts.KLib(null)
