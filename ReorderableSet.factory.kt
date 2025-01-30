package fr.gouvernathor.orderbitfield

/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you don't intend to manipulate Code indexes,
 * and if the elements don't contain them in their structure.
 */
public fun <E> reorderableSetOf(vararg elements: E): ReorderableSet<E> {
    return MapBasedReorderableSet(elements.toList())
}
/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you intend to manipulate Code indexes,
 * for instance if you need to save them or store them in a database,
 * or if the elements contain their own Code indexes as part of their structure.
 *
 * In that latter case, assuming the property is called "idx",
 * you can use `reorderableSetOf({ it.idx }, { e, c -> e.idx = c }, ...elements)`.
 *
 * In any case, only the container should ever be tampering with the Code indexes.
 *
 * Before passing them to this construction function, if you need to initialize the Code property,
 * you can set it to null or to the empty list ; the getter function will still be safe to call
 * because the constructor and any method will call the setter (with a non-null value) on any incoming element before ever calling the getter.
 */
public fun <E> reorderableSetOf(
    getCode: (E) -> Code,
    setCode: (E, Code) -> Unit,
    vararg elements: E,
): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, elements.toList())
}

// avoid converting to List if already a Collection
public fun <E> Collection<E>.toReorderableSet(): ReorderableSet<E> {
    return MapBasedReorderableSet(this)
}
public fun <E> Iterable<E>.toReorderableSet(): ReorderableSet<E> {
    return MapBasedReorderableSet(this.toList())
}
public fun <E> Sequence<E>.toReorderableSet(): ReorderableSet<E> {
    return MapBasedReorderableSet(this.toList())
}
public fun <E> Array<E>.toReorderableSet(): ReorderableSet<E> {
    return MapBasedReorderableSet(this.toList())
}

public fun <E> Iterable<E>.toReorderableSet(
    getCode: (E) -> Code,
    setCode: (E, Code) -> Unit,
): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, this)
}
public fun <E> Sequence<E>.toReorderableSet(
    getCode: (E) -> Code,
    setCode: (E, Code) -> Unit,
): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, this.toList())
}
public fun <E> Array<E>.toReorderableSet(
    getCode: (E) -> Code,
    setCode: (E, Code) -> Unit,
): ReorderableSet<E> {
    return SetLambdaBasedReorderableSet(getCode, setCode, this.toList())
}

// TODO provide a way to manually provide the Code indexes without them being recomputed ?
