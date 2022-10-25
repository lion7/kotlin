/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class JsSteppingTestAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> = buildList {
        if (globalDirectives.contains(ConfigurationDirectives.WITH_STDLIB))
            add(File(WITH_STDLIB_HELPER_PATH).toTestFile())
        else
            add(File(MINIMAL_HELPER_PATH).toTestFile())
        add(File(COMMON_HELPER_PATH).toTestFile())
    }

    companion object {
        private const val HELPERS_DIR = "compiler/testData/debug/jsTestHelpers"
        private const val COMMON_HELPER_PATH = "$HELPERS_DIR/jsCommonTestHelpers.kt"
        private const val MINIMAL_HELPER_PATH = "$HELPERS_DIR/jsMinimalTestHelpers.kt"
        private const val WITH_STDLIB_HELPER_PATH = "$HELPERS_DIR/jsWithStdlibTestHelpers.kt"
    }
}
