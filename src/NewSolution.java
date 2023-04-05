
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class NewSolution {

    // Constants
    private static final int SAMPLE_RATE = 44100; // Hz
    private static final int BITS_PER_SAMPLE = 16; // bits
    private static final int CHANNELS = 2;
    private static final int BYTES_PER_FRAME = BITS_PER_SAMPLE / 8 * CHANNELS;
    private static final double ONE_BIT_DURATION = 320e-6; // seconds
    private static final double ZERO_BIT_DURATION = 640e-6; // seconds
    private static final double LEAD_TONE_DURATION = 2.5; // seconds
    private static final double END_BLOCK_DURATION = 0.5; // seconds

    public static void main(String[] args) throws Exception {
        // Load the WAV file
        File file = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav");
        byte[] audioData = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(audioData);
        }

        // Convert stereo audio data to mono audio data
        byte[] monoData = convertStereoToMono(audioData);

        // Extract the bit stream from the audio data
        byte[] bitStream = extractBitStream(monoData);

        // Convert the bit stream to a byte stream
        byte[] byteStream = convertBitStreamToByteStream(bitStream);

        // Verify the byte stream
        verifyByteStream(byteStream);

        // Convert the byte stream to human-readable text
        String text = convertByteStreamToText(byteStream);
        System.out.println(text);
    }

    private static byte[] convertStereoToMono(byte[] stereoData) {
        ShortBuffer stereoBuffer = ByteBuffer.wrap(stereoData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] monoSamples = new short[stereoBuffer.capacity() / 2];
        for (int i = 0; i < stereoBuffer.capacity(); i += 2) {
            monoSamples[i / 2] = (short) ((stereoBuffer.get(i) + stereoBuffer.get(i + 1)) / 2);
        }
        ByteBuffer monoBuffer = ByteBuffer.allocate(monoSamples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortBuffer = monoBuffer.asShortBuffer();
        shortBuffer.put(monoSamples);
        return monoBuffer.array();
    }

    private static byte[] extractBitStream(byte[] audioData) {
        int numSamples = audioData.length / BYTES_PER_FRAME;
        int numBits = numSamples * 8;
        byte[] bitStream = new byte[numBits];
        int bitIndex = 0;
        boolean isHigh = false;
        int highStart = -1;
        for (int i = 0; i < numSamples; i++) {
            short sample = ByteBuffer.wrap(audioData, i * BYTES_PER_FRAME, BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN).getShort();
            if (sample > 0) {
                if (!isHigh) {
                    highStart = i;
                    isHigh = true;
                }
            } else {
                if (isHigh) {
                    double bitDuration = (i - highStart) * 1.0 / SAMPLE_RATE;
                    if (bitDuration < ZERO_BIT_DURATION) {
                        bitStream[bitIndex++] = 0;
                    } else {
                        bitStream[bitIndex++] = 1;
                    }
                    isHigh = false;
                }
            }
        }
        return bitStream;
    }

    private static byte[] convertBitStreamToByteStream(byte[] bitStream) {
        // Each byte is encoded using 11 bits
        int numBytes = bitStream.length / 11;
        byte[] byteStream = new byte[numBytes];

        // Iterate over the bit stream, processing each group of 11 bits as a byte
        for (int i = 0; i < numBytes; i++) {
            // Read the 11 bits for this byte
            int startBitIndex = i * 11;
            int endBitIndex = startBitIndex + 11;
            byte[] bits = Arrays.copyOfRange(bitStream, startBitIndex, endBitIndex);

            // Convert the bits to a byte
            byte b = 0;
            for (int j = 1; j < 9; j++) {
                if (bits[j+1]==1) {
                    b |= (1 << j);
                }
            }

            byteStream[i] = b;
        }

        return byteStream;
    }

    private static void verifyByteStream(byte[] byteStream) {
        byte[] expectedStart = {(byte) 0x42, (byte) 0x03};
        if (!Arrays.equals(Arrays.copyOf(byteStream, 2), expectedStart)) {
            throw new RuntimeException("Start of byte stream does not match expected value");
        }
        for (int i = 0; i < 64; i++) {
            byte[] messageBytes = Arrays.copyOfRange(byteStream, i * 31 + 2, (i + 1) * 31 - 1);
            byte expectedChecksum = byteStream[(i + 1) * 31 - 1];
            byte actualChecksum = calculateChecksum(messageBytes);
            if (actualChecksum != expectedChecksum) {
                throw new RuntimeException("Checksum error in message " + (i + 1));
            }
        }
        if (byteStream[1983] != 0) {
            throw new RuntimeException("End of byte stream does not match expected value");
        }
    }

    private static byte calculateChecksum(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) {
            sum += b & 0xFF;
        }
        return (byte) (sum % 256);
    }

    private static String convertByteStreamToText(byte[] byteStream) {
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < 1984; i++) {
            sb.append((char) byteStream[i]);
        }
        return sb.toString();
    }

}


