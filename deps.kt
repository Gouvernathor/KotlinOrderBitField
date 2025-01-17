val emptyMap = mapOf<Any, Any>()
val topValue = 256u
val maxByte = (topValue -1u).toUByte()
val magicMiddle = (topValue / 2u).toUByte()

public fun commonPrefix(s1: List<UByte>, s2: List<UByte>): List<UByte> {
    for ((i, c) in s1.withIndex()) {
        if (c != s2[i]) {
            return s1.drop(i)
        }
    }
    return s1
}

public fun generateCodes(
    nCodes: UInt,
    codeStart: List<UByte>,
    codeEnd: List<UByte>?,
    prefix: List<UByte>,
): Sequence<List<UByte>> {
    return sequence {
        // call yield() and/or yieldAll()
        if (nCodes == 0u) {
            // return Unit
        }
        yield(listOf())
    }
}
