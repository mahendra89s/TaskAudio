import java.io.FileInputStream
import java.util.*

object KotlinAFSKDecoder {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Read the WAV file into a byte array
        val fileInputStream = FileInputStream("//Users//webwerks//IdeaProjects//Audio Task//src//file_2.wav")
        val data = ByteArray(fileInputStream.available())
        fileInputStream.read(data)
        fileInputStream.close()

        // Decode the AFSK signal into a bit-stream
        val sampleRate = 44100
        val bitDuration = sampleRate / 1000 * 320
        val zeroDuration = sampleRate / 1000 * 640
        val leadToneDuration = sampleRate * 2.5f / 10
        val bits: MutableList<Int> = ArrayList()
        var i = 0
        while (i < leadToneDuration) {
            bits.add(1)
            i += bitDuration
        }
        for (j in 1 until data.size) {
            var zeroCount = 0
            var oneCount = 0
            var value = data[j].toInt() and 0xff
            while (value >= 128) {
                oneCount++
                value -= 128
            }
            while (value >= 64) {
                zeroCount++
                value -= 64
            }
            i += if (oneCount == 1 && zeroCount == 1) {
                // Found a bit
                if (i % bitDuration < zeroDuration) {
                    bits.add(0)
                } else {
                    bits.add(1)
                }
                bitDuration
            } else {
                // Not a bit
                (zeroCount + oneCount) * bitDuration
            }
        }
        while (i < data.size * 8) {
            bits.add(1)
            i += bitDuration
        }

        // Convert the bit-stream into a byte-stream
        val bytes: MutableList<Byte> = ArrayList()
        var bitIndex = 0
        var byteValue = 0
        for (k in bits.indices) {
            if (bits[k] == 1) {
                byteValue = byteValue or (1 shl bitIndex)
            }
            bitIndex++
            if (bitIndex == 8) {
                bytes.add(byteValue.toByte())
                bitIndex = 0
                byteValue = 0
            }
        }

        // Verify the checksums
        val byteStream = ByteArray(bytes.size)
        for (k in bytes.indices) {
            byteStream[k] = bytes[k]
        }
        var k = 2
        while (k < 1982) {
            val checksum = byteStream[k + 29]
            val message = byteStream.copyOfRange(k, k + 29)
            val computedChecksum = computeChecksum(message)
            if (computedChecksum != checksum) {
                throw Exception("Checksum mismatch")
            }
            k += 31
        }

        // Convert the byte-stream into human-readable text
        val text = String(byteStream.copyOfRange(2, 1981))
        println(text)
    }

    /* Explanation:

    The method takes a byte array data and computes its checksum using a simple sum modulo 256 algorithm.

    The variable sum is initialized to 0. Then, for each byte in the data array, the method adds its unsigned value (i.e., between 0 and 255) to the sum variable. The & 0xFF operation is used to convert the signed byte value to an unsigned value.

    Finally, the method returns the checksum as a byte value, which is the remainder of sum divided by 256.

    Note that the computeChecksum method assumes that the input byte array data is not null and has at least one element.*/
    private fun computeChecksum(data: ByteArray): Byte {
        var sum = 0
        for (i in data.indices) {
            sum += data[i].toInt() and 0xFF
        }
        return (sum % 256).toByte()
    }
}