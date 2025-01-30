package fr.gouvernathor.orderbitfield

internal fun <E> MapBasedReorderableSet(elements: Collection<E>): ReorderableSet<E> {
    if (elements.isEmpty()) {
        return MapBasedReorderableSet(mutableMapOf())
    } else {
        val codes = OrderBitField.initial(elements.size.toUInt()).toList()
        return MapBasedReorderableSet((elements zip codes).toMap().toMutableMap())
    }
}

private class MapBasedReorderableSet<E>(
    private val store: MutableMap<E, Code>,
): AbstractReorderableSet<E>() {

    // AbstractReorderableSet method

    override fun update(pairs: Iterable<Pair<E, Code>>, mayBeNew: Boolean) {
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

    override val elements: Iterable<E>
        get() = store.keys

    override val sortKey: (E) -> Code = { store[it]!! }

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

internal fun <E> SetLambdaBasedReorderableSet(
    getCode: (E) -> Code,
    setCode: (E, Code) -> Unit,
    elements: Iterable<E>,
): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, elements.toMutableSet())
}

private class SetLambdaBasedReorderableSet<E>(
    private val getCode: (E) -> Code,
    private val setCode: (E, Code) -> Unit,
    private val store: MutableSet<E>,
): AbstractReorderableSet<E>() {

    // AbstractReorderableSet method

    override fun update(pairs: Iterable<Pair<E, Code>>, mayBeNew: Boolean) {
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

    override val elements: Iterable<E>
        get() = store

    override val sortKey: (E) -> Code = getCode

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
