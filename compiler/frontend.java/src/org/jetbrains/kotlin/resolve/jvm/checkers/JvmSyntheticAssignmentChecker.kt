/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.isSynthesized
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.getVariance
import org.jetbrains.kotlin.types.expressions.BasicExpressionTypingVisitor
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.types.typeUtil.contains

object JvmSyntheticAssignmentChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (!resultingDescriptor.isSynthesized) return
        if (resultingDescriptor !is SyntheticJavaPropertyDescriptor) return
        if (reportOn !is KtNameReferenceExpression) return
        val containingClassDescriptor = resultingDescriptor.setMethod?.containingDeclaration as? ClassDescriptor ?: return
        val binaryExpression = reportOn.getParentOfType<KtBinaryExpression>(strict = true) ?: return
        if (!BasicExpressionTypingVisitor.isLValue(reportOn, binaryExpression)) return
        val receiverType = resolvedCall.extensionReceiver?.type ?: return
        val propertyType = resolvedCall.candidateDescriptor.returnType ?: return
        var projectedTypeParameterFound = false
        for (argument in receiverType.arguments) {
            val argumentType = argument.type
            if (argumentType is NewCapturedType) {
                if (argumentType.constructor.projection.getVariance() == TypeVariance.IN) {
                    continue
                }
                val typeParameter = argumentType.constructor.typeParameter ?: continue
                if (typeParameter !in containingClassDescriptor.declaredTypeParameters) continue
                if (propertyType.contains {
                        val mayBeTypeParameter = it.constructor.declarationDescriptor
                        // Note 1: we cannot compare type parameters by identify
                        // (synthetic property creates a copy of type parameter(s) in its class, if any)
                        // Note 2: synthetic property cannot have its own type parameters
                        // So it's enough to compare names and ensure that
                        // typeParameter is from class (see above) and mayByTypeParameter is from property
                        mayBeTypeParameter is TypeParameterDescriptor &&
                                mayBeTypeParameter.name == typeParameter.name &&
                                mayBeTypeParameter in resolvedCall.candidateDescriptor.typeParameters
                    }
                ) {
                    projectedTypeParameterFound = true
                    break
                }
            }
        }
        if (!projectedTypeParameterFound) return
        context.trace.report(ErrorsJvm.SYNTHETIC_SETTER_PROJECTED_OUT.on(binaryExpression.left ?: reportOn, resultingDescriptor))
    }
}