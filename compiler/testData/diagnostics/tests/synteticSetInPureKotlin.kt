class Container<E> {

    var w: Wrapper<E>? = null;

}

class Wrapper<W>

fun inn(container: Container<in String>, wrapper: Wrapper<Any>) {
    <!SETTER_PROJECTED_OUT!>container.w<!> = wrapper
}
