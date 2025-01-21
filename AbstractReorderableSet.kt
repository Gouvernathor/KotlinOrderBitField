package fr.gouvernathor.orderbitfield

internal abstract class AbstractReorderableSet<E>: ReorderableSet<E>, AbstractSet<E>() {
    abstract protected fun update(
        pairs: Iterable<Pair<E, OrderBitField>>,
        mayBeNew: Boolean = true,
    ): Unit

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
        var tranche: Iterable<E> = this
        // more efficient than .filter when it's already sorted
        if (startCode != null) {
            tranche = tranche.dropWhile { startCode < sortKey(it) }
        }
        if (endCode != null) {
            tranche = tranche.takeWhile { sortKey(it) < endCode }
        }
        return tranche
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
