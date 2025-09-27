package fr.gouvernathor.orderbitfield

private object abstractOrderStringFieldFactory {
    fun <O: OrderStringField> initial(n: UInt, construct: (String) -> O): Sequence<O> =
        generateCodes(n, "", null, "").map(construct)

    fun <O: OrderStringField> between(start: O, end: O, n: UInt, construct: (String) -> O): Sequence<O> = sequence {
        require(start < end) { "start must be less than end" }
        val prefix = commonPrefix(start.code, end.code)
        yieldAll(generateCodes(n, start.code.drop(prefix.length), end.code.drop(prefix.length), prefix).map(construct))
    }

    fun <O: OrderStringField> before(other: O, n: UInt, construct: (String) -> O): Sequence<O> =
        generateCodes(n, "", other.code, "").map(construct)

    fun <O: OrderStringField> after(other: O, n: UInt, construct: (String) -> O): Sequence<O> =
        generateCodes(n, other.code, null, "").map(construct)

    fun <O: OrderStringField> generate(start: String?, end: String?, n: UInt, construct: (String) -> O): Sequence<O> = sequence {
        val prefix: String
        if (start != null && end != null) {
            prefix = commonPrefix(start, end)
        } else {
            prefix = ""
        }
        val s = start ?: ""
        val e = if (end?.length ?: 0 > 0) end else null
        yieldAll(generateCodes(n, s, e, prefix).map(construct))
    }

    fun <O: OrderStringField> generate(start: O?, end: O?, n: UInt, construct: (String) -> O): Sequence<O> =
        generate(start?.code, end?.code, n, construct)
}

public open class OrderStringField internal constructor(internal val code: String): OrderField<OrderStringField> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
        require(code.all { it in LEGAL_CHARS }) { "code must only contain lowercase letters (internal error)" }
    }

    /**
     * Constructs both OrderStringField and BoundedOrderStringField instances.
     */
    companion object: OrderValueFactory<OrderStringField> {
        private fun getConstruct(maxSize: UInt?): (String) -> OrderStringField =
            if (maxSize != null)
                ({ BoundedOrderStringField(it, maxSize) })
            else
                ::OrderStringField
        fun initial(n: UInt, maxSize: UInt? = null): Sequence<OrderStringField> =
            abstractOrderStringFieldFactory.initial(n, getConstruct(maxSize))
        fun between(start: OrderStringField, end: OrderStringField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderStringField> =
            abstractOrderStringFieldFactory.between(start, end, n, getConstruct(maxSize))
        fun before(other: OrderStringField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderStringField> =
            abstractOrderStringFieldFactory.before(other, n, getConstruct(maxSize))
        fun after(other: OrderStringField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderStringField> =
            abstractOrderStringFieldFactory.after(other, n, getConstruct(maxSize))
        /**
         * Multi-purpose version of the 4 functions above,
         * using nullable boundaries,
         * accepts String instead of only OrderStringField,
         * doesn't check that the boundaries are correctly ordered.
         */
        fun generate(start: String?, end: String?, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderStringField> =
            abstractOrderStringFieldFactory.generate(start, end, n, getConstruct(maxSize))
        fun generate(start: OrderStringField?, end: OrderStringField?, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderStringField> =
            generate(start?.code, end?.code, n, maxSize)

        override fun initial(n: UInt) =
            abstractOrderStringFieldFactory.initial(n, ::OrderStringField)
        override fun between(start: OrderStringField, end: OrderStringField, n: UInt) =
            abstractOrderStringFieldFactory.between(start, end, n, ::OrderStringField)
        override fun before(other: OrderStringField, n: UInt) =
            abstractOrderStringFieldFactory.before(other, n, ::OrderStringField)
        override fun after(other: OrderStringField, n: UInt) =
            abstractOrderStringFieldFactory.after(other, n, ::OrderStringField)
        override fun generate(start: OrderStringField?, end: OrderStringField?, n: UInt) =
            abstractOrderStringFieldFactory.generate(start, end, n, ::OrderStringField)
    }

    override fun compareTo(other: OrderStringField): Int {
        return String.CASE_INSENSITIVE_ORDER.compare(this.code, other.code)
    }

    override fun rPad(toSize: UInt): OrderStringField {
        val uSize = code.length.toUInt()
        if (toSize == uSize) return this
        require(toSize > uSize) { "toSize must be greater or equal to the current size" }
        return OrderStringField(code.padEnd(toSize.toInt(), LEGAL_CHARS.first))
    }
}

public class BoundedOrderStringField internal constructor(
    code: String,
    override val maxSize: UInt,
): OrderStringField(code), BoundedOrderField<OrderStringField, BoundedOrderStringField> {
    init {
        require(code.length.toUInt() <= maxSize) { "the length of the code must not exceed maxSize (internal error)" }
    }

    public class BoundedOrderStringFieldFactory(private val maxSize: UInt): OrderValueFactory<BoundedOrderStringField> {
        override fun initial(n: UInt) =
            abstractOrderStringFieldFactory.initial(n, { BoundedOrderStringField(it, maxSize) })
        override fun between(start: BoundedOrderStringField, end: BoundedOrderStringField, n: UInt) =
            abstractOrderStringFieldFactory.between(start, end, n, { BoundedOrderStringField(it, maxSize) })
        override fun before(other: BoundedOrderStringField, n: UInt) =
            abstractOrderStringFieldFactory.before(other, n, { BoundedOrderStringField(it, maxSize) })
        override fun after(other: BoundedOrderStringField, n: UInt) =
            abstractOrderStringFieldFactory.after(other, n, { BoundedOrderStringField(it, maxSize) })
        override fun generate(start: BoundedOrderStringField?, end: BoundedOrderStringField?, n: UInt) =
            abstractOrderStringFieldFactory.generate(start?.code, end?.code, n, { BoundedOrderStringField(it, maxSize) })
    }

    override fun rPad(toSize: UInt): BoundedOrderStringField {
        require(toSize <= maxSize) { "toSize must not exceed maxSize" }
        val uSize = code.length.toUInt()
        if (toSize == uSize) return this
        require(toSize > uSize) { "toSize must be greater or equal to the current size" }
        return BoundedOrderStringField(code.padEnd(toSize.toInt(), LEGAL_CHARS.first), maxSize)
    }
    override fun rPad(): BoundedOrderStringField = rPad(maxSize)

    override operator fun plus(other: BoundedOrderStringField): BoundedOrderStringField {
        val code = rPad().code + other.code
        val newMaxSize = maxSize + other.maxSize
        return BoundedOrderStringField(code, newMaxSize)
    }
    override operator fun plus(other: OrderStringField): OrderStringField {
        val code = rPad().code + other.code
        return OrderStringField(code)
    }
}
