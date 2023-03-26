package hello

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.sound.sampled.AudioSystem

object KotlinAFSKDecoder {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Read the WAV file into a byte array

        val audioFile = AudioSystem.getAudioInputStream(File("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav"))
        //val audioBytes = ByteArrayOutputStream()

        // Extract the audio signal to bytes
       /* val buffer = ByteArray(1024)
        var bytesRead = audioFile.read(buffer, 0, buffer.size)
        while (bytesRead != -1) {
            audioBytes.write(buffer, 0, bytesRead)
            bytesRead = audioFile.read(buffer, 0, buffer.size)
        }
        val audioData = audioBytes.toByteArray()*/

        val audioData = ByteArray(audioFile.frameLength.toInt() * audioFile.format.frameSize)
        audioFile.read(audioData)

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

        //Convert the bit-stream to byte-stream
        val byteStream: MutableList<Byte> = ArrayList()
        var bitIndex = 0
        var byteValue = 0
        for (k in bitStream.indices) {
            if (bitStream[k]) {
                byteValue = byteValue or (1 shl bitIndex)
            }
            bitIndex++
            if (bitIndex == 8) {
                byteStream.add(byteValue.toByte())
                bitIndex = 0
                byteValue = 0
            }
        }

        /*// Convert the bit-stream to byte-stream
        val byteStream = mutableListOf<Byte>()
        for (i in bitStream.indices step 11) {
            var byte = 0
            for (j in 0 until 8) {
                if (bitStream[i + j]) {
                    byte = byte or (1 shl j)
                }
            }
            byteStream.add(byte.toByte())
        }*/

        // Check for lead tone and end block
        val leadToneSize = (2.5 * 44100).toInt()
        val endBlockSize = (0.5 * 44100).toInt()
        require(byteStream.subList(0, leadToneSize).all { it == 0xff.toByte() })
        require(byteStream.subList(byteStream.size - endBlockSize, byteStream.size).all { it == 0xff.toByte() })

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