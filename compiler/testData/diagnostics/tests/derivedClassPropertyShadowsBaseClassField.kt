// FILE: Base.java

public class Base {
    public String a = "a";

    public String b = "b";

    public String c = "c";
}

// FILE: test.kt

class Derived : Base() {
    val a = "aa"

    <!DERIVED_CLASS_PROPERTY_SHADOWS_BASE_CLASS_FIELD("with custom getter; Base")!>val b get() = "bb"<!>

    <!DERIVED_CLASS_PROPERTY_SHADOWS_BASE_CLASS_FIELD("with lateinit; Base")!>lateinit var c: String<!>
}

fun test(d: Derived) {
    d.a
    d.<!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY!>b<!>
    d.<!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY!>c<!>
}
