package fr.gouvernathor.orderbitfield

public interface OrderValue<O: OrderValue<O, T>, T>: Comparable<O>

/**
 * The backing type is (probably) a sequence, with a length,
 * and such that it can be padded with zero-like elements to a given length.
 */
public interface OrderField<O: OrderField<O, T>, T>: OrderValue<O, T> {
    val bounded: Boolean

    fun rPad(padSize: UInt): OrderField<O, T>
}

/**
 * There is a maximum size for the backing sequence.
 * This enables some additional operations, like field concatenation.
 */
public interface BoundedOrderField<O: OrderField<O, T>, T>: OrderField<O, T> {
    /**
     * This must be set at the class level, not the instance level.
     * (the interface will be changed when I find a way to do it)
     */
    val maxSize: UInt
    override val bounded: Boolean
        get() = true

    override fun rPad(padSize: UInt): BoundedOrderField<O, T>
    fun rPad(): BoundedOrderField<O, T>
    /**
     * In this case, the max size of the result is the sum of the max sizes of the two operands.
     */
    operator fun plus(other: BoundedOrderField<O, T>): BoundedOrderField<O, T>
    operator fun plus(other: OrderField<O, T>): OrderField<O, T>
}
