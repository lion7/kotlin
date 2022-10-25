/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UsedClassifierSymbolStatus.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

internal sealed interface UsedClassifierSymbolStatus2 {
    val isUnlinked: Boolean

    /** IR symbol of unlinked classifier. */
    // TODO: add reasons
    class Unlinked : UsedClassifierSymbolStatus2 {
        override val isUnlinked get() = true
        var isPatched = false // To avoid re-patching what already has been patched.
    }

    /** IR symbol of linked classifier. */
    object Linked : UsedClassifierSymbolStatus2 {
        override val isUnlinked get() = false
    }

    companion object {
        val UsedClassifierSymbolStatus2?.isUnlinked: Boolean get() = this?.isUnlinked == true
    }
}

internal enum class UsedClassifierSymbolStatus(val isUnlinked: Boolean) {
    /** IR symbol of unlinked classifier. */
    UNLINKED(true),

    /** IR symbol of linked classifier. */
    LINKED(false);
}

internal class UsedClassifierSymbols {
    private val linkedSymbols = THashSet<IrClassifierSymbol>()
    private val unlinkedSymbols = THashMap<IrClassifierSymbol, UsedClassifierSymbolStatus2.Unlinked>()

    fun forEachClassSymbolToPatch(patchAction: (IrClassSymbol) -> Unit) {
        val commonSymbols = linkedSymbols union unlinkedSymbols.keys
        assert(commonSymbols.isEmpty()) {
            "There are classifier symbols that are registered as linked and unlinked simultaneously: " + commonSymbols.joinToString()
        }

        unlinkedSymbols.forEachEntry { symbol, status ->
            if (!status.isPatched) {
                status.isPatched = true
                if (symbol.isBound && symbol is IrClassSymbol) patchAction(symbol)
            }
            true
        }
    }

    operator fun get(symbol: IrClassifierSymbol): UsedClassifierSymbolStatus2? =
        if (symbol in linkedSymbols) UsedClassifierSymbolStatus2.Linked else unlinkedSymbols[symbol]

    fun register(symbol: IrClassifierSymbol, isUnlinked: Boolean): Boolean {
        if (isUnlinked)
            unlinkedSymbols[symbol] = UsedClassifierSymbolStatus2.Unlinked()
        else
            linkedSymbols += symbol
        return isUnlinked
    }

    fun register(symbol: IrClassifierSymbol, status: UsedClassifierSymbolStatus): Boolean {
//        symbols.put(symbol, status.code)
        return status.isUnlinked
    }

    companion object {
        private inline val Byte.status: UsedClassifierSymbolStatus?
            get() = when (this) {
                1.toByte() -> UNLINKED
                2.toByte() -> LINKED
                else -> null
            }

        private inline val UsedClassifierSymbolStatus.code: Byte
            get() = when (this) {
                UNLINKED -> 1.toByte()
                LINKED -> 2.toByte()
            }
    }
}
