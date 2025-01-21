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

/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you don't intend to manipulate OrderBitField indexes,
 * and if the elements don't contain them in their structure.
 */
public fun <E> reorderableSetOf(vararg elements: E): ReorderableSet<E> {
    return MapBasedReorderableSet(*elements)
}
/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you intend to manipulate OrderBitField indexes,
 * for instance if you need to save them or store them in a database,
 * or if the elements contain their own OrderBitField indexes as part of their structure.
 *
 * In that latter case, assuming the property is called "idx",
 * you can use `reorderableSetOf({ it.idx }, { e, c -> e.idx = c }, ...elements)`.
 *
 * In any case, only the container should ever be tampering with the OrderBitField indexes.
 *
 * Before passing them to this construction function, if you need to initialize the OrderBitField property,
 * you can set it to null or to the empty list ; the getter function will still be safe to call
 * because the constructor and any method will call the setter (with a non-null value) on any incoming element before ever calling the getter.
 */
public fun <E> reorderableSetOf(getCode: (E) -> OrderBitField, setCode: (E, OrderBitField) -> Unit, vararg elements: E): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, *elements)
}

// TODO provide extension constructors (toReorderableSet) for Iterable, Sequence and Array
// which requires changing the constructors of the classes to take an Iterable instead of vararg

// TODO provide a way to manually provide the OrderBitField indexes without them being recomputed ?

internal abstract class AbstractReorderableSet<E>: ReorderableSet<E>, AbstractSet<E>() {
    abstract protected fun update(pairs: Iterable<Pair<E, OrderBitField>>, mayBeNew: Boolean = true): Unit

    override abstract val sortKey: (E) -> OrderBitField

    operator override fun iterator(): Iterator<E> {
        return elements().asSequence().sortedBy(sortKey).iterator()
    }

    override fun putBetween(start: E, end: E, vararg newElements: E) {
        require(start != end) { "start and end must be different" }
        val codes = OrderBitField.between(sortKey(start), sortKey(end), newElements.size.toUInt())
        update((newElements zip codes.toList()))
    }

    override fun putAtEnd(vararg newElements: E, last: Boolean) {
        // codes that will not be changed by this operation
        val unmovedCodes = elements().filter { it !in newElements }.map(sortKey)

        val codes: Sequence<OrderBitField>
        val nCodes = newElements.size.toUInt()
        if (unmovedCodes.isEmpty()) {
            codes = OrderBitField.initial(nCodes)
        } else if (last) {
            codes = OrderBitField.after(unmovedCodes.max(), nCodes)
        } else {
            codes = OrderBitField.before(unmovedCodes.min(), nCodes)
        }
        update((newElements zip codes.toList()))
    }

    override fun putNextTo(anchor: E, vararg newElements: E, after: Boolean) {
        require(anchor in this) { "anchor must be in the container" }

        // codes that will not be changed by this operation
        val unmovedCodes = elements().filter { it !in newElements }.map(sortKey)

        // reference code, from which neighbors are
        val anchorTrueCode = sortKey(anchor)

        // the two halves of the unmoved codes (lazy because the two won't necessarily be used)
        val unmovedCodesBefore = unmovedCodes.asSequence().filter { it < anchorTrueCode }
        val unmovedCodesAfter = unmovedCodes.asSequence().filter { anchorTrueCode < it }

        // one of the codes, that will be used as the boundary on the side of the anchor
        // in case the anchor is a part of the new elements
        val anchorCode: OrderBitField?
        if (anchor in newElements) {
            if (after) {
                anchorCode = unmovedCodesBefore.maxOrNull()
            } else {
                anchorCode = unmovedCodesAfter.minOrNull()
            }
        } else {
            anchorCode = anchorTrueCode
        }
        // the actual boundary codes
        val start: OrderBitField?
        val end: OrderBitField?
        if (after) {
            start = anchorCode
            end = unmovedCodesAfter.minOrNull()
        } else {
            start = unmovedCodesBefore.maxOrNull()
            end = anchorCode
        }
        // the new codes
        val codes: Sequence<OrderBitField>
        val nCodes = newElements.size.toUInt()
        if (start == null) {
            if (end == null) {
                codes = OrderBitField.initial(nCodes)
            } else {
                codes = OrderBitField.before(end, nCodes)
            }
        } else if (end == null) {
            codes = OrderBitField.after(start, nCodes)
        } else {
            codes = OrderBitField.between(start, end, nCodes)
        }
        update((newElements zip codes.toList()))
    }

    /**
     * Recompute the indices of these elements in this order.
     * Not public because passing non-consecutive elements to this function
     * yields unspecified ordering with the intermingled elements.
     */
    private fun partialRecompute(elements: Collection<E>) {
        val codes = OrderBitField.initial(elements.size.toUInt())
        update((elements zip codes.toList()), false)
    }
    override fun recompute() {
        partialRecompute(this) // not elements(), which is unordered
    }
    override fun <R : Comparable<R>> sortBy(selector: (E) -> R) {
        partialRecompute(this.sortedBy(selector)) // not elements() so that the sort is stable
    }
    override fun sortWith(comparator: Comparator<in E>) {
        partialRecompute(this.sortedWith(comparator)) // same
    }
    private fun getTranche(start: E?, end: E?): Iterable<E> {
        val startCode = if (start == null) null else sortKey(start)
        val endCode = if (end == null) null else sortKey(end)
        return this.filter { // not elements() so that the later sort is stable
            val code = sortKey(it)
            (startCode == null || startCode < code) && (endCode == null || code < endCode)
        }
    }
    override fun <R : Comparable<R>> sortTrancheBy(start: E?, end: E?, selector: (E) -> R) {
        val tranche = getTranche(start, end).sortedBy(selector)
        partialRecompute(tranche)
    }
    override fun sortTrancheWith(start: E?, end: E?, comparator: Comparator<in E>) {
        val tranche = getTranche(start, end).sortedWith(comparator)
        partialRecompute(tranche)
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

internal class MapBasedReorderableSet<E>(
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

internal class SetLambdaBasedReorderableSet<E>(
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
