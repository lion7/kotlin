// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 class FunInterfaceWithInlineNAnyKt\$box\$t\$1

inline class Z(val value: Any?)

fun interface IFooZ {
    fun foo(x: Z): Z
}

fun foo1(fs: IFooZ) = fs.foo(Z(1))

fun box(): String {
    val t = foo1 { Z((it.value as Int) + 41) }
    if (t.value != 42) return "Failed: t=$t"

    return "OK"
}