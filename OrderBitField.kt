package fr.gouvernathor.orderbitfield

public open class OrderBitField internal constructor(internal val code: Code): OrderField<OrderBitField, Code> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
    }

    // companion object (later)

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

public class BoundedOrderBitField internal constructor(code: Code, override val maxSize: UInt): OrderBitField(code), BoundedOrderField<BoundedOrderBitField, OrderBitField, Code> {
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
