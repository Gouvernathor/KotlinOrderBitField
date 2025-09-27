package fr.gouvernathor.orderbitfield

private val EMPTY_CODE: Code = emptyList()

private object abstractOrderBitFieldFactory {
    fun <O: OrderBitField> initial(n: UInt, construct: (Code) -> O): Sequence<O> = sequence {
        yieldAll(generateCodes(n, EMPTY_CODE, null, EMPTY_CODE).map(construct))
    }

    fun <O: OrderBitField> between(start: O, end: O, n: UInt, construct: (Code) -> O): Sequence<O> = sequence {
        require(start < end) { "start must be less than end" } // TODO
        val prefix = commonPrefix(start.code, end.code)
        yieldAll(generateCodes(n, start.code.drop(prefix.size), end.code.drop(prefix.size), prefix).map(construct))
    }

    fun <O: OrderBitField> before(other: O, n: UInt, construct: (Code) -> O): Sequence<O> = sequence {
        yieldAll(generateCodes(n, EMPTY_CODE, other.code, EMPTY_CODE).map(construct))
    }

    fun <O: OrderBitField> after(other: O, n: UInt, construct: (Code) -> O): Sequence<O> = sequence {
        yieldAll(generateCodes(n, other.code, null, EMPTY_CODE).map(construct))
    }

    fun <O: OrderBitField> generate(start: Code?, end: Code?, n: UInt, construct: (Code) -> O): Sequence<O> = sequence {
        val prefix: Code
        if (start != null && end != null) {
            prefix = commonPrefix(start, end)
        } else {
            prefix = EMPTY_CODE
        }
        val s = start ?: EMPTY_CODE
        val e = if (end?.size ?: 0 > 0) end else null
        yieldAll(generateCodes(n, s, e, prefix).map(construct))
    }
}

public open class OrderBitField internal constructor(internal val code: Code): OrderField<OrderBitField> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
    }

    /**
     * Constructs both OrderBitField and BoundedOrderBitField instances.
     */
    companion object: OrderValueFactory<OrderBitField> {
        private fun getConstruct(maxSize: UInt?): (Code) -> OrderBitField =
            if (maxSize != null)
                ({ BoundedOrderBitField(it, maxSize) })
            else
                ::OrderBitField
        fun initial(n: UInt, maxSize: UInt? = null): Sequence<OrderBitField> =
            abstractOrderBitFieldFactory.initial(n, getConstruct(maxSize))
        fun between(start: OrderBitField, end: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> =
            abstractOrderBitFieldFactory.between(start, end, n, getConstruct(maxSize))
        fun before(other: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> =
            abstractOrderBitFieldFactory.before(other, n, getConstruct(maxSize))
        fun after(other: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> =
            abstractOrderBitFieldFactory.after(other, n, getConstruct(maxSize))
        /**
         * Multi-purpose version of the 4 functions above,
         * pass null to remove a boundary,
         * accepts Code instead of only OrderBitField,
         * doesn't check that the boundaries are correctly ordered.
         */
        fun generate(start: Code?, end: Code?, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> =
            abstractOrderBitFieldFactory.generate(start, end, n, getConstruct(maxSize))

        override fun initial(n: UInt) =
            abstractOrderBitFieldFactory.initial(n, ::OrderBitField)
        override fun between(start: OrderBitField, end: OrderBitField, n: UInt) =
            abstractOrderBitFieldFactory.between(start, end, n, ::OrderBitField)
        override fun before(other: OrderBitField, n: UInt) =
            abstractOrderBitFieldFactory.before(other, n, ::OrderBitField)
        override fun after(other: OrderBitField, n: UInt) =
            abstractOrderBitFieldFactory.after(other, n, ::OrderBitField)
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

    public class BoundedOrderBitFieldFactory(private val maxSize: UInt): OrderValueFactory<BoundedOrderBitField> {
        override fun initial(n: UInt) =
            abstractOrderBitFieldFactory.initial(n, { BoundedOrderBitField(it, maxSize) })
        override fun between(start: BoundedOrderBitField, end: BoundedOrderBitField, n: UInt) =
            abstractOrderBitFieldFactory.between(start, end, n, { BoundedOrderBitField(it, maxSize) })
        override fun before(other: BoundedOrderBitField, n: UInt) =
            abstractOrderBitFieldFactory.before(other, n, { BoundedOrderBitField(it, maxSize) })
        override fun after(other: BoundedOrderBitField, n: UInt) =
            abstractOrderBitFieldFactory.after(other, n, { BoundedOrderBitField(it, maxSize) })
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
