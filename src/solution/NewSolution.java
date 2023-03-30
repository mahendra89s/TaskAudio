package solution;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NewSolution {

    private static final int LEAD_DURATION = 2500; // in milliseconds
    private static final int END_DURATION = 500;
    private static final int SAMPLE_RATE = 44100;

    public static void main(String[] args) {
        // Set up audio file and input stream
        File audioFile = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_2.wav");
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(audioFile);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return;
        }

        // Extract audio data as byte array
        byte[] audioData = new byte[(int) ais.getFrameLength() * ais.getFormat().getFrameSize()];
        try {
            ais.read(audioData);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Decode AFSK-modulated audio data to binary data
        byte[] binaryData = decode(audioData);

        // Convert binary data to text
        String text = new String(binaryData, StandardCharsets.UTF_8);

        // Print the decoded text
        System.out.println(text);
    }


    public static byte[] decode(byte[] audioData) {

        // Set AFSK decoding parameters
        int baudRate = 1200;
        int toneFrequency = 2200;
        int markFrequency = 1200;

        // Define constants for lead and end durations
        double LEAD_DURATION = 2.5; // seconds
        double END_DURATION = 0.5; // seconds

        // Calculate the sample rate
        int SAMPLE_RATE = 44100;

        // Calculate the duration of one bit in milliseconds
        double bitDuration = 1000.0 / baudRate;

        // Calculate the number of samples per bit
        int samplesPerBit = (int) (SAMPLE_RATE * bitDuration / 1000.0);

        // Calculate the number of samples per tone
        int samplesPerTone = SAMPLE_RATE / toneFrequency;

        // Calculate the number of samples per mark tone
        int samplesPerMark = SAMPLE_RATE / markFrequency;

        // Calculate the number of samples for the lead and end tones
        int samplesLead = (int) (LEAD_DURATION * SAMPLE_RATE);
        int samplesEnd = (int) (END_DURATION * SAMPLE_RATE);

        // Create a StringBuilder to store the decoded binary data
        StringBuilder binaryDataBuilder = new StringBuilder();

        // Initialize the sample index and bit index
        int sampleIndex = samplesLead;
        int bitIndex = 0;

        // Loop through each byte in the audio data
        for (int i = 0; i < audioData.length; i++) {
            // Loop through each bit in the byte
            for (int j = 0; j < 8; j++) {
                // Get the sample value for the current bit
                int sampleValue = 0;
                if (((audioData[i] >> (7 - j)) & 1) == 1) {
                    // Current bit is a mark tone
                    sampleValue = Short.MAX_VALUE * markFrequency / SAMPLE_RATE;
                } else {
                    // Current bit is a space tone
                    sampleValue = Short.MAX_VALUE * toneFrequency / SAMPLE_RATE;
                }

                // Loop through the samples for the current bit
                for (int k = 0; k < samplesPerBit; k++) {
                    // Add the sample value to the binary data builder
                    binaryDataBuilder.append((sampleValue > 0) ? "1" : "0");

                    // Increment the sample index
                    sampleIndex++;

                    // If we have reached the end of a tone, skip to the next tone
                    if (sampleIndex % samplesPerTone == 0) {
                        sampleIndex += samplesPerBit;
                    }
                }

                // Increment the bit index
                bitIndex++;

                // If we have reached the end of a byte, convert it to a character and add it to the binary data builder
                if (bitIndex % 8 == 0) {
                    String byteString = binaryDataBuilder.substring(binaryDataBuilder.length() - 8);
                    char c = (char) Integer.parseInt(byteString, 2);
                    binaryDataBuilder.setLength(binaryDataBuilder.length() - 8);
                    binaryDataBuilder.append(c);
                }
            }
        }

        // Get the decoded binary data as a byte array
        String binaryDataString = binaryDataBuilder.toString();
        int numBytes = binaryDataString.length() / 8;
        byte[] binaryData = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            String byteString = binaryDataString.substring(i * 8, (i + 1) * 8);
            binaryData[i] = (byte) Integer.parseInt(byteString, 2);
        }

        return binaryData;
    }

}

