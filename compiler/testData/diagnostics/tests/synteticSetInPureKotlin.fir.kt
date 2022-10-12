class Container<E> {

    var w: Wrapper<E>? = null;

}

class Wrapper<W>

fun inn(container: Container<in String>, wrapper: Wrapper<Any>) {
    container.w = <!ASSIGNMENT_TYPE_MISMATCH!>wrapper<!>
}
