package solution;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AFSKDecoder1 {
    static final int ONE_DURATION = 320; // in microseconds
    static final int ZERO_DURATION = 640; // in microseconds
    public static void main(String[] args) throws Exception {
        try {
            // Step 1: Read the audio file and extract the audio data
            File audioFile = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_2.wav");

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

            // Step 3: Convert the bitstream into bytes
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


}