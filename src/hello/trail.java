package hello;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
public class trail {
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {


// Define constants
        final int ONE_DURATION = 320; // in microseconds
        final int ZERO_DURATION = 640; // in microseconds
        final int SAMPLE_RATE = 44100; // in Hz
        final int MESSAGE_SIZE = 30; // in bytes
        final int NUM_MESSAGES = 64; // total number of messages
        final int BYTES_PER_MESSAGE = MESSAGE_SIZE + 1; // including checksum byte
        final int TOTAL_BYTES = BYTES_PER_MESSAGE * NUM_MESSAGES; // total number of bytes

// Open audio file
        File file = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav");
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

// Read audio data
        byte[] audioData = new byte[(int) audioInputStream.getFrameLength() * audioInputStream.getFormat().getFrameSize()];
        audioInputStream.read(audioData);

// Convert audio data to digital signal
        List<Integer> digitalSignal = new ArrayList<Integer>();
        for (int i = 0; i < audioData.length; i += 2) {
            int sample = (audioData[i + 1] << 8) | audioData[i];
            digitalSignal.add(sample);
        }

// Detect zero-crossings to extract bit-stream
        List<Integer> bitStream = new ArrayList<Integer>();
        int threshold = 0;
        for (int i = 1; i < digitalSignal.size(); i++) {
            int prevSample = digitalSignal.get(i - 1);
            int currSample = digitalSignal.get(i);
            if (prevSample < threshold && currSample >= threshold) { // zero-crossing
                int bitDuration = i - (bitStream.size() * 2); // duration in microseconds
                if (bitDuration < ONE_DURATION) {
                    bitStream.add(1); // one signal
                } else if (bitDuration > ZERO_DURATION) {
                    bitStream.add(0); // zero signal
                }
            }
        }

// Convert bit-stream to byte-stream
        List<Byte> byteStream = new ArrayList<Byte>();
        for (int i = 0; i < bitStream.size(); i += 11) {
            int byteValue = 0;
            for (int j = 0; j < 8 && i+j<bitStream.size(); j++) {
                byteValue |= (bitStream.get(i + j) << j); // least-significant bit first
            }
            byteStream.add((byte) byteValue);
        }

// Verify checksums
        int sum = 0;
        for (int i = 2; i < TOTAL_BYTES; i++) {
            sum += byteStream.get(i);
            if (i % BYTES_PER_MESSAGE == MESSAGE_SIZE) {
                if ((byte) sum != byteStream.get(i + 1)) {
                    System.out.println("Checksum error in message " + (i / BYTES_PER_MESSAGE));
                    return;
                }
                sum = 0;
            }
        }

        // Extract human-readable text
        String text = Arrays.toString(byteStream.toArray(new Byte[0]));
        //String text = new String(byteStream.toArray(new Byte[0]), 2, TOTAL_BYTES - 3);
        System.out.println(text);

    }
}
