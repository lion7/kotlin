/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOnWithSuppression
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass

object FirDelegationSuperCallInEnumConstructorChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isEnumClass) {
            return
        }

        for (constructor in declaration.declarations) {
            if (constructor !is FirConstructor || constructor.isPrimary) continue
            withSuppressedDiagnostics(constructor, context) { ctx ->
                val delegatedConstructor = constructor.delegatedConstructor ?: return@withSuppressedDiagnostics
                if (!delegatedConstructor.isThis && delegatedConstructor.source?.kind !is KtFakeSourceElementKind) {
                    reporter.reportOnWithSuppression(
                        delegatedConstructor,
                        FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR,
                        ctx
                    )
                }
            }
        }
    }
}
