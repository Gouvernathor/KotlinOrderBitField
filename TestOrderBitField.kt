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

private operator fun Code.compareTo(other: Code): Int {
    val n = size.coerceAtMost(other.size)
    for (i in 0 until n) {
        val diff = this[i].compareTo(other[i])
        if (diff != 0) return diff
    }
    return size.compareTo(other.size)
}
@ExperimentalUnsignedTypes
private operator fun Code.compareTo(other: UByteArray): Int {
    val n = size.coerceAtMost(other.size)
    for (i in 0 until n) {
        val diff = this[i].compareTo(other[i])
        if (diff != 0) return diff
    }
    return size.compareTo(other.size)
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
            // have the two codes ordered
            if (c < buf) {
                c1 = c
                c2 = buf.toList()
            } else {
                c1 = buf.toList()
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
}
