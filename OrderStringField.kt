package fr.gouvernathor.orderbitfield

private object abstractOrderStringFieldFactory {
    // ...
}

public open class OrderStringField internal constructor(internal val code: String): OrderField<OrderStringField> {
    init {
        require(code.isNotEmpty()) { "code must not be empty (internal error)" }
        require(code.all { it in LEGAL_CHARS }) { "code must only contain lowercase letters (internal error)" }
    }

    // companion object

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

    // public class BoundedOrderStringFieldFactory

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
