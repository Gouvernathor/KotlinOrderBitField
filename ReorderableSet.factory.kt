package fr.gouvernathor.orderbitfield

/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you don't intend to manipulate OrderBitField indexes,
 * and if the elements of the set don't contain the indexes in their structure.
 */
public fun <E> reorderableSetOf(vararg elements: E): ReorderableSet<E> =
    reorderableSetOf(OrderBitField, *elements)
/**
 * Create a new ReorderableSet with the given elements,
 * allowing you to specify how the OrderValue indexes are created.
 */
public fun <E, O: OrderValue<O>> reorderableSetOf(
    factory: OrderValueFactory<O>,
    vararg elements: E,
): ReorderableSet<E> =
    elements.toList().toReorderableSet(factory)

/**
 * Create a new ReorderableSet with the given elements.
 * Use this if you intend to manipulate OrderBitField indexes,
 * for instance if you need to save them or store them in a database,
 * or if the elements contain their own OrderBitField indexes as part of their structure.
 *
 * In that latter case, assuming the property is called "idx",
 * you can use `reorderableSetOf({ it.idx }, { e, c -> e.idx = c }, *elements)`.
 *
 * In any case, only the container should ever be tampering with the OrderBitField indexes.
 *
 * Before passing them to this construction function, if you need to initialize the OrderBitField property,
 * you can set it to null or to the empty list ; the getter function will still be safe to call
 * because the constructor and any method will call the setter (with a non-null value) on any incoming element before ever calling the getter.
 */
public fun <E> reorderableSetOf( // TODO this should probably use the String implem by default
    getCode: (E) -> OrderBitField,
    setCode: (E, OrderBitField) -> Unit,
    vararg elements: E,
): ReorderableSet<E> =
    reorderableSetOf(getCode, setCode, OrderBitField, *elements)
public fun <E, O: OrderValue<O>> reorderableSetOf(
    getCode: (E) -> O,
    setCode: (E, O) -> Unit,
    factory: OrderValueFactory<O>,
    vararg elements: E,
): ReorderableSet<E> =
    elements.toList().toReorderableSet(getCode, setCode, factory)

// avoid converting to List if already a Collection
public fun <E> Collection<E>.toReorderableSet(): ReorderableSet<E> =
    this.toReorderableSet(OrderBitField)
public fun <E, O: OrderValue<O>> Collection<E>.toReorderableSet(
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    MapBasedReorderableSet(this, factory)
public fun <E> Iterable<E>.toReorderableSet(): ReorderableSet<E> =
    this.toReorderableSet(OrderBitField)
public fun <E, O: OrderValue<O>> Iterable<E>.toReorderableSet(
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    this.toList().toReorderableSet(factory)
public fun <E> Sequence<E>.toReorderableSet(): ReorderableSet<E> =
    this.toReorderableSet(OrderBitField)
public fun <E, O: OrderValue<O>> Sequence<E>.toReorderableSet(
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    this.toList().toReorderableSet(factory)
public fun <E> Array<E>.toReorderableSet(): ReorderableSet<E> =
    this.toReorderableSet(OrderBitField)
public fun <E, O: OrderValue<O>> Array<E>.toReorderableSet(
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    this.toList().toReorderableSet(factory)

public fun <E> Iterable<E>.toReorderableSet(
    getCode: (E) -> OrderBitField,
    setCode: (E, OrderBitField) -> Unit,
): ReorderableSet<E> =
    this.toReorderableSet(getCode, setCode, OrderBitField)
public fun <E, O: OrderValue<O>> Iterable<E>.toReorderableSet(
    getCode: (E) -> O,
    setCode: (E, O) -> Unit,
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    SetLambdaBasedReorderableSet(getCode, setCode, factory, this)
public fun <E> Sequence<E>.toReorderableSet(
    getCode: (E) -> OrderBitField,
    setCode: (E, OrderBitField) -> Unit,
): ReorderableSet<E> =
    this.toReorderableSet(getCode, setCode, OrderBitField)
public fun <E, O: OrderValue<O>> Sequence<E>.toReorderableSet(
    getCode: (E) -> O,
    setCode: (E, O) -> Unit,
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    this.toList().toReorderableSet(getCode, setCode, factory)
public fun <E> Array<E>.toReorderableSet(
    getCode: (E) -> OrderBitField,
    setCode: (E, OrderBitField) -> Unit,
): ReorderableSet<E> =
    this.toReorderableSet(getCode, setCode, OrderBitField)
public fun <E, O: OrderValue<O>> Array<E>.toReorderableSet(
    getCode: (E) -> O,
    setCode: (E, O) -> Unit,
    factory: OrderValueFactory<O>,
): ReorderableSet<E> =
    this.toList().toReorderableSet(getCode, setCode, factory)

// TODO provide a way to manually provide the OrderBitField indexes without them being recomputed ?
