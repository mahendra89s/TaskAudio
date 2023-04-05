package solution;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class AFSKDemodulator {
    private static final double PI = 3.141592653589793;
    private static final int BAUD_RATE = 300;
    private static final int MARK_FREQ = 1200;
    private static final int SPACE_FREQ = 2200;
    private static final int ZERO_LENGTH = 640; // in microseconds
    private static final int ONE_LENGTH = 320; // in microseconds
    private static final int LEAD_DURATION = 2500; // in milliseconds
    private static final int END_DURATION = 500; // in milliseconds
    private static final int MESSAGE_LENGTH = 31; // 8 data bits + 1 start bit + 2 stop bits = 11 bits per byte
    private static final int CHECKSUM_LENGTH = 1; // 1 byte for checksum

    public static void main(String[] args) throws Exception {
        byte[] byteStream = demodulate("//Users//webwerks//IdeaProjects//Audio Task//src//file_1.wav");
        String text = new String(byteStream, StandardCharsets.US_ASCII);
        System.out.println(text);
    }

    public static byte[] demodulate(String filename) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filename));
        AudioFormat format = ais.getFormat();
        float sampleRate = format.getSampleRate();
        int frameSize = format.getFrameSize();
        byte[] buffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int leadSamples = (int) (LEAD_DURATION * sampleRate / 1000);
        int endSamples = (int) (END_DURATION * sampleRate / 1000);
        int messageCount = 64;
        int messageBytes = MESSAGE_LENGTH - CHECKSUM_LENGTH;
        int totalBytes = messageCount * messageBytes + 2;
        int[] zeroCrossings = new int[2];
        int index = 0;
        boolean inLeadTone = true;
        boolean inEndBlock = false;
        while ((bytesRead = ais.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i += frameSize) {
                double sample = getSample(buffer, i, format);
                if (inLeadTone) {
                    leadSamples--;
                    if (leadSamples <= 0) {
                        inLeadTone = false;
                    }
                } else if (inEndBlock) {
                    endSamples--;
                    if (endSamples <= 0) {
                        return baos.toByteArray();
                    }
                } else {
                    zeroCrossings[index] = getZeroCrossing(sample, buffer, i, frameSize, format);
                    index = (index + 1) % 2;
                    if (index == 0) {
                        int length = zeroCrossings[1] - zeroCrossings[0];
                        int bit = demodulateBit(length, sampleRate);
                        if (bit == 1) {
                            byte[] message = readMessage(ais, totalBytes);
                            if (message != null) {
                                baos.write(message, 0, message.length);
                            } else {
                                inEndBlock = true;
                            }
                        }
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    private static double getSample(byte[] buffer, int offset, AudioFormat format) {
        return buffer[offset + 1] << 11 | buffer[offset] & 0xFF;
    }


    private static int getZeroCrossing(double sample, byte[] buffer, int offset, int frameSize, AudioFormat format) {
        double prevSample = getSample(buffer, offset - frameSize, format);
        double slope = sample - prevSample;
        int sign = slope >= 0 ? 1 : -1;
        int zeroCrossing = -1;
        if (sign * prevSample <= 0 && sign * sample >= 0) {
            zeroCrossing = offset;
        } else if (Math.abs(slope) >= 0.05) {
            zeroCrossing = offset - (int) (sample * frameSize / slope);
        }
        return zeroCrossing;
    }

    private static int demodulateBit(int length, float sampleRate) {
        if (length >= ONE_LENGTH * sampleRate / 1000000) {
            return 1;
        } else {
            return 0;
        }
    }

    private static byte[] readMessage(AudioInputStream ais, int totalBytes) throws IOException {
        byte[] message = new byte[totalBytes];
        int bytesRead = ais.read(message);
        if (bytesRead != totalBytes) {
            return null;
        }
        int checksum = 0;
        for (int i = 0; i < message.length - CHECKSUM_LENGTH; i++) {
            checksum += message[i];
        }
        if ((checksum & 0xFF) != message[message.length - 1]) {
            return null;
        }
        return message;
    }

}
