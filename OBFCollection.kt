/**
 * Like a set, elements are unique.
 * Unlike a set, elements are ordered.
 * Unlike a list, elements' indices are not contiguous, and opaque (you can't find an element by its index).
 *
 * Unless otherwise specified, mutation methods generally ignore
 * whether the elements to insert are already in the container or not.
 */
public interface IOBFContainer<E>: Collection<E> {
    /**
     * Put the elements between the stard and end elements.
     */
    fun putBetween(start: E, end: E, vararg newElements: E): Unit

    /**
     * Put the elements at one end of the container.
     */
    fun putAtEnd(vararg newElements: E, last: Boolean = true): Unit

    /**
     * Put the elements next to the given anchor element.
     * It is an error to provide an element which is not in the container.
     * It is unspecified and at the very least unoptimized to include the anchor element in the new elements.
     */
    fun putNextTo(anchor: E, vararg newElements: E, after: Boolean = true): Unit

    /**
     * Recompute and optimize the indices of the elements, keeping the order.
     */
    fun recompute(): Unit

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
     * Remove the elements from the container.
     * It is an error to provide any element which is not in the container.
     */
    fun remove(vararg elements: E): Unit

    /**
     * Remove the elements from the container, if they are in it.
     */
    fun discard(vararg elements: E): Unit

    // contains iter len : provided by Collection
}
