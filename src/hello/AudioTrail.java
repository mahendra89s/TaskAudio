package hello;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class AudioTrail {
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {



        // Load the audio file as an input stream
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav"));

        // Set the sample rate and duration of the AFSK-encoded signal
        double sampleRate = audioInputStream.getFormat().getSampleRate();
        double duration = audioInputStream.getFrameLength() / sampleRate;

        // Define the bit durations for one and zero signals
        double oneDuration = 320e-6;
        double zeroDuration = 640e-6;

        // Calculate the number of samples for each bit duration
        int oneSamples = (int) Math.round(oneDuration * sampleRate);
        int zeroSamples = (int) Math.round(zeroDuration * sampleRate);

        // Read the audio data from the input stream
        byte[] audioData = new byte[(int) (duration * audioInputStream.getFormat().getFrameRate())];
        audioInputStream.read(audioData);

        // Initialize the bit-stream and the start and end sample indices
        StringBuilder bitStream = new StringBuilder();
        int startSample = 0;
        int endSample;

        // Analyze the waveform to extract the bit-stream
        for (int i = 1; i < audioData.length; i++) {
            double prevValue = audioData[i - 1];
            double curValue = audioData[i];

            // Detect a zero-crossing and compute the sample distance between two zero-crossings
            if (prevValue < 0 && curValue >= 0) {
                endSample = i;
                int sampleDistance = endSample - startSample;

                // If the sample distance matches the one-duration, add a one-bit to the bit-stream
                if (sampleDistance >= oneSamples * 0.9 && sampleDistance <= oneSamples * 1.1) {
                    bitStream.append("1");
                }
                // If the sample distance matches the zero-duration, add a zero-bit to the bit-stream
                else if (sampleDistance >= zeroSamples * 0.9 && sampleDistance <= zeroSamples * 1.1) {
                    bitStream.append("0");
                }

                startSample = i;
            }
        }

        // Print the extracted bit-stream
        System.out.println("Bit-stream: " + bitStream);

        // Convert the bit-stream to a byte-stream
        StringBuilder byteStream = new StringBuilder();
        for (int i = 0; i < bitStream.length(); i += 11) {
            // Extract the 11-bit block from the bit-stream
            String block = bitStream.substring(i, i + 11);

            // Ignore any incomplete blocks at the end of the bit-stream
            if (block.length() < 11) {
                break;
            }

            // Decode the 11-bit block to a byte and add it to the byte-stream
            byte b = 0;
            for (int j = 0; j < 8; j++) {
                int bit = block.charAt(j) == '1' ? 1 : 0;
                b |= (byte) (bit << j);
            }
            byteStream.append((char) b);
        }

        // Print the byte-stream as hexadecimal values
        System.out.println("Byte-stream: " + byteStream.toString().getBytes());

        // Check for the lead tone at the beginning of the byte-stream
        boolean hasLeadTone = true;
        for (int i = 0; i < 500; i++) {
            if (byteStream.charAt(i) != (char) 0xff) {
                hasLeadTone = false;
                break;
            }
        }
        if (!hasLeadTone) {
            System.out.println("Lead tone not detected");
            return;
        }

        // Check for the end block at the end of the byte-stream
        boolean hasEndBlock = true;
        for (int i = byteStream.length() - 501; i < byteStream.length() - 1; i++) {
            if (byteStream.charAt(i) != (char) 0xff) {
                hasEndBlock = false;
                break;
            }
        }
        if (!hasEndBlock) {
            System.out.println("End block not detected");
            return;
        }

        // Split the byte-stream into 64 messages of 30 bytes each
        String[] messages = new String[64];
        for (int i = 0; i < 64; i++) {
            int start = i * 31 + 2;
            int end = start + 30;
            if(end > byteStream.length()){
                break;
            }
            messages[i] = byteStream.substring(start, end);
        }

        // Compute the checksum for each message
        for (int i = 0; i < 64; i++) {
            String message = messages[i];
            int checksum = 0;
            for (int j = 0; j < 30; j++) {
                char c = message.charAt(j);
                checksum += (int) c;
            }
            checksum %= 256;
            // Check the checksum against the last byte of the message
            char expectedChecksum = message.charAt(29);
            if (checksum != (int) expectedChecksum) {
                System.out.println("Checksum mismatch for message " + i);
                return;
            }
        }

        // Convert the byte-stream to a human-readable text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            String message = messages[i].substring(0, 30);
            for (int j = 0; j < message.length(); j += 11) {
                String byteString = message.substring(j, j + 11);
                byte b = 0;
                for (int k = 0; k < 8; k++) {
                    char c = byteString.charAt(k + 1);
                    if (c == '1') {
                        b |= (1 << k);
                    }
                }
                sb.append((char) b);
            }
        }
        String decodedText = sb.toString();

        System.out.println("decodedText : " + decodedText);
    }
}
