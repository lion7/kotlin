/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

private let REPEAT_COUNT = 10000
private let WORKERS_COUNT = 10
private let REFERENCES_COUNT = 3

private class ReferenceWrapper {
    private weak var weakRef: KotlinData?
    private var strongRef: KotlinData?

    init() {
        let data = KotlinData(x: Int32.random(in: 1 ... 1000))
        self.strongRef = data
        self.weakRef = data
    }

    var value: Int32? {
        return self.weakRef?.x
    }

    func dispose() {
        self.strongRef = nil
    }

    func stress() -> Int32 {
        var counter: Int32 = 0
        for _ in 1...REPEAT_COUNT {
            counter += self.value ?? 0
        }
        return counter
    }
}

private func deadReferenceWrapper() -> ReferenceWrapper {
    let ref = ReferenceWrapper()
    ref.dispose()
    GCKt.collect()
    return ref
}

class WeakRefBenchmark {
    private let aliveRef = ReferenceWrapper()
    private let deadRef = deadReferenceWrapper()

    // Access alive reference.
    func aliveReference() {
        let counter = aliveRef.stress()
        if counter == 0 {
            fatalError()
        }
    }

    // Access dead reference.
    func deadReference() {
        let counter = deadRef.stress()
        if counter != 0 {
            fatalError()
        }
    }

    // Access reference that is nulled out in the middle.
    func dyingReference() {
        let ref = ReferenceWrapper()

        ref.dispose()
        GCKt.schedule()

        let counter = ref.stress()
        Blackhole.companion.consume(value: counter)
    }
}

class WeakRefBenchmarkThreads {
    private var workers: [Worker] = []
    private var aliveRefs: [ReferenceWrapper] = []
    private var deadRefs: [ReferenceWrapper] = []

    init() {
        for _ in 1...WORKERS_COUNT {
            workers.append(startWorker())
        }
        for _ in 1...REFERENCES_COUNT {
            aliveRefs.append(ReferenceWrapper())
            deadRefs.append(ReferenceWrapper())
        }
        for ref in deadRefs {
            ref.dispose()
        }
        GCKt.collect()
    }

    private func stress(_ refs: [ReferenceWrapper]) -> [Future<Int32>] {
        var futures: [Future<Int32>] = []
        for (index, worker) in workers.enumerated() {
            let ref = refs[index % REFERENCES_COUNT]
            let future = try! worker.execute() {
                ref.stress()
            }
            futures.append(future)
        }
        return futures
    }

    private func consumeFutures(_ futures: [Future<Int32>]) -> Int32 {
        var counter: Int32 = 0
        for future in futures {
            counter += future.result
        }
        return counter
    }

    func teardown() {
        for worker in workers {
            worker.terminate()
        }
    }

    // Access alive references on multiple threads.
    func aliveReferences() {
        let futures = stress(aliveRefs)
        let counter = consumeFutures(futures)
        if counter == 0 {
            fatalError()
        }
    }

    // Access dead references on multiple threads.
    func deadReferences() {
        let futures = stress(deadRefs)
        let counter = consumeFutures(futures)
        if counter != 0 {
            fatalError()
        }
    }

    // Concurrently access references that are nulled out on other threads.
    func dyingReferences() {
        var refs: [ReferenceWrapper] = []
        for _ in 1...REFERENCES_COUNT {
            refs.append(ReferenceWrapper())
        }

        let futures = stress(refs)

        for ref in refs {
            ref.dispose()
        }
        GCKt.schedule()

        Blackhole.companion.consume(value: consumeFutures(futures))
    }
}
