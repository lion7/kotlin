// This file is compiled into each stepping test only if the WITH_STDLIB directive IS specified.

package testUtils

internal val stdlibFqNames = mapOf(
    Pair::class to "kotlin.Pair",
    Triple::class to "kotlin.Triple",
    HashMap::class to "kotlin.collections.HashMap",
)

private object EmptyContinuation: Continuation<Any?> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

@JsExport
fun makeEmptyContinuation(): dynamic = EmptyContinuation
