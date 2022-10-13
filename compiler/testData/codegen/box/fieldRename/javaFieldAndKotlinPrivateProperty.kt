// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: we resolve to field, but should resolve to property (see KT-54393)
// More or less duplicates the case in KT-34943

// FILE: BaseJava.java
public class BaseJava {
    public String a = "FAIL";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "OK"

    fun x() = a
}

fun box() = Derived().x()
