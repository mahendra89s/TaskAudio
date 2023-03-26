
import javax.sound.sampled.AudioSystem
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NewAudioDecoderKotlin {

    @JvmStatic
    fun main() {
        // Load the audio file
        val audioFile = AudioSystem.getAudioInputStream(File("audio.wav"))
        val audioBytes = ByteArrayOutputStream()

        // Extract the audio signal to bytes
        val buffer = ByteArray(1024)
        var bytesRead = audioFile.read(buffer, 0, buffer.size)
        while (bytesRead != -1) {
            audioBytes.write(buffer, 0, bytesRead)
            bytesRead = audioFile.read(buffer, 0, buffer.size)
        }
        val audioData = audioBytes.toByteArray()

        // Extract the bit-stream from the audio signal
        val bitStream = mutableListOf<Boolean>()
        var prevSample = 0
        for (i in audioData.indices step 2) {
            val sample = ByteBuffer.wrap(audioData.copyOfRange(i, i + 2))
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .short
            val isOne = sample > 0 && sample > prevSample
            val isZero = sample < 0 && sample < prevSample
            prevSample = sample.toInt()
            if (isOne) {
                bitStream.addAll(List(320) { true })
            } else if (isZero) {
                bitStream.addAll(List(640) { false })
            }
        }

        // Convert the bit-stream to byte-stream
        val byteStream = mutableListOf<Byte>()
        for (i in bitStream.indices step 11) {
            var byte = 0
            for (j in 0 until 8) {
                if (bitStream[i + j]) {
                    byte = byte or (1 shl j)
                }
            }
            byteStream.add(byte.toByte())
        }

        // Check for lead tone and end block
        val leadToneSize = (2.5 * 44100).toInt()
        val endBlockSize = (0.5 * 44100).toInt()
        require(byteStream.subList(0, leadToneSize).all { it == 0xFF.toByte() })
        require(byteStream.subList(byteStream.size - endBlockSize, byteStream.size).all { it == 0xFF.toByte() })

        // Verify the checksums and get the human-readable text
        val messageSize = 31
        val messageCount = 64
        var text = ""
        for (i in 0 until messageCount) {
            val start = i * messageSize + 2
            val end = start + messageSize - 1
            val message = byteStream.subList(start, end)
            val checksum = byteStream[end].toInt() and 0xFF
            val sum = message.fold(0) { acc, byte -> acc + (byte.toInt() and 0xFF) } % 256
            require(checksum == sum) { "Checksum error in message ${i + 1}" }
            text += message.map { it.toChar() }.joinToString("")
        }
        println(text)
    }
}