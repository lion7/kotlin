/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

/**
 * Compiler is non-lazy, so it does nothing.
 */
object FirCompilerLazyDeclarationResolver : FirLazyDeclarationResolver() {
    private var currentTransformerPhase = ThreadLocal<FirResolvePhase>()
    private var collectingDiagnostics = ThreadLocal.withInitial { false }

    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase)
    }

    @PrivateForInline
    fun startResolvingPhase(phase: FirResolvePhase) {
        check(currentTransformerPhase.get() == null)
        currentTransformerPhase.set(phase)
    }

    @PrivateForInline
    fun finishResolvingPhase(phase: FirResolvePhase) {
        check(currentTransformerPhase.get() == phase)
        currentTransformerPhase.set(null)
    }

    @PrivateForInline
    fun startDiagnosticCollection() {
        collectingDiagnostics.set(true)
    }

    @PrivateForInline
    fun finishDiagnosticCollection() {
        collectingDiagnostics.set(false)
    }

    @OptIn(PrivateForInline::class)
    inline fun controlLazyResolveContractsCheckingInside(phase: FirResolvePhase, action: () -> Unit) {
        startResolvingPhase(phase)
        try {
            action()
        } finally {
            finishResolvingPhase(phase)
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun controlLazyResolveContractsCheckingInsideDiagnosticCollection(action: () -> Unit) {
        startDiagnosticCollection()
        try {
            action()
        } finally {
            finishDiagnosticCollection()
        }
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase) {
        if (collectingDiagnostics.get()) return

        val currentResolvePhase = currentTransformerPhase.get()
            ?: error("Current phase is not set, please call startResolvingPhaseFormCurrentThread before starting transforming the file")

        check(requestedPhase < currentResolvePhase) {
            "lazyResolveToPhase($requestedPhase) cannot be called from a transformer with phase $currentResolvePhase." +
                    "lazyResolveToPhase can be called only from transformer with phase which is strictly greater when requested phase " +
                    ", i.e., lazyResolveToPhase(A) may be only called from a lazy transformer with phase B, where A < B " +
                    "This is a contract of lazy resolution"
        }
    }
}

