val EMPTY_CODE: Code = emptyList()

/**
 * Represents the ordering index of a value with respect to other similarly indexed values.
 */
public class OrderBitField protected constructor(code: Code): Code by code, Comparable<OrderBitField> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
    }

    val maxSize: UInt? = null

    companion object {
        /**
         * Constructor, yields OrderBitField instances.
         * Returns the shortest possible values,
         * and then as evenly distributed as possible.
         */
        fun initial(n: UInt = 1u): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, EMPTY_CODE, null, EMPTY_CODE).map { OrderBitField(it) })
        }

        /**
         * Constructor, yields OrderBitField instances that are between the two given OrderBitField instances.
         * Returns the shortest possible values,
         * and then as evenly spaced between the two boundaries as possible.
         */
        fun between(start: OrderBitField, end: OrderBitField, n: UInt = 1u): Sequence<OrderBitField> = sequence {
            require(start < end) { "start must be less than end" }
            val prefix = commonPrefix(start, end)
            yieldAll(generateCodes(n, start.drop(prefix.size), end.drop(prefix.size), prefix).map { OrderBitField(it) })
        }

        fun before(other: OrderBitField, n: UInt = 1u): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, EMPTY_CODE, other, EMPTY_CODE).map { OrderBitField(it) })
        }

        fun after(other: OrderBitField, n: UInt = 1u): Sequence<OrderBitField> = sequence {
            yieldAll(generateCodes(n, other, null, EMPTY_CODE).map { OrderBitField(it) })
        }
    }

    override fun compareTo(other: OrderBitField): Int {
        val n = size.coerceAtMost(other.size)
        for (i in 0..<n) {
            val diff = this[i].compareTo(other[i])
            if (diff != 0) return diff
        }
        return size.compareTo(other.size)
    }
}
