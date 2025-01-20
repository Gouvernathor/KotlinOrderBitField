import kotlin.random.Random
import kotlin.random.nextUBytes
import kotlin.random.nextUInt
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.collections.listOf
import kotlin.collections.dropLast
import kotlin.math.ceil
import kotlin.math.log

private operator fun Code.compareTo(other: Code): Int {
    val n = size.coerceAtMost(other.size)
    for (i in 0 until n) {
        val diff = this[i].compareTo(other[i])
        if (diff != 0) return diff
    }
    return size.compareTo(other.size)
}

private infix fun Int.pow(exponent: UInt): Int {
    var rv = 1
    for (i in 0u until exponent) {
        rv *= this
    }
    return rv
}

val ZERO_UBYTE = 0u.toUByte()

class TestOrderBitField(randomKey: Int? = null) {
    val random = Random(randomKey ?: Random.Default.nextInt())

    @Test
    @ExperimentalUnsignedTypes
    fun testIntegrity() {
        val nCodesTest: Int = random.nextInt(3000, 4000)
        parameterizedExecution(nCodesTest, emptyList(), null)

        val codesize = 10

        // generate a random code
        var c = let {
            val buf = UByteArray(codesize)
            random.nextUBytes(buf)
            if (buf[codesize-1] == ZERO_UBYTE) {
                buf[codesize-1] = random.nextUInt(1u, 256u).toUByte()
            }
            buf.toList()
        }
        // test it as minimal and maximal boundary
        parameterizedExecution(nCodesTest, c, null)
        parameterizedExecution(nCodesTest, emptyList(), c)

        // generate a second random code, a bit longer
        val c1: Code
        val c2: Code
        let {
            val buf = UByteArray((codesize*1.2f).toInt())
            random.nextUBytes(buf)
            if (buf[codesize-1] == ZERO_UBYTE) {
                buf[codesize-1] = random.nextUInt(1u, 256u).toUByte()
            }
            val cn = buf.toList()
            // have the two codes ordered
            if (c < cn) {
                c1 = c
                c2 = cn
            } else {
                c1 = cn
                c2 = c
            }
        }
        // test with the two as boundaries
        parameterizedExecution(nCodesTest, c1, c2)

        val commonsize = random.nextInt(codesize/3, codesize*5/6)
        // the min boundary is a prefix of the max boundary
        parameterizedExecution(nCodesTest, c1.take(commonsize), c1)
        parameterizedExecution(nCodesTest, c1.dropLast(1), c1)
        // the boundaries have a common part
        parameterizedExecution(nCodesTest,
            c1.take(commonsize)+c1,
            c1.take(commonsize)+c2)
        parameterizedExecution(nCodesTest,
            c1.dropLast(1)+c1,
            c1.dropLast(1)+c2)
    }

    @Test
    fun testLimitValues() {
        OrderBitField.generate(null, null, 1u).toList()
        OrderBitField.generate(emptyList(), null, 1u).toList()
        OrderBitField.generate(null, emptyList(), 1u).toList()
        OrderBitField.generate(emptyList(), emptyList(), 1u).toList()
    }

    @Test
    fun testCompactness() {
        var codes: List<Code>

        // Test that for two consecutive UBytes :
        val digMin = random.nextInt(255).toUByte()
        val digMax = (digMin+1u).toUByte()
        // * for 1 requested code, it gets no longer than 2 digits long
        codes = parameterizedExecution(1, listOf(digMin), listOf(digMax))
        assertEquals(2, codes[0].size)
        // * for 255 requested codes, none gets longer than 2 digits long
        codes = parameterizedExecution(255, listOf(digMin), listOf(digMax))
        codes.forEach { assertEquals(2, it.size) }

        // Idem with more depth
        // adjust the code size to have a reasonable processing time
        val codesize = 3
        val nCodesTest = (256 pow codesize.toUInt()) - 1
        // around 16 millions
        codes = parameterizedExecution(nCodesTest, emptyList(), null)
        codes.forEach { assertTrue(it.size <= codesize) }
        codes = parameterizedExecution(nCodesTest, listOf(digMin), listOf(digMax))
        codes.forEach { assertTrue(it.size <= codesize+1) }
    }

    /**
     * Tests successively adding elements after one another.
     * The expected limit, without intermediary recomputes,
     * is of 7 or 8 times the max number of digits in a code.
     * Why 7 or 8 ? because 2**7 < 2**8 = 256 < 2**9
     * (with 256 possible values for a UByte)
     * so 7 or 8 is the number of insertions (here the number of loops)
     * after which the code takes a new digit, supposing that
     * each code gets at the midpoint of the remaining range.
     * floor(log2(256)) = 7?8?
     */
    @Test
    fun testMaxNumberOfSuccessiveCallsAndRecompute() {
        var lastCode: Code = emptyList()
        val codes = mutableSetOf(lastCode)
        val maxCodeSize = 5

        for (i in 0 until 7*maxCodeSize) {
            lastCode = parameterizedExecution(1, lastCode, null)[0]
            assertTrue(codes.add(lastCode))
        }

        // TODO test that when calling recompute,
        // the number of codes returned is the same,
        // and the returned codes are at maximum compactness
    }

    @Test
    fun testDoublesInRecompute() {
        val nCodes = 10
        val codesLarge = parameterizedExecution(nCodes, emptyList(), null)
        val codesDoubled = parameterizedExecution(nCodes, codesLarge[nCodes/2], codesLarge[nCodes/2+1])
        val codes = codesLarge + codesDoubled + codesDoubled

        assertEquals(3*nCodes, codes.size)
        assertEquals(2*nCodes, setOf(codes).size)

        // TODO make a new container whose elements' codes are those in codes
        // call recompute
        // test that the container's size is still 3*nCodes, and that its number of distinct codes is also 3*nCodes
        // TODO also decide whether that's the intended behavior or not
    }

    private fun parameterizedExecution(nCodesTest: Int, boundMin: Code, boundMax: Code?): List<Code> {
        // if this check doesn't pass, the caller test has an error
        assertTrue((boundMax == null) || boundMin < boundMax)

        val codes: List<Code> = OrderBitField.generate(boundMin, boundMax, nCodesTest).toList()

        // check the number of codes
        assertEquals(nCodesTest, codes.size)

        // check that all codes are different
        assertEquals(nCodesTest, codes.toSet().size)

        // check that the boundaries are respected
        for (code in codes) {
            assertTrue(boundMin < code)
            if (boundMax != null) {
                assertTrue(code < boundMax)
            }
        }

        // check that the codes are sorted
        // assertContentEquals(codes.sorted(), codes)

        // check that no code is empty or ends with 0
        assertTrue(codes.none { it.isEmpty() })
        assertTrue(codes.none { it.last() == ZERO_UBYTE })

        return codes
    }

    private fun testMaxCompactness(codes: Collection<Code>) {
        val maxCodeSize = codes
            .map { it.size }
            .maxOrNull() ?: 0
        assertTrue(maxCodeSize <= ceil(log(codes.size.toFloat(), 256f)))
    }
}
