/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.internal.GC
import kotlin.native.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.cinterop.StableRef
import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

private const val REPEAT_COUNT = BENCHMARK_SIZE
private const val WORKERS_COUNT = 10
private const val REFERENCES_COUNT = 3

private class Data(var x: Int = Random.nextInt(1000) + 1)

private class ReferenceWrapper private constructor(
    data: Data
) {
    private val weak = WeakReference(data)
    private val strong = StableRef.create(data)

    val value: Int?
        get() = weak.value?.x

    fun dispose() {
        strong.dispose()
    }

    companion object {
        fun create() = ReferenceWrapper(Data())
    }
}

private fun ReferenceWrapper.stress() = (1..REPEAT_COUNT).sumOf {
    this.value ?: 0
}

open class WeakRefBenchmark {
    private val aliveRef = ReferenceWrapper.create()
    private val deadRef = ReferenceWrapper.create().apply {
        dispose()
        GC.collect()
    }

    // Access alive reference.
    fun aliveReference() {
        assertNotEquals(0, aliveRef.stress())
    }

    // Access dead reference.
    fun deadReference() {
        assertEquals(0, deadRef.stress())
    }

    // Access reference that is nulled out in the middle.
    fun dyingReference() {
        val ref = ReferenceWrapper.create()

        ref.dispose()
        GC.schedule()

        Blackhole.consume(ref.stress())
    }
}

open class WeakRefBenchmarkWorkers {
    private val workers = Array(WORKERS_COUNT) { Worker.start() }
    private val aliveRefs = Array(REFERENCES_COUNT) { ReferenceWrapper.create() }
    private val deadRefs = Array(REFERENCES_COUNT) {
        ReferenceWrapper.create().apply {
            dispose()
        }
    }.also {
        GC.collect()
    }

    private fun Array<ReferenceWrapper>.stress() = workers.mapIndexed { index, worker ->
        worker.execute(TransferMode.SAFE, { this[index % REFERENCES_COUNT] }) {
            it.stress()
        }
    }

    fun teardown() {
        workers.forEach {
            it.requestTermination().result
        }
    }

    // Access alive references on multiple threads.
    fun aliveReferences() {
        val futures = aliveRefs.stress()
        assertNotEquals(0, futures.sumOf { it.result })
    }

    // Access dead references on multiple threads.
    fun deadReferences() {
        val futures = deadRefs.stress()
        assertEquals(0, futures.sumOf { it.result })
    }

    // Concurrently access references that are nulled out on other threads.
    fun dyingReferences() {
        val refs = Array(REFERENCES_COUNT) { ReferenceWrapper.create() }

        val futures = refs.stress()

        refs.forEach { it.dispose() }
        GC.schedule()

        Blackhole.consume(futures.sumOf { it.result })
    }
}
