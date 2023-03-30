package solution;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewDecoder {
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
        double sampleRate = 44100; // the audio sample rate
        double lowFrequency = 1200; // the low AFSK frequency
        double highFrequency = 2200; // the high AFSK frequency
        int bitDuration = 16 ; // the number of samples per bit
        int halfBitDuration = bitDuration / 2;
        int zeroDuration = (int) Math.round(sampleRate * 640 / 1000000); // duration of a zero bit in samples
        int oneDuration = (int) Math.round(sampleRate * 320 / 1000000); // duration of a one bit in samples
        int leadDuration = (int) Math.round(sampleRate * 2.5); // duration of the lead tone in samples
        int endDuration = (int) Math.round(sampleRate * 0.5); // duration of the end block in samples

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

        // detect zero crossings
        List<Integer> zeroCrossings = new ArrayList<>();
        for (int i = 1; i < audioData.length; i++) {
            if ((audioData[i] > 0 && audioData[i - 1] < 0) || (audioData[i] < 0 && audioData[i - 1] > 0)) {
                zeroCrossings.add(i);
            }
        }

        // detect transitions and decode bits
        List<Integer> transitions = new ArrayList<>();
        for (int i = 1; i < zeroCrossings.size(); i++) {
            int duration = zeroCrossings.get(i) - zeroCrossings.get(i - 1);
            if (duration > zeroDuration - halfBitDuration && duration < zeroDuration + halfBitDuration) {
                transitions.add(0);
            } else if (duration > oneDuration - halfBitDuration && duration < oneDuration + halfBitDuration) {
                transitions.add(1);
            }
        }

        // remove lead tone and end block
        transitions = transitions.subList(leadDuration / bitDuration, transitions.size() - endDuration / bitDuration);

        // group bits into bytes
        List<Integer> bits = new ArrayList<>();
        for (int i = 0; i < transitions.size()-1 ; i += 2) {
            int byteValue = 0;
            for (int j = i + 1; j < i + 9 && j<transitions.size(); j++) {
                byteValue |= transitions.get(j) << (j - i - 1);
            }
            bits.add(byteValue);
        }

        // convert bits to bytes
        byte[] byteStream = new byte[bits.size()];
        for (int i = 0; i < bits.size(); i++) {
            byteStream[i] = (byte) bits.get(i).intValue();
        }

        for (int i = 0; i < byteStream.length; i++) {
            byte reversedByte = 0;
            for (int j = 0; j < 8; j++) {
                reversedByte |= ((byteStream[i] >> j) & 0x01) << (7 - j);
            }
            byteStream[i] = reversedByte;
        }


        // check the first two bytes
        if (byteStream[0] != (byte) 0x42 || byteStream[1] != (byte) 0x03) {
            throw new IllegalArgumentException("Invalid byte stream format.");
        }

        // extract the messages
        List<byte[]> messages = new ArrayList<>();
        for (int i = 2; i < byteStream.length - 1; i += 31) {
            byte[] message = Arrays.copyOfRange(byteStream, i, i + 30);
            byte checksum = byteStream[i + 30];
            if (calculateChecksum(message) != checksum) {
                throw new IllegalArgumentException("Invalid message checksum.");
            }
            messages.add(message);
        }

        // verify the checksums
        for (int i = 0; i < byteStream.length; i += 31) {
            byte[] message = Arrays.copyOfRange(byteStream, i, i + 30);
            byte checksum = byteStream[i + 30];
            if (calculateChecksum(message) != checksum) {
                throw new IllegalArgumentException("Invalid message checksum.");
            }
        }


        // check the last byte
        if (byteStream[byteStream.length - 1] != (byte) 0x00) {
            throw new IllegalArgumentException("Invalid byte stream format.");
        }

        // calculate the checksum of a message


        String text = new String(byteStream, StandardCharsets.US_ASCII);
        System.out.println(text);
    }

    private static byte calculateChecksum(byte[] message) {
        int sum = 0;
        for (byte b : message) {
            sum += b & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }
}
