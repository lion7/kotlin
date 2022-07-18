/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.kapt3.test.*
import org.jetbrains.kotlin.kapt3.test.KaptTestDirectives.MAP_DIAGNOSTIC_LOCATIONS
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.junit.jupiter.api.Test

abstract class AbstractKotlinKapt4ContextTest(
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +MAP_DIAGNOSTIC_LOCATIONS
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
            ::KaptEnvironmentConfigurator,
        )

        facadeStep(::Kapt4Facade)
    }
}

class KotlinKapt4ContextTestGenerated : AbstractKotlinKapt4ContextTest(TargetBackend.JVM_IR) {
    @Test
    @TestMetadata("enums.kt")
    fun testEnums() {
        runTest("plugins/kapt3/kapt3-compiler/testData/converter/enums.kt")
    }
}
