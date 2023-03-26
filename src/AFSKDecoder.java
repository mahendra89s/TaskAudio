
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import javax.sound.sampled.*;

public class AFSKDecoder {
    public static void main(String[] args) throws Exception {
        // Load the WAV file
        File file = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_2.wav");
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

        // Get the audio format and channels
        AudioFormat format = audioInputStream.getFormat();
        int channels = format.getChannels();

        // Read the data into a byte array
        byte[] data = new byte[audioInputStream.available()];
        audioInputStream.read(data);

        // Extract the bitstream from the data
        ArrayList<Byte> bitstream = new ArrayList<>();
        int lastSample = 0;
        boolean isHigh = false;
        for (int i = 0; i < data.length; i += 2 * channels) {
            // Get the sample value as a short
            short sample = ByteBuffer.wrap(new byte[] { data[i], data[i + 1] }).order(ByteOrder.LITTLE_ENDIAN).getShort();

            // Determine whether the signal is high or low
            boolean newIsHigh = (sample > 0) ? true : false;

            // If the signal has changed, add a bit to the bitstream
            if (newIsHigh != isHigh) {
                if (isHigh) {
                    bitstream.add((byte) 1);
                } else {
                    bitstream.add((byte) 0);
                }
                isHigh = newIsHigh;
            }

            // If the signal is high, skip the next 320 microseconds
            if (isHigh) {
                i += (int) (format.getSampleRate() * 320 / 1e6) * 2 * channels;
            }
        }

        // Convert the bitstream to a byte array
        byte[] bytes = new byte[bitstream.size() / 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (bitstream.get(i * 8 + j) == 1) {
                    bytes[i] |= (1 << j);
                }
            }
        }

        // Print the byte array as a string of hex digits
        for (byte b : bytes) {
            System.out.printf("%02x ", b);
        }
        System.out.println();
    }

}
