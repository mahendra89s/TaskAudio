package solution;



import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


public class AfskDecoder {

    private final StringBuilder messageBuilder = new StringBuilder();

    public static void main(String[] args) {
        String filename = "//Users//webwerks//IdeaProjects//Audio Task//src//file_2.wav";
        AfskDecoder decoder = new AfskDecoder();
        decoder.decode(filename);
    }

    public void decode(String filename) {
        /*try {
            /// Load the audio file.
            TarsosDSPAudioInputStream audioInputStream = new UniversalAudioInputStream(new File(filename), new TarsosDSPAudioFormat(44100, 16, 1, true, false));

            // Extract the audio samples.
            float[] samples = new float[audioInputStream.getFrameLength()];
            audioInputStream.read(samples, 0, samples.length);


            // Find the zero-crossings.
            ZeroCrossingDetector zcDetector = new ZeroCrossingDetector();
            List<Integer> crossings = zcDetector.detect(samples);

            // Detect the bits.
            BitDetector bitDetector = new BitDetector();
            List<Integer> bits = bitDetector.detect(crossings);

            // Detect the bytes.
            ByteDetector byteDetector = new ByteDetector();
            List<Byte> bytes = byteDetector.detect(convertToBytes(bits));

            // Decode the message.
            MessageDecoder messageDecoder = new MessageDecoder();
            String message = messageDecoder.decode(convertToBytes2(bytes));

            // Output the decoded message.
            System.out.println("Decoded message: " + message);

        } catch (IOException e) {
            // Output any errors.
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }*/
    }

    public byte[] convertToBytes(List<Integer> list) throws IOException {
        // write to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (int element : list) {
            out.writeUTF(Integer.toString(element));
        }
        return baos.toByteArray();
    }
    public byte[] convertToBytes2(List<Byte> list) throws IOException {
        // write to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        for (byte element : list) {
            out.writeUTF(Byte.toString(element));
        }
        return baos.toByteArray();
    }

   /* private void info(String msg) {
        infoMessages.add(msg);
    }

    private void error(String msg) {
        errorMessages.add(msg);
    }

    private void message(String msg) {
        messageBuilder.append(msg);
    }

    private void raw(byte[] data) {
        for (byte b : data) {
            rawBytes.add(b);
        }
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }*/

    public String getMessage() {
        return messageBuilder.toString();
    }

    /*public byte[] getRawBytes() {
        return Bytes.toArray(rawBytes);
    }*/




    public static class ZeroCrossingDetector {

        public List<Integer> detect(byte[] samples) {
            List<Integer> crossings = new ArrayList<>();
            for (int i = 1; i < samples.length; i++) {
                if ((samples[i] >= 0 && samples[i - 1] < 0) || (samples[i] < 0 && samples[i - 1] >= 0)) {
                    crossings.add(i);
                }
            }
            return crossings;
        }

    }


    public static class BitDetector {

        private int threshold = 1000;

        public List<Integer> detect(List<Integer> crossings) {
            List<Integer> bits = new ArrayList<>();

            for (int i = 1; i < crossings.size(); i++) {
                int period = crossings.get(i) - crossings.get(i - 1);

                if (period > threshold) {
                    bits.add(1);
                } else {
                    bits.add(0);
                }
            }

            return bits;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }
    }


    public static class ByteDetector {

        private int byteStartIndex = 0;
        private List<Byte> bytes = new ArrayList<>();

        public List<Byte> detect(byte[] bits) {
            for (int i = 0; i < bits.length; i++) {
                if (i >= byteStartIndex + 8) {
                    byteStartIndex += 8;
                    bytes.add(extractByte(bits, byteStartIndex));
                }

                if (i >= byteStartIndex && i < byteStartIndex + 8) {
                    continue;
                }

                if (bits[i] == 1) {
                    byteStartIndex = i;
                }
            }

            return bytes;
        }

        private byte extractByte(byte[] bits, int startIndex) {
            byte result = 0;

            for (int i = 0; i < 8; i++) {
                if (bits[startIndex + i] == 1) {
                    result |= (1 << i);
                }
            }

            return result;
        }
    }

    public static class MessageDecoder {

        public String decode(byte[] bytes) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                char c = (char) b;
                sb.append(c);
            }

            return sb.toString();
        }

    }




}
