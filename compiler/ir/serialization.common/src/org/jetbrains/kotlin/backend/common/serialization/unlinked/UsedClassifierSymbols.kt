/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

internal sealed interface UsedClassifierSymbolStatus {
    val isUnlinked: Boolean

    /** IR symbol of unlinked classifier. */
    // TODO: add reasons
    class Unlinked : UsedClassifierSymbolStatus {
        override val isUnlinked get() = true
        var isPatched = false // To avoid re-patching what already has been patched.
    }

    /** IR symbol of linked classifier. */
    object Linked : UsedClassifierSymbolStatus {
        override val isUnlinked get() = false
    }

    companion object {
        val UsedClassifierSymbolStatus?.isUnlinked: Boolean get() = this?.isUnlinked == true
    }
}

internal class UsedClassifierSymbols {
    private val linkedSymbols = THashSet<IrClassifierSymbol>()
    private val unlinkedSymbols = THashMap<IrClassifierSymbol, UsedClassifierSymbolStatus.Unlinked>()

    fun forEachClassSymbolToPatch(patchAction: (IrClassSymbol) -> Unit) {
        val commonSymbols = linkedSymbols intersect unlinkedSymbols.keys
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

    operator fun get(symbol: IrClassifierSymbol): UsedClassifierSymbolStatus? =
        if (symbol in linkedSymbols) UsedClassifierSymbolStatus.Linked else unlinkedSymbols[symbol]

    fun registerUnlinked(symbol: IrClassifierSymbol): Boolean {
        unlinkedSymbols[symbol] = UsedClassifierSymbolStatus.Unlinked()
        return true
    }

    fun registerLinked(symbol: IrClassifierSymbol): Boolean {
        linkedSymbols += symbol
        return false
    }
}
