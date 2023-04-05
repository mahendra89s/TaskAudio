package solution;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class AFSKDecoder1 {
    public static void main(String[] args) throws Exception {
        try {
            // Step 1: Read the audio file and extract the audio data
            File audioFile = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav");


            AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);



            StringBuilder bitStreamBuilder = new StringBuilder();


            int numChannels = audioIn.getFormat().getChannels();
            int bytesPerSample = audioIn.getFormat().getSampleSizeInBits() / 8;
            int frameSize = numChannels * bytesPerSample;
            int sampleRate = (int) audioIn.getFormat().getSampleRate();
            int numSamples = (int) (audioIn.getFrameLength() * numChannels);
            //byte[] audioData = new byte[numSamples * bytesPerSample];

            //audioIn.read(audioData);

            File file = new File("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav");
            byte[] audioData = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(audioData);
            }

            int zeroDuration = (sampleRate * 640) / 1000000;
            int oneDuration = (sampleRate * 320) / 1000000;

            // Decode the audio data using AFSK modulation
            int[] samples = new int[numSamples];
            for (int i = 0; i < numSamples; i++) {
                int sampleValue = 0;
                for (int j = 0; j < bytesPerSample; j++) {
                    int offset = (i * frameSize) + (j * numChannels);
                    if (offset < audioData.length) {
                        sampleValue |= ((int) audioData[offset]) << (j * 8);
                    }
                }
                samples[i] = sampleValue;
            }

            List<Integer> zeroCrossings = new ArrayList<>();
            for (int i = 1; i < numSamples; i++) {
                if ((samples[i - 1] >= 0 && samples[i] < 0) || (samples[i - 1] < 0 && samples[i] >= 0)) {
                    zeroCrossings.add(i);
                }
            }

            for (int i = 1; i < zeroCrossings.size(); i++) {
                int period = zeroCrossings.get(i) - zeroCrossings.get(i - 1);
                if (period < zeroDuration) {
                    bitStreamBuilder.append("1");
                } else {
                    bitStreamBuilder.append("0");
                }
            }

            // remove lead tone and end block
            int startIndex = (int) Math.ceil(2.5 * sampleRate)/11;
            int endIndex = bitStreamBuilder.length() - (int) Math.ceil(0.5 * sampleRate)/11;
            String dataBits = bitStreamBuilder.substring(startIndex, endIndex);

            //remove header of 2 bytes and 1 byte at end
            /*int startIndex1 = 22;
            int endIndex1 = dataBits.length()-11;
            String dataBits1 = dataBits.substring(startIndex1,endIndex1);*/

            // Convert the bitstream into bytes
            StringBuilder byteStreamBuilder = new StringBuilder();
            String bitStream = dataBits.toString();
            for (int i = 0; i < bitStream.length(); i += 11) {
                if(i+9 < bitStream.length()){
                    StringBuilder byteString = new StringBuilder(bitStream.substring(i + 1, i + 9));
                    //encoded with least significant bit first
                    byteString.reverse();
                    byteStreamBuilder.append(byteString);
                }
            }
            String byteStream = byteStreamBuilder.toString();

            //Verify the checksum of each message
            StringBuilder messageBuilder = new StringBuilder();
            int byteCount = 0;
            for (int i = 0; i < byteStream.length(); i += 8) {
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

            // Extract the human-readable text from the decoded data
            String message = messageBuilder.toString();
            System.out.println(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}