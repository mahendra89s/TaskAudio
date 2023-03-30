package solution;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class EndodeAudio {
    static String text= "It was the White Rabbit, trotting slowly back again, and looking anxiously about as it went, as if it had lost something; and she heard it muttering to itself `The Duchess! The Duchess! Oh my dear paws! Oh my fur and whiskers! She'll get me executed, as sure as ferrets are ferrets! Where can I have dropped them, I wonder?' Alice guessed in a moment that it was looking for the fan and the pair of white kid gloves, and she very good-naturedly began hunting about for them, but they were nowhere to be seen--everything seemed to have changed since her swim in the pool, and the great hall, with the glass table and the little door, had vanished completely.Very soon the Rabbit noticed Alice, as she went hunting about, and called out to her in an angry tone, Why, Mary Ann, what are you doing out here? Run home this moment, and fetch me a pair of gloves and a fan! Quick, now! And Alice was so much frightened that she ran off at once in the direction it pointed to, without trying to explain the mistake it had made.He took me for his housemaid,' she said to herself as she ran. `How surprised he'll be when he finds out who I am! But I'd better take him his fan and gloves--that is, if I can find them. As she said this, she came upon a neat little house, on the door of which was a bright brass plate with the name `W. RABBIT' engraved upon it. She went in without knocking, and hurried upstairs, in great fear lest she should meet the real Mary Ann, and be turned out of the house before she had found the fan and gloves.How queer it seems,Alice said to herself, to be going messages for a rabbit! I suppose Dinah ll be sending me on messages next!' And she began fancying the sort of thing that would happen: `\"Miss Alice! Come here directly, and get ready for your walk! Coming in a minute, nurse! But I've got to see that the mouse doesn't get out. Only I don't think,' Alice went on, `that they";
    public static void main(String[] args) {



                // Set up audio file and input stream
                File audioFile = new File("audio.wav");
                /*byte[] audioData = null;
                try {
                    audioData = Files.readAllBytes(audioFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                // Parse binary data into text data
                StringBuilder textDataBuilder = new StringBuilder(text);
                // Use AFSK decoding scheme described in the prompt to extract binary data and convert to text

                // Convert text data to byte array
                byte[] byteData = textDataBuilder.toString().getBytes(StandardCharsets.UTF_8);

                // Set up audio format
                float sampleRate = 44100;
                int sampleSizeInBits = 16;
                int channels = 2;
                boolean signed = true;
                boolean bigEndian = false;
                AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

                // Use AFSK modulation to generate new audio file
                int baudRate = 1200;
                int toneFrequency = 1200;
                int markFrequency = 2200;
                byte[] afskData = encode(byteData, baudRate, toneFrequency, markFrequency);
                ByteArrayInputStream bais = new ByteArrayInputStream(afskData);
                AudioInputStream ais = new AudioInputStream(bais, audioFormat, afskData.length);

                // Write new audio file
                File outputFile = new File("output.wav");
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    private static final int SAMPLE_RATE = 44100;
    private static final int LEAD_DURATION = 2500;
    private static final int END_DURATION = 500;
    public static byte[] encode(byte[] data, int baudRate, int toneFrequency, int markFrequency) {
        // Calculate the duration of one bit in milliseconds
        double bitDuration = 1000.0 / baudRate;

        // Calculate the number of samples per bit
        int samplesPerBit = (int)(SAMPLE_RATE * bitDuration / 1000.0);

        // Calculate the number of samples per tone
        int samplesPerTone = SAMPLE_RATE / toneFrequency;

        // Calculate the number of samples per mark tone
        int samplesPerMark = SAMPLE_RATE / markFrequency;

        // Calculate the number of samples for the lead and end tones
        int samplesLead = (int)(LEAD_DURATION * SAMPLE_RATE);
        int samplesEnd = (int)(END_DURATION * SAMPLE_RATE);

        // Create the output array
        byte[] output = new byte[(int)Math.ceil((samplesLead + (data.length * 11) + samplesEnd) / 8.0)];

        // Initialize the sample index and output bit index
        int sampleIndex = 0;
        int outputBitIndex = 0;

        // Add the lead tone
        for (int i = 0; i < samplesLead; i++) {
            short sampleValue = (short)(Short.MAX_VALUE * Math.sin(2 * Math.PI * toneFrequency * sampleIndex / SAMPLE_RATE));
            outputBitIndex = setBit(output, outputBitIndex, sampleValue > 0);
            sampleIndex++;
        }

        // Encode the data
        for (byte b : data) {
            // Add the start bit
            outputBitIndex = setBit(output, outputBitIndex, false);

            // Encode the data bits
            for (int i = 0; i < 8; i++) {
                boolean dataBit = (b & (1 << i)) != 0;
                int toneSamples = dataBit ? samplesPerMark : samplesPerTone;
                for (int j = 0; j < toneSamples; j++) {
                    short sampleValue = (short)(Short.MAX_VALUE * Math.sin(2 * Math.PI * toneFrequency * sampleIndex / SAMPLE_RATE));
                    outputBitIndex = setBit(output, outputBitIndex, sampleValue > 0);
                    sampleIndex++;
                }
            }

            // Add the stop bits
            outputBitIndex = setBit(output, outputBitIndex, true);
            outputBitIndex = setBit(output, outputBitIndex, true);
        }

        // Add the end tone
        for (int i = 0; i < samplesEnd; i++) {
            short sampleValue = (short)(Short.MAX_VALUE * Math.sin(2 * Math.PI * toneFrequency * sampleIndex / SAMPLE_RATE));
            outputBitIndex = setBit(output, outputBitIndex, sampleValue > 0);
            sampleIndex++;
        }

        return output;
    }

    private static int setBit(byte[] array, int index, boolean value) {
        int byteIndex = index / 8;
        int bitIndex = index % 8;
        if(byteIndex < array.length){
            if (value) {
                array[byteIndex] |= (1 << bitIndex);
            } else {
                array[byteIndex] &= ~(1 << bitIndex);
            }
        }
        return index + 1;
    }


}
