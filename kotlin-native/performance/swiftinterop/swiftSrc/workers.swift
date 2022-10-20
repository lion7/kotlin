/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class Future<T> {
    private var state: Promise<T>

    fileprivate init(_ state: Promise<T>) {
        self.state = state
    }

    var result: T {
        get {
            return self.state.value
        }
    }
}

enum WorkerError : Error {
    case terminated
}

protocol Worker {
    func execute<T>(closure: @escaping () -> T) throws -> Future<T>
    func terminate()
}

func startWorker() -> Worker {
    let worker = WorkerImpl()
    worker.start()
    return worker
}

private class Promise<T> {
    private var _value: T? = nil
    private let lock = NSCondition()

    var value: T {
        get {
            lock.lock()
            while _value == nil {
                lock.wait()
            }
            let result = _value!
            lock.unlock()
            return result
        }
        set(newValue) {
            lock.lock()
            if _value != nil {
                fatalError("_value must not be already set but is '\(_value!)'")
            }
            _value = newValue
            lock.broadcast()
            lock.unlock()
        }
    }
}

private class WorkerImpl: Thread, Worker {
    private let workLock = NSCondition()
    private var queue: [() -> Void] = []
    private var _terminate = false

    private let terminateLock = NSCondition()
    private var done = false

    override func main() {
        while true {
            workLock.lock()
            while queue.count == 0 && !_terminate {
                workLock.wait()
            }
            if queue.count == 0 {
                if !_terminate {
                    fatalError("Empty queue but terminate is not set")
                }
                workLock.unlock()
                terminateLock.lock()
                done = true
                terminateLock.broadcast()
                terminateLock.unlock()
                return
            }
            let workItem = queue.remove(at: 0)
            workLock.unlock()
            workItem()
        }
    }

    func terminate() {
        workLock.lock()
        _terminate = true
        workLock.broadcast()
        workLock.unlock()

        terminateLock.lock()
        while !done {
            terminateLock.wait()
        }
        terminateLock.unlock()
    }

    func execute<T>(closure: @escaping () -> T) throws -> Future<T> {
        let promise = Promise<T>()

        workLock.lock()
        if _terminate {
            throw WorkerError.terminated
        }
        queue.append({
            let result = closure()
            promise.value = result
        })
        workLock.broadcast()
        workLock.unlock()

        return Future<T>(promise)
    }
}
