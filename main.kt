val MAX_BYTE = UByte.Companion.MAX_VALUE
val TOP_VALUE = MAX_BYTE + 1u
val MAGIC_MIDDLE = (TOP_VALUE / 2u).toUByte()

/**
 * Requires s1 and s2 to be ordered, in that order.
 */
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
): Sequence<List<UByte>> = sequence {
    // call yield() and/or yieldAll()
    if (nCodes == 0u) {
        return@sequence
    }

    val startDigit1 = if (!codeStart.isEmpty()) codeStart.first() else 0u
    val endDigit1 = if (!codeEnd.isNullOrEmpty()) codeEnd.first() else MAX_BYTE

    // there is going to be direct codes (of the form prefix+x)
    // and longer codes (of the form prefix+x+y...)

    // range of possible direct digits : [[startDigit1 + 1, endDigit1]]
    val nDirectCandidates = endDigit1 - startDigit1
    // the x direct digits that will be used for direct codes
    val direct: Collection<UByte> // Iterable ?
    // the number of longer codes that will be generated for each x digit
    val longer: Map<UByte, UInt>

    if (nDirectCandidates >= nCodes) {
        // everything can go in direct codes

        if (nDirectCandidates == nCodes) {
            // no need to arrange
            direct = ((startDigit1 + 1u).toUByte()..endDigit1).toSet() as Collection<UByte> // see which is correct
        } else {
            direct = simpleDistributeIndices(nCodes, (startDigit1 + 1u).toUByte(), endDigit1).toSet()
        }

        longer = emptyMap()
    } else {
        // there are too many codes to be generated for direct codes to be enough
        // we take all available direct codes
        direct = ((startDigit1 + 1u).toUByte()..endDigit1).map { it.toUByte() } // see which is correct

        // distributing longer codes among the digits by which they will begin
        // interval of those starting digits : [[startDigit1, endDigit1]]
        val longerPonderation = mutableMapOf<UByte, UInt>().withDefault { TOP_VALUE }
        // could afford to have UByte values, but unnecessary (could also be floats for that matter)
        // ints are easier to handle in K and incur no precision loss

        // if we have a starting boundary and it has a second digit,
        // the first digit's ponderation is the distance between
        // that second digit (excluded) and TOP_VALUE (included)
        if (codeStart.size > 1) {
            longerPonderation[startDigit1] = TOP_VALUE - codeStart[1]
        }
        // otherwise that digit has no particular ponderation
        // in any case, startDigit1 is always valid as a start for longer codes

        val longerMaxBoundary: UByte // inclusive boundary
        if (!codeEnd.isNullOrEmpty()) {
            // if there is an end boundary
            if (codeEnd.size > 1) {
                // if it has a second digit,
                // the first digit's ponderation is the distance between
                // 0 (included) and that second digit (excluded)
                longerPonderation[endDigit1] = codeEnd[1] + 0u

                longerMaxBoundary = endDigit1
            } else {
                longerMaxBoundary = (endDigit1 - 1u).toUByte()
            }
        } else {
            longerMaxBoundary = endDigit1
        }

        longer = ponderatedDistributeIndices(
            nCodes - nDirectCandidates,
            startDigit1, longerMaxBoundary,
            longerPonderation,
        )
    }

    // assert sum(longer.values()) + len(direct) == nCodes

    for (cInt in startDigit1..endDigit1) {
        val c = cInt.toUByte()
        val pre = prefix + c

        if (direct.contains(c)) {
            yield(pre)
        }

        val nRecurs = longer.getOrDefault(c, 0u)
        if (nRecurs > 0u) {
            yieldAll(generateCodes(
                nRecurs,
                if (codeStart.size > 0 && c == startDigit1) codeStart.drop(1) else emptyList(),
                if (!codeEnd.isNullOrEmpty() && c == endDigit1) codeEnd.drop(1) else null,
                pre))
        }
    }
}

/**
 * Spreads nCodes codes among the mn to mx digits inclusive,
 * with a ponderation for each index.
 */
fun ponderatedDistributeIndices(
    nCodes: UInt,
    mn: UByte, mx: UByte,
    ponderation: Map<UByte, UInt>,
): Map<UByte, UInt> {
    // assert 0u <= mn
    // assert mn < mx
    // assert mx < TOP_VALUE

    val nChars = mx - mn + 1u
    val attrib = mutableMapOf<UByte, UInt>().withDefault { 0u }
    var remaining = nCodes

    if (nCodes > nChars) {
        val total = (mn..mx).map({ponderation[it.toUByte()]!!}).sum()
        for (cInt in mn..mx) {
            val c = cInt.toUByte()
            val value = nCodes * ponderation[c]!! / total
            attrib[c] = value
            remaining -= value
        }
    }

    for (c in simpleDistributeIndices(remaining, mn, mx)) {
        attrib[c] = attrib[c]!! + 1u
    }

    return attrib
}
