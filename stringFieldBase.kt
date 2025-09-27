package fr.gouvernathor.orderbitfield

import kotlin.toUInt

internal val LEGAL_CHARS = 'a'..'z'
private val MIN_CHAR = LEGAL_CHARS.first
private val MAX_CHAR = LEGAL_CHARS.last
private val NCHARS = LEGAL_CHARS.count().toUInt()

/**
 * Requires s1 and s2 to be ordered, in that order.
 */
internal fun commonPrefix(s1: String, s2: String): String {
    for ((i, c) in s1.withIndex()) {
        if (c != s2[i]) {
            return s1.drop(i)
        }
    }
    return s1
}

/**
 * The codes are generated in order.
 * codeStart may be empty but not null,
 * codeEnd may be null but not empty.
 */
internal fun generateCodes(
    nCodes: UInt,
    codeStart: String,
    codeEnd: String?,
    prefix: String,
): Sequence<String> = sequence {
    require(codeEnd == null || codeEnd.isNotEmpty()) { "codeEnd must be null or non-empty (internal error)" }

    if (nCodes == 0u) {
        return@sequence
    }

    val startChar1 = if (!codeStart.isEmpty()) codeStart.first() else MIN_CHAR
    val endChar1 = if (codeEnd != null) codeEnd.first() else MAX_CHAR

    // there is going to be direct codes (of the form prefix+x)
    // and longer codes (of the form prefix+x+y...)

    // range of possible direct chars : [[startChar1 + 1, endChar1]]
    val directCandidates = startChar1.inc()..endChar1
    val nDirectCandidates = directCandidates.count().toUInt()
    // the x direct chars that will be used for direct codes
    val direct: Iterable<Char>
    // the number of longer codes that will be generated for each x char
    val longer: Map<Char, UInt>

    if (nDirectCandidates >= nCodes) {
        // everything can go in direct codes

        if (nDirectCandidates == nCodes) {
            // no need to arrange
            direct = directCandidates
        } else {
            direct = simpleDistributeIndices(nCodes, startChar1.inc()..endChar1).toSet()
        }

        longer = emptyMap()
    } else {
        // there are too many codes to be generated for direct codes to be enough
        // we take all available direct codes
        direct = directCandidates

        // distributing longer codes among the chars by which they will begin
        // interval of those starting chars : [[startChar1, endChar1]]
        val longerPonderation = mutableMapOf<Char, UInt>().withDefault { NCHARS }
        // could have any Number values, but unnecessary (could also be floats for that matter)
        // ints are easier to handle in K with no precision loss

        // if we have a starting boundary and it has a second char,
        // the first char's ponderation is the distance between
        // that second char (exclusive) and one more than the max char (inclusive)
        if (codeStart.length >= 2) {
            longerPonderation[startChar1] = NCHARS - (codeStart[1] - MIN_CHAR).toUInt()
        }
        // otherwise that char has no particular ponderation
        // in any case, startChar1 is always valid as a start for longer codes

        val longerMaxBoundary: Char // inclusive boundary
        if (codeEnd != null) {
            // if there is an ending boundary
            if (codeEnd.length >= 2) {
                // if it has a second char,
                // the first char's ponderation is the distance between
                // the min char (inclusive) and that second char (exclusive)
                longerPonderation[endChar1] = (codeEnd[1] - MIN_CHAR).toUInt()

                longerMaxBoundary = endChar1
            } else {
                longerMaxBoundary = endChar1.dec()
            }
        } else {
            longerMaxBoundary = endChar1
        }

        longer = ponderatedDistributeIndices(
            nCodes - nDirectCandidates,
            startChar1..longerMaxBoundary,
            longerPonderation,
        )
    }

    // assert longer.values.sum() + direct.size.toUInt() == nCodes

    for (c in startChar1..endChar1) {
        val pre = prefix + c

        if (c in direct) {
            yield(pre)
        }

        val nRecurs = longer.getOrDefault(c, 0u)
        if (nRecurs > 0u) {
            yieldAll(generateCodes(
                nRecurs,
                if (codeStart.length > 0 && c == startChar1) codeStart.drop(1) else "",
                if (codeEnd != null && c == endChar1) codeEnd.drop(1) else null,
                pre))
        }
    }
}

/**
 * Spreads nCodes codes among the range chars inclusive,
 * with a ponderation for each index.
 */
private fun ponderatedDistributeIndices(
    nCodes: UInt,
    range: CharRange,
    ponderation: Map<Char, UInt>,
): Map<Char, UInt> {
    // assert MIN_CHAR <= range.first
    // assert range.first < range.last
    // assert range.last <= MAX_CHAR

    val nChars = range.count().toUInt()
    val attrib = mutableMapOf<Char, UInt>().withDefault { 0u }
    var remaining = nCodes

    if (nCodes > nChars) {
        val total = range.map { ponderation[it]!! } .sum()
        for (c in range) {
            val value = nCodes * ponderation[c]!! / total
            attrib[c] = value
            remaining -= value
        }
    }

    for (c in simpleDistributeIndices(remaining, range)) {
        attrib[c] = attrib[c]!! + 1u
    }

    return attrib
}

/**
 * Spreads nCodes codes among the range chars.
 * nCodes must be positive, and lower or equal to the number of chars in the range.
 *
 * If there are 2 codes to spread, they are attributed on a thirds basis.
 * Otherwise, one code is placed at the middle char rounded down,
 * and the others are spread recursively among the remaining chars.
 */
private fun simpleDistributeIndices(
    nCodes: UInt,
    range: CharRange,
): Sequence<Char> = sequence {
    if (nCodes <= 0u) {
        return@sequence
    }

    val nChars = range.count()

    // require(nCodes <= nChars)

    if (nCodes == 2u) {
        yield(range.elementAt((nChars-1)/3))
        yield(range.elementAt((2*nChars-1)/3))
        return@sequence
    }

    // midpoint
    val pivot = range.elementAt(nChars/2)

    if (nCodes == 1u) {
        yield(pivot)
    } else {
        /*
        number of codes to be put on the right
        (must be lower or equal to those on the left because of how the pivot is computed,
        which favors the fact that appending is more frequent than prepending,
        so if we must choose, best leave more room after than before)
        */
        val right = (nCodes - 1u) / 2u

        yieldAll(simpleDistributeIndices(nCodes-1u-right, range.first..pivot.dec()))
        yield(pivot)
        yieldAll(simpleDistributeIndices(right, pivot.inc()..range.last))
    }
}
