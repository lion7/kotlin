// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: resolved to the field instead of property
// Field VS property: case 4.1
// See KT-54393 for details (fails with exception)
// DUMP_IR

// FILE: BaseJava.java
public class BaseJava {
    public String a = "FAIL";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    val a = "OK"
}

fun box() = Derived().a