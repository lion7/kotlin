/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.model.Symbol
import com.intellij.model.SymbolResolveResult
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

internal class SymbolLightPsiJavaCodeReferenceElementWithReference(private val ktElement: PsiElement, reference: PsiReference) :
    SymbolLightPsiJavaCodeReferenceElementBase(ktElement),
    PsiReference by reference {

    override fun getElement(): PsiElement = ktElement

    override fun resolveReference(): MutableCollection<out SymbolResolveResult> {
        return super<PsiReference>.resolveReference()
    }

    override fun resolvesTo(target: Symbol): Boolean {
        return super<PsiReference>.resolvesTo(target)
    }

    override fun getAbsoluteRange(): TextRange {
        return super<PsiReference>.getAbsoluteRange()
    }

    override fun getVariants(): Array<Any> {
        return super<PsiReference>.getVariants()
    }

    override fun checkAdd(element: PsiElement) {
        @Suppress("DEPRECATION")
        super.checkAdd(element)
    }
}