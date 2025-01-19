/**
 * Like a set, elements are unique.
 * Unlike a set, elements are ordered.
 * Unlike a list, elements' indices are not contiguous, and opaque (you can't find an element by its index).
 *
 * Unless otherwise specified, mutation methods generally ignore
 * whether the elements to insert are already in the container or not.
 */
public interface ReorderableContainer<E>: Collection<E> {
    /**
     * Yields the elements of the container, in unspecified order.
     * May be a cheaper operation than iterating over the container,
     * which does it in order.
     */
    fun elements(): Iterable<E>

    /**
     * Extracts a key function, to be passed in the sortedBy function.
     */
    fun sortKey(): (E) -> Comparable<*>

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
     * Remove the elements from the container.
     * It is an error to provide any element which is not in the container.
     */
    fun remove(vararg elements: E): Unit

    /**
     * Remove the elements from the container, if they are in it.
     */
    fun discard(vararg elements: E): Unit
}


class MapBasedReorderableContainer<E>(vararg elements: E): ReorderableContainer<E>, AbstractCollection<E>() {
    private val store: MutableMap<E, OrderBitField>
    init {
        if (elements.isEmpty()) {
            store = mutableMapOf()
        } else {
            val codes = OrderBitField.initial(elements.size.toUInt()).toList()
            store = (elements zip codes).toMap().toMutableMap()
        }
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

    // ReorderableContainer methods

    override fun elements(): Iterable<E> {
        return store.keys
    }

    override fun sortKey(): (E) -> OrderBitField {
        return { store[it]!! }
    }

    override fun putBetween(start: E, end: E, vararg newElements: E) {
        require(start != end) { "start and end must be different" }
        require(start in store) { "start must be in the container" }
        require(end in store) { "end must be in the container" }
        val codes = OrderBitField.between(store[start]!!, store[end]!!, newElements.size.toUInt()).toList()
        val newMap = (newElements zip codes).toMap()
        store.putAll(newMap)
    }

    override fun putAtEnd(vararg newElements: E, last: Boolean) {
        val codes: Sequence<OrderBitField>
        if (store.isEmpty()) {
            codes = OrderBitField.initial(newElements.size.toUInt())
        } else if (last) {
            codes = OrderBitField.after(store.values.max(), newElements.size.toUInt())
        } else {
            codes = OrderBitField.before(store.values.min(), newElements.size.toUInt())
        }
        val newMap = (newElements zip codes.toList()).toMap()
        store.putAll(newMap)
    }

    override fun putNextTo(anchor: E, vararg newElements: E, after: Boolean) {
        require(anchor in store) { "anchor must be in the container" }
        val anchorCode = store[anchor]!!
        val codes: Sequence<OrderBitField>
        if (after) {
            val end = store.values.filter { it > anchorCode }.minOrNull()
            if (end == null) {
                codes = OrderBitField.after(anchorCode, newElements.size.toUInt())
            } else {
                codes = OrderBitField.between(anchorCode, end, newElements.size.toUInt())
            }
        } else {
            val start = store.values.filter { it < anchorCode }.maxOrNull()
            if (start == null) {
                codes = OrderBitField.before(anchorCode, newElements.size.toUInt())
            } else {
                codes = OrderBitField.between(start, anchorCode, newElements.size.toUInt())
            }
        }
        val newMap = (newElements zip codes.toList()).toMap()
        store.putAll(newMap)
    }

    override fun recompute() {
        val codes = OrderBitField.initial(store.size.toUInt())
        val newMap = (this zip codes.toList()).toMap() // not store.keys, which is unordered
        store.clear()
        store.putAll(newMap)
    }

    override fun popItem(last: Boolean): E {
        require(store.isNotEmpty()) { "the container is empty" }
        val element: E
        if (last) {
            element = store.entries.maxBy { it.value }.key
        } else {
            element = store.entries.minBy { it.value }.key
        }
        store.remove(element)
        return element
    }

    override fun popItems(n: Int, last: Boolean): Iterable<E> {
        val toPop = n.coerceAtMost(store.size)
        val rang = if (last) (size downTo size-toPop-1) else (0..<toPop)
        val thislist = this.toList() // sorted
        val rv = rang.map { thislist[it] }
        rv.forEach { store.remove(it) }
        return rv
    }

    override fun remove(vararg elements: E) {
        for (element in elements) {
            store.remove(element) ?: throw IllegalArgumentException("element not in the container")
        }
    }

    override fun discard(vararg elements: E) {
        for (element in elements) {
            store.remove(element)
        }
    }
}
