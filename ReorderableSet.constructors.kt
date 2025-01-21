package fr.gouvernathor.orderbitfield

/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you don't intend to manipulate OrderBitField indexes,
 * and if the elements don't contain them in their structure.
 */
public fun <E> reorderableSetOf(vararg elements: E): ReorderableSet<E> {
    return MapBasedReorderableSet(elements.toList())
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
    return SetLambdaBasedReorderableSet(getCode, setCode, elements.toList())
}

// TODO provide extension constructors (toReorderableSet) for Iterable, Sequence and Array

// TODO provide a way to manually provide the OrderBitField indexes without them being recomputed ?
