package fr.gouvernathor.orderbitfield

/**
 * Represents the ordering index of a value with respect to other similarly indexed values.
 */
public interface OrderValue<in O: OrderValue<O>>: Comparable<O>

/**
 * The backing type is (probably) a sequence, with a length,
 * and such that it can be padded with zero-like elements to a given length.
 */
public interface OrderField<in O: OrderField<O>>: OrderValue<O> {
    fun rPad(toSize: UInt): OrderField<O>
}

/**
 * There is a maximum size for the backing sequence.
 * This enables some additional operations, like field concatenation.
 */
public interface BoundedOrderField<in O: OrderField<O>, in BO: BoundedOrderField<O, BO>>: OrderField<O> {
    val maxSize: UInt

    override fun rPad(toSize: UInt): BoundedOrderField<O, BO>
    fun rPad(): BoundedOrderField<O, BO> = rPad(maxSize)
    /**
     * In this case, the max size of the result is the sum of the max sizes of the two operands.
     */
    operator fun plus(other: BO): BoundedOrderField<O, BO>
    operator fun plus(other: O): OrderField<O>
}


interface OrderValueFactory<O: OrderValue<O>> {
    /**
     * Yields n instances of O.
     * Should return the shortest possible values (with types T where that makes sense),
     * and then as evenly distributed as possible.
     */
    fun initial(n: UInt = 1u): Sequence<O>
    /**
     * Yields n instances of O that are between the two given O instances.
     * Should return the shortest possible values (with types T where that makes sense),
     * and then as evenly spaced between the two boundaries as possible.
     */
    fun between(start: O, end: O, n: UInt = 1u): Sequence<O>
    fun before(other: O, n: UInt = 1u): Sequence<O>
    fun after(other: O, n: UInt = 1u): Sequence<O>
}
