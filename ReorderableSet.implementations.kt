package fr.gouvernathor.orderbitfield

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
