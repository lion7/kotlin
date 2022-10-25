/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.native.ref.*

fun testArrayAllocation(size: Int) {
    val arr = IntArray(size)
    // Put array into weak reference to force it be allocated on the heap.
    @Suppress("UNUSED_VARIABLE")
    val weakArr = WeakReference(arr)
    // Force a write into the memory.
    // TODO: How to make sure the optimizer never deletes this write?
    arr[size - 1] = 42
    assertEquals(42, arr[size - 1])
}

@Test
fun sanity() {
    // Should always succeed everywhere
    testArrayAllocation(1 shl 10)
}

@Test
fun test() {
    val size = 1 shl 30
    when (Platform.cpuArchitecture.bitness) {
        32 -> assertFailsWith<OutOfMemoryError> {
            testArrayAllocation(size)
        }
        64 -> testArrayAllocation(size)
        else -> TODO()
    }
}
