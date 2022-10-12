/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.codeReferences

import com.intellij.model.Symbol
import com.intellij.model.SymbolResolveResult
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor

internal abstract class SymbolLightPsiJavaCodeReferenceElementBase(private val ktElement: PsiElement) :
    PsiElement by ktElement,
    PsiJavaCodeReferenceElement {

    override fun multiResolve(incompleteCode: Boolean): Array<JavaResolveResult> = emptyArray()

    override fun processVariants(processor: PsiScopeProcessor) {}

    override fun advancedResolve(incompleteCode: Boolean): JavaResolveResult =
        JavaResolveResult.EMPTY

    override fun getQualifier(): PsiElement? = null

    override fun getReferenceName(): String? = null

    override fun getReferenceNameElement(): PsiElement? = null

    override fun getParameterList(): PsiReferenceParameterList? = null

    override fun getTypeParameters(): Array<PsiType> = emptyArray()

    override fun isQualified(): Boolean = false

    override fun getQualifiedName(): String? = null

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun resolveReference(): MutableCollection<out SymbolResolveResult> {
        return super.resolveReference()
    }

    override fun resolvesTo(target: Symbol): Boolean {
        return super.resolvesTo(target)
    }

    override fun getAbsoluteRange(): TextRange {
        return super.getAbsoluteRange()
    }

    override fun getVariants(): Array<Any> {
        return super.getVariants()
    }

    override fun getTextRangeInParent(): TextRange {
        return super<PsiJavaCodeReferenceElement>.getTextRangeInParent()
    }

    override fun getOwnReferences(): MutableIterable<PsiSymbolReference> {
        return super<PsiJavaCodeReferenceElement>.getOwnReferences()
    }
}