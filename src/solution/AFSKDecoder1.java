package solution;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AFSKDecoder1 {
    static final int ONE_DURATION = 320; // in microseconds
    static final int ZERO_DURATION = 640; // in microseconds
    private static final byte[] LEAD_TONE = new byte[2_500_000]; // 2.5 seconds of 1-bits
    private static final byte[] END_BLOCK = new byte[500_000]; // 0.5 seconds of 1-bits

    static {
        // Initialize the lead tone and end block arrays with 1-bits
        java.util.Arrays.fill(LEAD_TONE, (byte) 0xFF);
        java.util.Arrays.fill(END_BLOCK, (byte) 0xFF);
    }
    public static void main(String[] args) throws Exception {
        try {
            // Step 1: Read the audio file and extract the audio data
            File audioFile = new File("//home//user//TaskAudio//src//file_2.wav");

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);

            // Get the audio format
            AudioFormat format = audioIn.getFormat();

            // Calculate the number of bytes per frame
            int bytesPerFrame = format.getFrameSize();

            // Calculate the number of frames in the audio stream
            long frames = audioIn.getFrameLength();

            // Calculate the total number of bytes in the audio stream
            int totalBytes = (int) (frames * bytesPerFrame);

            // Read all bytes from the audio stream
            byte[] audioData = new byte[totalBytes];
            audioIn.read(audioData);

            // Step 2: Decode the audio data using AFSK modulation
            StringBuilder bitStreamBuilder = new StringBuilder();

            int sampleRate = 44100;
            int zeroThreshold = 15000; // adjust this threshold based on the audio file
            for (int i = 0; i < audioData.length; i += 2) {
                int sample = (audioData[i + 1] << 11) | (audioData[i] & 0xff);
                if (sample > sampleRate) {
                    bitStreamBuilder.append("1");
                } else {
                    bitStreamBuilder.append("0");
                }
            }


             //Step 3: Convert the bitstream into bytes
            StringBuilder byteStreamBuilder = new StringBuilder();
            String bitStream = bitStreamBuilder.toString();
            for (int i = 0; i < bitStream.length(); i += 11) {
                String byteString = bitStream.substring(i, i + 8);
                byteStreamBuilder.append(byteString);
            }
            String byteStream = byteStreamBuilder.toString();

            // Step 4: Verify the checksum of each message
            StringBuilder messageBuilder = new StringBuilder();
            int byteCount = 0;
            for (int i = 0; i < byteStream.length(); i += 11) {
                String byteString = byteStream.substring(i, i + 8);
                byte b = (byte) Integer.parseInt(byteString, 2);
                messageBuilder.append((char) b);
                byteCount++;
                if (byteCount % 31 == 0) {
                    byte checksum = (byte) 0;
                    for (int j = i - 30; j < i; j += 8) {
                        String checksumByteString = byteStream.substring(j, j + 8);
                        byte checksumByte = (byte) Integer.parseInt(checksumByteString, 2);
                        checksum = (byte) ((checksum + checksumByte) % 256);
                    }
                    String checksumByteString = byteStream.substring(i, i + 8);
                    byte expectedChecksum = (byte) Integer.parseInt(checksumByteString, 2);
                    if (checksum != expectedChecksum) {
                        System.err.println("Checksum error in message " + (byteCount / 31));
                    }
                }
            }

            // Step 5: Extract the human-readable text from the decoded data
            String message = messageBuilder.toString();
            System.out.println(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Find the index of the first occurrence of the given subsequence in the byte array
    private static int findSubsequence(byte[] array, byte[] subsequence) {
        for (int i = 0; i <= array.length - subsequence.length; i++) {
            boolean match = true;
            for (int j = 0; j < subsequence.length; j++) {
                if (array[i + j] != subsequence[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        throw new IllegalArgumentException("Subsequence not found");
    }
}