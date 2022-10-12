/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

object DelegatedDefaultInterfaceMethodsAreOverriddenChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        if (declaration !is KtClassOrObject) return

        val delegatedTypes = declaration.superTypeListEntries
            .asSequence()
            .filterIsInstance<KtDelegatedSuperTypeEntry>()
            .mapNotNull { specifier -> specifier.typeReference }
            .mapNotNull { context.trace.get(BindingContext.TYPE, it) }
            .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
            .toSet()

        if (delegatedTypes.isEmpty()) return

        descriptor.defaultType.memberScope.getContributedDescriptors()
            .asSequence()
            .filterIsInstance<FunctionDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .flatMap { it.overriddenDescriptors }
            .firstOrNull { it.isDefaultJavaMethod && it.containingDeclaration in delegatedTypes }
            ?.let {
                context.trace.report(ErrorsJvm.NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD.on(declaration, it.name.asString()))
                return
            }
    }

    private val CallableMemberDescriptor.isDefaultJavaMethod: Boolean
        get() = kind == CallableMemberDescriptor.Kind.DECLARATION && (containingDeclaration as? JavaClassDescriptor)?.kind == ClassKind.INTERFACE
}
