import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

fun main() {
    val audioFilePath = "//Users//webwerks//IdeaProjects//Audio Task//src//file_2.wav"
    val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(File(audioFilePath))
    val audioBytes = audioInputStream.readAllBytes()
    val bits = decodeAFSK(audioBytes)

    val leadToneDuration = 2.5 // seconds
    val leadToneBits = (leadToneDuration / 0.032).toInt() // 0.032 = bit duration in seconds
    val messageBits = 30 * 8 + 11 // 30 bytes with 11 start/stop bits
    val messageCount = (bits.size - leadToneBits - messageBits - 1) / messageBits
    val messages = mutableListOf<ByteArray>()

    // Parse messages
    var pos = leadToneBits
    for (i in 0 until messageCount) {
        val message = mutableListOf<Byte>()

        // Extract message bytes
        for (j in 0 until 30) {
            var byte = 0.toByte()

            // Extract each bit of the byte
            for (k in 0 until 8) {
                byte = (byte + (bits[pos++] shl k)).toByte()
            }

            message.add(byte)
        }

        // Verify checksum
        val checksum = message.fold(0) { sum, byte -> sum + byte.toInt() } % 256
        if (checksum != bits[pos++]) {
            println("Invalid checksum for message $i")
            continue
        }

        messages.add(message.toByteArray())
    }

    println("Decoded messages:")
    for (message in messages) {
        println(message.decodeToString())
    }
}

fun decodeAFSK(bytes: ByteArray): List<Int> {
    var lastSample = 0
    var state = 0
    var bit = 0
    val bits = mutableListOf<Int>()

    for (i in bytes.indices step 2) {
        // Compute the current sample from the 16-bit little-endian PCM data
        val sample = (bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)

        // Check if the signal has crossed the zero line
        if (lastSample <= 0 && sample > 0) {
            // Determine the bit value based on the state and bit duration
            bits.add(if (state == 1) 1 else 0)
            bit++

            // Reset the state and bit counter for the next bit
            if (bit == 2) {
                state = 0
                bit = 0
            }
        }

        // Determine the state based on the signal level and bit duration
        if (sample > 0x2000) {
            state = 1
        } else if (sample < -0x2000) {
            state = 0
        }

        lastSample = sample
    }

    return bits
}
