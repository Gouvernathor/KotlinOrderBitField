package fr.gouvernathor.orderbitfield

internal val EMPTY_CODE: Code = emptyList()

/**
 * Represents the ordering index of a value with respect to other similarly indexed values.
 */
public class OrderBitField protected constructor(code: Code, val maxSize: UInt?): Code by code, Comparable<OrderBitField> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
        require(maxSize == null || (code.size.toUInt() <= maxSize)) { "code is larger than maxSize" }
    }

    val bounded: Boolean
        get() = maxSize != null

    companion object {
        /**
         * Constructor, yields OrderBitField instances.
         * Returns the shortest possible values,
         * and then as evenly distributed as possible.
         */
        fun initial(n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, EMPTY_CODE, null, EMPTY_CODE).map { OrderBitField(it, maxSize) })
        }

        /**
         * Constructor, yields OrderBitField instances that are between the two given OrderBitField instances.
         * Returns the shortest possible values,
         * and then as evenly spaced between the two boundaries as possible.
         */
        fun between(start: OrderBitField, end: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            require(start < end) { "start must be less than end" }
            val prefix = commonPrefix(start, end)
            yieldAll(generateCodes(n, start.drop(prefix.size), end.drop(prefix.size), prefix).map { OrderBitField(it, maxSize) })
        }

        fun before(other: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, EMPTY_CODE, other, EMPTY_CODE).map { OrderBitField(it, maxSize) })
        }

        fun after(other: OrderBitField, n: UInt = 1u, maxSize: UInt? = null): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, other, null, EMPTY_CODE).map { OrderBitField(it, maxSize) })
        }

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
            yieldAll(generateCodes(n, s, e, prefix).map { OrderBitField(it, maxSize) })
        }
    }

    override operator fun compareTo(other: OrderBitField): Int {
        val n = size.coerceAtMost(other.size)
        for (i in 0..<n) {
            val diff = this[i].compareTo(other[i])
            if (diff != 0) return diff
        }
        return size.compareTo(other.size)
    }

    /**
     * Returns an OrderBitField instance whose size is exactly the given size (or the native maxsize if not provided).
     * Primarily used as part as code concatenation,
     * also useful when matching a BINARY(x) (rather than VARBINARY) column in a database.
     */
    fun rPad(padSize: UInt? = maxSize): OrderBitField {
        require(padSize != null) { "the pad size must be specified when the OrderBitField is unbounded" }
        val uSize = size.toUInt()
        if (padSize == uSize) return this
        require(padSize <= uSize) { "the pad size must be lesser or equal to the size" }
        return OrderBitField(this as Code + (List((padSize - uSize).toInt()) { 0u.toUByte() }), maxSize)
    }

    /**
     * Addition is only supported when the left operand is bounded (when it has a maxSize).
     *
     * To support this uniformly in systems where orderBitFields always have the same maxSize,
     * (for instance when they match a VARBINARY(x) column in a database),
     * you can simply provide a replacement for the Companion object whose methods take no maxSize parameter,
     * and pass the chosen maxSize to the actual Companion collective constructor functions.
     * If/when it matches a BINARY(x) column, you can make the proxy Companion object's methods
     * map their return values using OrderBitField::rPad.
     */
    operator fun plus(other: OrderBitField): OrderBitField {
        require(bounded) { "the left operand must be bounded for addition to work" }
        val code = this.rPad() + other as Code
        val newMaxSize = other.maxSize ?.plus(maxSize!!)
        return OrderBitField(code, newMaxSize)
    }
}
