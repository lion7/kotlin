/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"

namespace kotlin::gc {

void enableWeakRefBarriers() noexcept;
void disableWeakRefBarriers() noexcept;

OBJ_GETTER(weakRefRead, ObjHeader* const * weakRefAddress) noexcept;
ObjHeader* weakRefReadUnsafe(ObjHeader* const * weakRefAddress) noexcept;
void weakRefMark(ObjHeader** weakRefAddress) noexcept;
void weakRefResetMark(ObjHeader** weakRefAddress) noexcept;

}
