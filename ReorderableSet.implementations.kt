package fr.gouvernathor.orderbitfield

internal fun <E> MapBasedReorderableSet(elements: Collection<E>): ReorderableSet<E> {
    if (elements.isEmpty()) {
        return MapBasedReorderableSet(mutableMapOf(), OrderBitField)
    } else {
        val codes = OrderBitField.initial(elements.size.toUInt()).toList()
        return MapBasedReorderableSet((elements zip codes).toMap().toMutableMap(), OrderBitField)
    }
}

private class MapBasedReorderableSet<E, O: OrderValue<O>>(
    private val store: MutableMap<E, O>,
    orderValueFactory: OrderValueFactory<O>,
): AbstractReorderableSet<E, O>(orderValueFactory) {

    // AbstractReorderableSet method

    override fun update(pairs: Iterable<Pair<E, O>>, mayBeNew: Boolean) {
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

    override val sortKey: (E) -> O = { store[it]!! }

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
    getCode: (E) -> OrderBitField,
    setCode: (E, OrderBitField) -> Unit,
    elements: Iterable<E>,
): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, elements.toMutableSet(), OrderBitField)
}

private class SetLambdaBasedReorderableSet<E, O: OrderValue<O>>(
    private val getCode: (E) -> O,
    private val setCode: (E, O) -> Unit,
    private val store: MutableSet<E>,
    orderValueFactory: OrderValueFactory<O>,
): AbstractReorderableSet<E, O>(orderValueFactory) {

    // AbstractReorderableSet method

    override fun update(pairs: Iterable<Pair<E, O>>, mayBeNew: Boolean) {
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

    override val sortKey: (E) -> O = getCode

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
