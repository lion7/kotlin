/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma clang diagnostic ignored "-Watomic-alignment"

#include "WeakRefBarriers.hpp"

#include "PointerBits.h"

#include <atomic>

using namespace kotlin;

namespace {

inline constexpr unsigned markBit = 1;

using WeakRefReadType = ObjHeader*(*)(ObjHeader* const *, ObjHeader**) noexcept;

std::atomic<WeakRefReadType> weakRefReadImpl = nullptr;

OBJ_GETTER(weakRefReadNoBarriers, ObjHeader* const * weakRefAddress) noexcept {
    ObjHeader* value;
    __atomic_load(weakRefAddress, &value, __ATOMIC_RELAXED);
    RETURN_OBJ(clearPointerBits(value, markBit));
}

OBJ_GETTER(weakRefReadWithBarriers, ObjHeader* const * weakRefAddress) noexcept {
    ObjHeader* value;
    __atomic_load(weakRefAddress, &value, __ATOMIC_RELAXED);
    if (!hasPointerBits(value, markBit)) {
        return nullptr;
    }
    RETURN_OBJ(clearPointerBits(value, markBit));
}

}

void gc::enableWeakRefBarriers() noexcept {
    // Happens inside STW.
    weakRefReadImpl.store(weakRefReadWithBarriers, std::memory_order_relaxed);
}

void gc::disableWeakRefBarriers() noexcept {
    // Happens inside STW.
    weakRefReadImpl.store(weakRefReadNoBarriers, std::memory_order_relaxed);
}

OBJ_GETTER(gc::weakRefRead, ObjHeader* const * weakRefAddress) noexcept {
    // weakRefReadImpl only changes inside STW. Access is always synchronized.
    auto* impl = weakRefReadImpl.load(std::memory_order_relaxed);
    RETURN_RESULT_OF(impl, weakRefAddress);
}

ObjHeader* gc::weakRefReadUnsafe(ObjHeader* const * weakRefAddress) noexcept {
    return clearPointerBits(*weakRefAddress, markBit);
}

void gc::weakRefMark(ObjHeader** weakRefAddress) noexcept {
    ObjHeader* value;
    __atomic_load(weakRefAddress, &value, __ATOMIC_RELAXED);
    while (true) {
        ObjHeader* desired = setPointerBits(value, markBit);
        if (desired == value)
            return;
        bool result = __atomic_compare_exchange_n(weakRefAddress, &value, desired, true, __ATOMIC_RELAXED, __ATOMIC_RELAXED);
        if (result)
            return;
    }
}

void gc::weakRefResetMark(ObjHeader** weakRefAddress) noexcept {
    ObjHeader* value;
    __atomic_load(weakRefAddress, &value, __ATOMIC_RELAXED);
    while (true) {
        ObjHeader* desired = clearPointerBits(value, markBit);
        if (desired == value)
            return;
        bool result = __atomic_compare_exchange_n(weakRefAddress, &value, desired, true, __ATOMIC_RELAXED, __ATOMIC_RELAXED);
        if (result)
            return;
    }
}
