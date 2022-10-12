/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cfg

import javaslang.Function1
import javaslang.Tuple1
import javaslang.Tuple2
import javaslang.Tuple3
import javaslang.collection.*
import javaslang.collection.List
import javaslang.collection.Map
import javaslang.collection.Queue
import javaslang.collection.Set
import javaslang.collection.Stack
import javaslang.collection.Vector
import javaslang.control.Either
import javaslang.control.Option
import javaslang.control.Try
import org.jetbrains.kotlin.util.javaslang.ImmutableHashMap
import org.jetbrains.kotlin.util.javaslang.ImmutableMap
import java.io.PrintStream
import java.io.PrintWriter
import java.util.*
import java.util.function.*
import java.util.function.Function
import java.util.stream.Stream

interface ReadOnlyControlFlowInfo<K : Any, D : Any> {
    fun getOrNull(key: K): D?
    // Only used in tests
    fun asMap(): ImmutableMap<K, D>
}

abstract class ControlFlowInfo<S : ControlFlowInfo<S, K, D>, K : Any, D : Any>
internal constructor(
    protected val map: ImmutableMap<K, D> = ImmutableHashMap.empty()
) : ImmutableMap<K, D> by map, ReadOnlyControlFlowInfo<K, D> {
    protected abstract fun copy(newMap: ImmutableMap<K, D>): S

    override fun put(key: K, value: D): S = put(key, value, this[key].getOrElse(null as D?))

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the persistent hashmap again
     */
    fun put(key: K, value: D, oldValue: D?): S {
        @Suppress("UNCHECKED_CAST")
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as S
        return copy(map.put(key, value))
    }

    override fun getOrNull(key: K): D? = this[key].getOrElse(null as D?)
    override fun asMap() = this

    fun retainAll(predicate: (K) -> Boolean): S = copy(map.removeAll(map.keySet().filterNot(predicate)))

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()

    override fun fold(zero: Tuple2<K, D>?, combine: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Tuple2<K, D> {
        return super.fold(zero, combine)
    }

    override fun <U : Any?> foldLeft(zero: U, f: BiFunction<in U, in Tuple2<K, D>, out U>?): U {
        return super.foldLeft(zero, f)
    }

    override fun <U : Any?> foldRight(zero: U, f: BiFunction<in Tuple2<K, D>, in U, out U>?): U {
        return super.foldRight(zero, f)
    }

    override fun reduce(op: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Tuple2<K, D> {
        return super.reduce(op)
    }

    override fun reduceOption(op: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.reduceOption(op)
    }

    override fun reduceLeft(op: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Tuple2<K, D> {
        return super.reduceLeft(op)
    }

    override fun reduceLeftOption(op: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.reduceLeftOption(op)
    }

    override fun reduceRight(op: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Tuple2<K, D> {
        return super.reduceRight(op)
    }

    override fun reduceRightOption(op: BiFunction<in Tuple2<K, D>, in Tuple2<K, D>, out Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.reduceRightOption(op)
    }

    override fun forEach(action: BiConsumer<K, D>?) {
        super.forEach(action)
    }

    override fun forEach(action: Consumer<in Tuple2<K, D>>?) {
        super.forEach(action)
    }

    override fun spliterator(): Spliterator<Tuple2<K, D>> {
        return super.spliterator()
    }

    override fun contains(element: Tuple2<K, D>?): Boolean {
        return super.contains(element)
    }

    override fun <U : Any?> corresponds(that: MutableIterable<U>?, predicate: BiPredicate<in Tuple2<K, D>, in U>?): Boolean {
        return super.corresponds(that, predicate)
    }

    override fun eq(o: Any?): Boolean {
        return super.eq(o)
    }

    override fun exists(predicate: Predicate<in Tuple2<K, D>>?): Boolean {
        return super.exists(predicate)
    }

    override fun forAll(predicate: Predicate<in Tuple2<K, D>>?): Boolean {
        return super.forAll(predicate)
    }

    override fun get(): Tuple2<K, D> {
        return super.get()
    }

    override fun getOption(): Option<Tuple2<K, D>> {
        return super.getOption()
    }

    override fun getOrElse(other: Tuple2<K, D>?): Tuple2<K, D> {
        return super.getOrElse(other)
    }

    override fun getOrElse(supplier: Supplier<out Tuple2<K, D>>?): Tuple2<K, D> {
        return super.getOrElse(supplier)
    }

    override fun <X : Throwable?> getOrElseThrow(supplier: Supplier<X>?): Tuple2<K, D> {
        return super.getOrElseThrow(supplier)
    }

    override fun getOrElseTry(supplier: Try.CheckedSupplier<out Tuple2<K, D>>?): Tuple2<K, D> {
        return super.getOrElseTry(supplier)
    }

    override fun isEmpty(): Boolean {
        return super.isEmpty()
    }

    override fun isSingleValued(): Boolean {
        return super.isSingleValued()
    }

    override fun <U : Any?> map(mapper: Function<in Tuple2<K, D>, out U>?): Seq<U> {
        return super.map(mapper)
    }

    override fun stringPrefix(): String {
        TODO("Not yet implemented")
    }

    override fun out(out: PrintStream?) {
        super.out(out)
    }

    override fun out(writer: PrintWriter?) {
        super.out(writer)
    }

    override fun stderr() {
        super.stderr()
    }

    override fun stdout() {
        super.stdout()
    }

    override fun toArray(): javaslang.collection.Array<Tuple2<K, D>> {
        return super.toArray()
    }

    override fun toCharSeq(): CharSeq {
        return super.toCharSeq()
    }

    override fun <C : MutableCollection<Tuple2<K, D>>?> toJavaCollection(factory: Supplier<C>?): C {
        return super.toJavaCollection(factory)
    }

    override fun toJavaArray(): Array<Any> {
        return super.toJavaArray()
    }

    override fun toJavaArray(componentType: Class<Tuple2<K, D>>?): Array<Tuple2<K, D>> {
        return super.toJavaArray(componentType)
    }

    override fun toJavaList(): MutableList<Tuple2<K, D>> {
        return super.toJavaList()
    }

    override fun <LIST : MutableList<Tuple2<K, D>>?> toJavaList(factory: Supplier<LIST>?): LIST {
        return super.toJavaList(factory)
    }

    override fun toJavaOptional(): Optional<Tuple2<K, D>> {
        return super.toJavaOptional()
    }

    override fun toJavaSet(): MutableSet<Tuple2<K, D>> {
        return super.toJavaSet()
    }

    override fun <SET : MutableSet<Tuple2<K, D>>?> toJavaSet(factory: Supplier<SET>?): SET {
        return super.toJavaSet(factory)
    }

    override fun toJavaStream(): Stream<Tuple2<K, D>> {
        return super.toJavaStream()
    }

    override fun <R : Any?> toLeft(right: Supplier<out R>?): Either<Tuple2<K, D>, R> {
        return super.toLeft(right)
    }

    override fun <R : Any?> toLeft(right: R): Either<Tuple2<K, D>, R> {
        return super.toLeft(right)
    }

    override fun toList(): List<Tuple2<K, D>> {
        return super.toList()
    }

    override fun toOption(): Option<Tuple2<K, D>> {
        return super.toOption()
    }

    override fun toQueue(): Queue<Tuple2<K, D>> {
        return super.toQueue()
    }

    override fun <L : Any?> toRight(left: Supplier<out L>?): Either<L, Tuple2<K, D>> {
        return super.toRight(left)
    }

    override fun <L : Any?> toRight(left: L): Either<L, Tuple2<K, D>> {
        return super.toRight(left)
    }

    override fun toSet(): Set<Tuple2<K, D>> {
        return super.toSet()
    }

    override fun toStack(): Stack<Tuple2<K, D>> {
        return super.toStack()
    }

    override fun toStream(): javaslang.collection.Stream<Tuple2<K, D>> {
        return super.toStream()
    }

    override fun toTry(): Try<Tuple2<K, D>> {
        return super.toTry()
    }

    override fun toTry(ifEmpty: Supplier<out Throwable>?): Try<Tuple2<K, D>> {
        return super.toTry(ifEmpty)
    }

    override fun toTree(): Tree<Tuple2<K, D>> {
        return super.toTree()
    }

    override fun toVector(): Vector<Tuple2<K, D>> {
        return super.toVector()
    }

    override fun average(): Option<Double> {
        return super.average()
    }

    override fun containsAll(elements: MutableIterable<Tuple2<K, D>>?): Boolean {
        return super.containsAll(elements)
    }

    override fun count(predicate: Predicate<in Tuple2<K, D>>?): Int {
        return super.count(predicate)
    }

    override fun existsUnique(predicate: Predicate<in Tuple2<K, D>>?): Boolean {
        return super.existsUnique(predicate)
    }

    override fun find(predicate: Predicate<in Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.find(predicate)
    }

    override fun findLast(predicate: Predicate<in Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.findLast(predicate)
    }

    override fun <U : Any?> flatMap(mapper: Function<in Tuple2<K, D>, out MutableIterable<U>>?): Seq<U> {
        return super.flatMap(mapper)
    }

    override fun hasDefiniteSize(): Boolean {
        return super.hasDefiniteSize()
    }

    override fun head(): Tuple2<K, D> {
        TODO("Not yet implemented")
    }

    override fun headOption(): Option<Tuple2<K, D>> {
        return super.headOption()
    }

    override fun isTraversableAgain(): Boolean {
        return super.isTraversableAgain()
    }

    override fun last(): Tuple2<K, D> {
        return super.last()
    }

    override fun lastOption(): Option<Tuple2<K, D>> {
        return super.lastOption()
    }

    override fun length(): Int {
        return super.length()
    }

    override fun max(): Option<Tuple2<K, D>> {
        return super.max()
    }

    override fun maxBy(comparator: Comparator<in Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.maxBy(comparator)
    }

    override fun <U : Comparable<U>?> maxBy(f: Function<in Tuple2<K, D>, out U>?): Option<Tuple2<K, D>> {
        return super.maxBy(f)
    }

    override fun min(): Option<Tuple2<K, D>> {
        return super.min()
    }

    override fun minBy(comparator: Comparator<in Tuple2<K, D>>?): Option<Tuple2<K, D>> {
        return super.minBy(comparator)
    }

    override fun <U : Comparable<U>?> minBy(f: Function<in Tuple2<K, D>, out U>?): Option<Tuple2<K, D>> {
        return super.minBy(f)
    }

    override fun mkString(): String {
        return super.mkString()
    }

    override fun mkString(delimiter: CharSequence?): String {
        return super.mkString(delimiter)
    }

    override fun mkString(prefix: CharSequence?, delimiter: CharSequence?, suffix: CharSequence?): String {
        return super.mkString(prefix, delimiter, suffix)
    }

    override fun nonEmpty(): Boolean {
        return super.nonEmpty()
    }

    override fun product(): Number {
        return super.product()
    }

    override fun <U : Any?> scanLeft(zero: U, operation: BiFunction<in U, in Tuple2<K, D>, out U>?): Seq<U> {
        return super.scanLeft(zero, operation)
    }

    override fun <U : Any?> scanRight(zero: U, operation: BiFunction<in Tuple2<K, D>, in U, out U>?): Seq<U> {
        return super.scanRight(zero, operation)
    }

    override fun sum(): Number {
        return super.sum()
    }

    override fun <T1 : Any?, T2 : Any?> unzip(unzipper: BiFunction<in K, in D, Tuple2<out T1, out T2>>?): Tuple2<Seq<T1>, Seq<T2>> {
        return super.unzip(unzipper)
    }

    override fun <T1 : Any?, T2 : Any?> unzip(unzipper: Function<in Tuple2<K, D>, Tuple2<out T1, out T2>>?): Tuple2<Seq<T1>, Seq<T2>> {
        return super.unzip(unzipper)
    }

    override fun <T1 : Any?, T2 : Any?, T3 : Any?> unzip3(unzipper: BiFunction<in K, in D, Tuple3<out T1, out T2, out T3>>?): Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> {
        return super.unzip3(unzipper)
    }

    override fun <T1 : Any?, T2 : Any?, T3 : Any?> unzip3(unzipper: Function<in Tuple2<K, D>, Tuple3<out T1, out T2, out T3>>?): Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> {
        return super.unzip3(unzipper)
    }

    override fun <U : Any?> zip(that: MutableIterable<U>?): Seq<Tuple2<Tuple2<K, D>, U>> {
        return super.zip(that)
    }

    override fun <U : Any?> zipAll(that: MutableIterable<U>?, thisElem: Tuple2<K, D>?, thatElem: U): Seq<Tuple2<Tuple2<K, D>, U>> {
        return super.zipAll(that, thisElem, thatElem)
    }

    override fun zipWithIndex(): Seq<Tuple2<Tuple2<K, D>, Long>> {
        return super.zipWithIndex()
    }

    override fun arity(): Int {
        return super.arity()
    }

    override fun curried(): Function1<K, D> {
        return super.curried()
    }

    override fun tupled(): Function1<Tuple1<K>, D> {
        return super.tupled()
    }

    override fun reversed(): Function1<K, D> {
        return super.reversed()
    }

    override fun memoized(): Function1<K, D> {
        return super.memoized()
    }

    override fun isMemoized(): Boolean {
        return super.isMemoized()
    }

    override fun apply(key: K): D {
        return super.apply(key)
    }

    override fun <V : Any?> compose(before: Function<in V, out K>): Function1<V, D> {
        return super.compose(before)
    }

    override fun <V : Any?> andThen(after: Function<in D, out V>): Function1<K, V> {
        return super.andThen(after)
    }

    override fun containsValue(value: D): Boolean {
        return super.containsValue(value)
    }

    override fun <U : Any?> transform(f: Function<in Map<K, D>, out U>?): U {
        return super.transform(f)
    }

    override fun <U : Any?> traverse(mapper: BiFunction<K, D, out U>?): Seq<U> {
        return super.traverse(mapper)
    }
}