package fr.gouvernathor.orderbitfield

private val EMPTY_CODE: Code = emptyList()

public open class OrderBitField internal constructor(internal val code: Code): OrderField<OrderBitField> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
    }

    /**
     * Constructs both OrderBitField and BoundedOrderBitField instances.
     */
    companion object: OrderValueFactory<OrderBitField> {
        private fun construct(code: Code, maxSize: UInt?): OrderBitField {
            return if (maxSize != null) {
                BoundedOrderBitField(code, maxSize)
            } else {
                OrderBitField(code)
            }
        }

        fun initial(n: UInt, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, EMPTY_CODE, null, EMPTY_CODE).map { construct(it, maxSize) })
        }
        override fun initial(n: UInt) = initial(n, null)

        fun between(start: OrderBitField, end: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            require(start < end) { "start must be less than end" }
            val prefix = commonPrefix(start.code, end.code)
            yieldAll(generateCodes(n, start.code.drop(prefix.size), end.code.drop(prefix.size), prefix).map { construct(it, maxSize) })
        }
        override fun between(start: OrderBitField, end: OrderBitField, n: UInt) = between(start, end, n, null)

        fun before(other: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, EMPTY_CODE, other.code, EMPTY_CODE).map { construct(it, maxSize) })
        }
        override fun before(other: OrderBitField, n: UInt) = before(other, n, null)

        fun after(other: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, other.code, null, EMPTY_CODE).map { construct(it, maxSize) })
        }
        override fun after(other: OrderBitField, n: UInt) = after(other, n, null)

        /**
         * Multi-purpose version of the 4 functions above,
         * pass null to remove a boundary,
         * accepts Code instead of only OrderBitField,
         * doesn't check that the boundaries are correctly ordered.
         */
        fun generate(start: Code?, end: Code?, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            val prefix: Code
            if (start != null && end != null) {
                prefix = commonPrefix(start, end)
            } else {
                prefix = EMPTY_CODE
            }
            val s = start ?: EMPTY_CODE
            val e: Code?
            if (end?.size ?: 0 > 0) {
                e = end
            } else {
                e = null
            }
            yieldAll(generateCodes(n, s, e, prefix).map { construct(it, maxSize) })
        }
    }

    override fun compareTo(other: OrderBitField): Int {
        val n = code.size.coerceAtMost(other.code.size)
        for (i in 0..<n) {
            val diff = code[i].compareTo(other.code[i])
            if (diff != 0) return diff
        }
        return code.size.compareTo(other.code.size)
    }

    override fun rPad(toSize: UInt): OrderBitField {
        val uSize = code.size.toUInt()
        if (toSize == uSize) return this
        require(toSize > uSize) { "toSize must be greater or equal to the current size" }
        return OrderBitField(code + (List((toSize - uSize).toInt()) { 0u.toUByte() }))
    }
}

public class BoundedOrderBitField internal constructor(code: Code, override val maxSize: UInt): OrderBitField(code), BoundedOrderField<OrderBitField, BoundedOrderBitField> {
    init {
        require(code.size.toUInt() <= maxSize) { "the size of the code must not exceed maxSize (internal error)" }
    }

    override fun rPad(toSize: UInt): BoundedOrderBitField {
        require(toSize <= maxSize) { "toSize must not exceed maxSize" }
        val uSize = code.size.toUInt()
        if (toSize == uSize) return this
        require(toSize > uSize) { "toSize must be greater or equal to the current size" }
        return BoundedOrderBitField(code + (List((toSize - uSize).toInt()) { 0u.toUByte() }), maxSize)
    }
    override fun rPad(): BoundedOrderBitField = rPad(maxSize)

    override operator fun plus(other: BoundedOrderBitField): BoundedOrderBitField {
        val code = rPad().code + other.code
        val newMaxSize = maxSize + other.maxSize
        return BoundedOrderBitField(code, newMaxSize)
    }
    override operator fun plus(other: OrderBitField): OrderBitField {
        val code = rPad().code + other.code
        return OrderBitField(code)
    }
}
