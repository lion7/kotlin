// TARGET_BACKEND: JVM_IR

// FILE: B.java

public abstract class B implements A {
    public String size = "FAIL";
}

// FILE: main.kt

interface A {
    val size: String
}

class C : B() {
    @Suppress("DERIVED_CLASS_PROPERTY_SHADOWS_BASE_CLASS_FIELD")
    override val size: String get() = "OK"
}

@Suppress("BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY")
fun box() = C().size

