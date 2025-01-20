// don't use MutableCollection (the add and addAll methods don't provide where to insert the elements)

// TODO maybe make it implement the Set interface
/**
 * Like a set, elements are unique.
 * Unlike a set, elements are ordered.
 * Unlike a list, elements' indices are not contiguous, and opaque (you can't find an element by its index).
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
     * When providing non-contiguous elements as start and end, the ordering between,
     * on one part, any element previously between start and end, and,
     * on the other part, any of the new elements, is unspecified.
     */
    fun putBetween(start: E, end: E, vararg newElements: E): Unit
    // TODO maybe deprecate this method in favor of putNextTo, which has less consistency constraints

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
     * Remove the element from the container, and return whether it was in it.
     */
    fun remove(element: E): Boolean

    /**
     * Remove the elements from the container.
     */
    fun removeAll(elements: Iterable<E>): Unit
    fun removeAll(elements: Sequence<E>): Unit
}

abstract class AbstractReorderableSet<E>: ReorderableSet<E>, AbstractSet<E>() {
    abstract protected fun update(pairs: Iterable<Pair<E, OrderBitField>>, mayBeNew: Boolean = true): Unit

    override abstract val sortKey: (E) -> OrderBitField

    override fun putBetween(start: E, end: E, vararg newElements: E) {
        require(start != end) { "start and end must be different" }
        val codes = OrderBitField.between(sortKey(start), sortKey(end), newElements.size.toUInt())
        update((newElements zip codes.toList()))
    }

    override fun putAtEnd(vararg newElements: E, last: Boolean) {
        val codes: Sequence<OrderBitField>
        if (isEmpty()) {
            codes = OrderBitField.initial(newElements.size.toUInt())
        } else if (last) {
            codes = OrderBitField.after(elements().maxOf(sortKey), newElements.size.toUInt())
        } else {
            codes = OrderBitField.before(elements().minOf(sortKey), newElements.size.toUInt())
        }
        update((newElements zip codes.toList()))
    }

    override fun putNextTo(anchor: E, vararg newElements: E, after: Boolean) {
        require(anchor in this) { "anchor must be in the container" }
        val anchorCode = sortKey(anchor)
        val codes: Sequence<OrderBitField>
        if (after) {
            val end = elements().map(sortKey).filter { it > anchorCode }.minOrNull()
            if (end == null) {
                codes = OrderBitField.after(anchorCode, newElements.size.toUInt())
            } else {
                codes = OrderBitField.between(anchorCode, end, newElements.size.toUInt())
            }
        } else {
            val start = elements().map(sortKey).filter { it < anchorCode }.maxOrNull()
            if (start == null) {
                codes = OrderBitField.before(anchorCode, newElements.size.toUInt())
            } else {
                codes = OrderBitField.between(start, anchorCode, newElements.size.toUInt())
            }
        }
        update((newElements zip codes.toList()))
    }

    override fun recompute() {
        val codes = OrderBitField.initial(size.toUInt())
        update((this zip codes.toList()), false) // not elements(), which is unordered
    }

    override fun popItem(last: Boolean): E {
        require(isNotEmpty()) { "the container is empty" }
        val element: E
        if (last) {
            element = elements().maxBy(sortKey)
        } else {
            element = elements().minBy(sortKey)
        }
        remove(element)
        return element
    }

    override fun popItems(n: Int, last: Boolean): Iterable<E> {
        val toPop = n.coerceAtMost(size)
        val sortedSeq = if (last)
            // this is faster if Sequence::sortedBy is lazy-optimized
            elements().asSequence().sortedByDescending(sortKey) else
            elements().asSequence().sortedBy(sortKey)
        val rv = sortedSeq.take(toPop).toList()
        removeAll(rv)
        return rv
    }
}

class MapBasedReorderableSet<E>(
    vararg elements: E,
): AbstractReorderableSet<E>() {
    private val store: MutableMap<E, OrderBitField>
    init {
        if (elements.isEmpty()) {
            store = mutableMapOf()
        } else {
            val codes = OrderBitField.initial(elements.size.toUInt()).toList()
            store = (elements zip codes).toMap().toMutableMap()
        }
    }

    // AbstractReorderableSet method

    override fun update(pairs: Iterable<Pair<E, OrderBitField>>, mayBeNew: Boolean) {
        store.putAll(pairs)
    }

    // Collection methods

    override val size: Int
        get() = store.size

    // overridden for performance
    override fun contains(element: E): Boolean = store.contains(element)

    // overridden for performance
    override fun containsAll(elements: Collection<E>): Boolean = store.keys.containsAll(elements)

    operator override fun iterator(): Iterator<E> {
        return store.keys.sortedBy { store[it] }.iterator()
    }

    // remaining ReorderableSet methods

    override fun elements(): Iterable<E> {
        return store.keys
    }

    override val sortKey: (E) -> OrderBitField = { store[it]!! }

    override fun remove(element: E): Boolean {
        return store.remove(element) != null
    }

    override fun removeAll(elements: Iterable<E>) {
        store.keys.removeAll(elements)
    }
    override fun removeAll(elements: Sequence<E>) {
        store.keys.removeAll(elements)
    }
}

class SetLambdaBasedReorderableSet<E>(
    private val getCode: (E) -> OrderBitField,
    private val setCode: (E, OrderBitField) -> Unit,
    vararg elements: E,
): AbstractReorderableSet<E>() {
    private val store: MutableSet<E> = elements.toMutableSet()

    // AbstractReorderableSet method

    override fun update(pairs: Iterable<Pair<E, OrderBitField>>, mayBeNew: Boolean) {
        pairs.forEach { (element, code) -> setCode(element, code) }
        if (mayBeNew) {
            store.addAll(pairs.map { it.first })
        }
    }

    // Collection methods

    override val size: Int
        get() = store.size

    // overridden for performance
    override fun contains(element: E): Boolean = store.contains(element)

    // overridden for performance
    override fun containsAll(elements: Collection<E>): Boolean = store.containsAll(elements)

    operator override fun iterator(): Iterator<E> {
        return store.sortedBy { getCode(it) }.iterator()
    }

    // ReorderableSet methods

    override fun elements(): Iterable<E> {
        return store
    }

    override val sortKey: (E) -> OrderBitField = getCode

    override fun remove(element: E): Boolean {
        return store.remove(element)
    }

    override fun removeAll(elements: Iterable<E>) {
        store.removeAll(elements)
    }
    override fun removeAll(elements: Sequence<E>) {
        store.removeAll(elements)
    }
}
