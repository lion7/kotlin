/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object JvmPropertyVsFieldAmbiguityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClassOrObject) return
        if (descriptor !is ClassDescriptor) return
        for (property in declaration.declarations) {
            if (property !is KtProperty) continue
            val hasLateInit = property.hasModifier(KtTokens.LATEINIT_KEYWORD)
            if (!hasLateInit && property.getter == null) continue

            descriptor.unsubstitutedMemberScope.getContributedVariables(
                property.nameAsSafeName, NoLookupLocation.FROM_TEST
            ).forEach { mayBeField ->
                if (!mayBeField.isJavaField) return@forEach
                var field = mayBeField
                while (field.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                    field = field.overriddenDescriptors.firstOrNull() ?: break
                }
                val fieldClassDescriptor = field.containingDeclaration as? ClassDescriptor
                if (fieldClassDescriptor === descriptor) return@forEach

                context.trace.report(
                    ErrorsJvm.DERIVED_CLASS_PROPERTY_SHADOWS_BASE_CLASS_FIELD.on(
                        property,
                        if (hasLateInit) "with lateinit" else "with custom getter",
                        fieldClassDescriptor?.fqNameSafe?.asString() ?: "unknown class"
                    )
                )
            }
        }
    }
}
