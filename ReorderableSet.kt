package fr.gouvernathor.orderbitfield

/**
 * Like a set, elements are unique.
 * Elements are not sorted.
 * Like a list, elements are ordered.
 * Unlike a list, elements are not indexed.
 * Technically they are, but their indices are not contiguous, and opaque (you can't find an element by its index).
 * Unlike any mutable collection, you can't insert an element without specifying where to insert it relative to the existing elements.
 * The container is mutable in two ways: you can add and remove elements, and you can also efficiently reorder them.
 *
 * Unless otherwise specified, mutation methods generally ignore
 * whether the elements to insert are already in the container or not.
 */
public interface ReorderableSet<E>: Set<E> {
    /**
     * Yields the elements of the container, in unspecified order.
     * May be a cheaper operation than iterating over the container,
     * which does it in order.
     */
    fun elements(): Iterable<E>

    /**
     * A key function, to be passed in the sortedBy function.
     */
    val sortKey: (E) -> Comparable<*>

    /**
     * Put the elements between the stard and end elements.
     * It is an error to provide the same element as both start and end,
     * or to provide start or end elements which are not in the container.
     * It is unspecified and at the very least unoptimized to include the start or end element in the new elements.
     * When providing non-contiguous elements as start and end, the ordering
     * between, on one part, any element previously between start and end,
     * and on the other part, any of the new elements, is unspecified.
     * To avoid these issues, prefer using the putNextTo method.
     */
    fun putBetween(start: E, end: E, vararg newElements: E): Unit

    /**
     * Put the elements at one end of the container.
     */
    fun putAtEnd(vararg newElements: E, last: Boolean = true): Unit

    /**
     * Put the elements next to the given anchor element.
     * It is an error to provide an anchor element which is not in the container.
     * Including the anchor element in the new elements is allowed, however.
     */
    fun putNextTo(anchor: E, vararg newElements: E, after: Boolean = true): Unit

    /**
     * Recompute and optimize the indices of the elements, keeping the order.
     */
    fun recompute(): Unit

    fun <R : Comparable<R>> sortBy(selector: (E) -> R): Unit
    fun sortWith(comparator: Comparator<in E>): Unit
    fun <R : Comparable<R>> sortTrancheBy(start: E?, end: E?, selector: (E) -> R): Unit
    fun sortTrancheWith(start: E?, end: E?, comparator: Comparator<in E>): Unit

    /**
     * Remove and return the last or first element of the container.
     * It is an error to call this method on an empty container.
     */
    fun popItem(last: Boolean = true): E

    /**
     * Remove and return the last or first (at most) n elements of the container.
     * This method may be called on an empty or unsufficiently filled container,
     * where it will return a shorter number of elements than requested.
     */
    fun popItems(n: Int, last: Boolean = true): Iterable<E>

    /**
     * Remove the element from the container, and return whether it was in it.
     */
    fun remove(element: E): Boolean

    /**
     * Remove the elements from the container.
     */
    fun removeAll(elements: Iterable<E>): Unit
    fun removeAll(elements: Sequence<E>): Unit
}
fun <E : Comparable<E>> ReorderableSet<E>.sort() {
    this.sortBy({ it })
}
fun <E : Comparable<E>> ReorderableSet<E>.sortTranche(start: E?, end: E?) {
    this.sortTrancheBy(start, end, { it })
}
