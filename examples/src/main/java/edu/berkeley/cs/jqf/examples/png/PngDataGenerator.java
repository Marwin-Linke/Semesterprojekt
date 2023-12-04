package edu.berkeley.cs.jqf.examples.png;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class PngDataGenerator{

    // IHDR values
    private byte[] imageWidth = new byte[4];
    private byte[] imageHeight = new byte[4];
    private byte bitsPerChannel;
    private byte colorType;
    private byte compressionMethod;
    private byte filterMethod;
    private byte interlace;

    // image data values
    private int width;
    private int height;
    private int channels;

    // debugging
    private boolean debugging;

    public PngDataGenerator(boolean debugging){
        this.debugging = debugging;
    }

    public byte[] generate(SourceOfRandomness randomness) {

        ByteArrayOutputStream png = new ByteArrayOutputStream();

        try {

            png.write(generateSignature());
            png.write(generateIHDR(randomness));
            png.write(generateIDAT(randomness));
            png.write(generateIEND());

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return png.toByteArray();
    }

    private byte[] generateSignature(){

        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private byte[] generateIHDR(SourceOfRandomness randomness){

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();

        try {

            this.imageWidth = intToByteArray(randomness.nextInt(1, 100));
            this.imageHeight = intToByteArray(randomness.nextInt(1, 100));

            // for debug purposes
            //this.imageHeight = intToByteArray(2);
            //this.imageWidth = intToByteArray(2);

            this.bitsPerChannel = (byte) 0x08;
            if(randomness.nextBoolean()) {
                this.colorType = (byte) 0x00;
            } else {
                this.colorType = (byte) 0x02;
            }
            this.compressionMethod = (byte) 0x00;
            this.filterMethod = (byte) 0x00;
            // filter method 0x01 implemented but doesn't work
            this.interlace = (byte) 0x00;

            ihdr.write(imageWidth);
            ihdr.write(imageHeight);
            ihdr.write(bitsPerChannel);
            ihdr.write(colorType);
            ihdr.write(compressionMethod);
            ihdr.write(filterMethod);
            ihdr.write(interlace);

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return constructChunk("IHDR".getBytes(), ihdr);

    }

    private byte[] generateIDAT(SourceOfRandomness randomness){

        byte[] imageData = generateImageData(randomness);
        byte[] filteredData = addFilter(imageData);

        debugHex("image data", imageData);
        debugHex("filtered data", filteredData);

        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(filteredData);
        deflater.finish();

        byte[] compressedData = new byte[filteredData.length * 2];
        int compressedLength = deflater.deflate(compressedData);
        deflater.end();

        idat.write(compressedData, 0, compressedLength);

        return constructChunk("IDAT".getBytes(), idat);
    }

    private byte[] generateImageData(SourceOfRandomness randomness){

        switch (colorType) {
            case 0x00 :
                channels = 1;
                break;
            case 0x02 :
                channels = 3;
                break;
            default:
                channels = 1;
                break;
        }

        width = ByteBuffer.wrap(imageWidth).getInt();
        height = ByteBuffer.wrap(imageHeight).getInt();
        int scanline = width * channels;

        byte[] imageData = new byte[height * scanline];

        for (int y = 0; y < height; y++) {

            //imageData[y * scanline] = 0x00; //filter byte

            for (int x = 0; x < width; x++) {

                int index = y * scanline + x * channels;

                for (int i = 0; i < channels; i++) { // iterates through channels

                    // randomizes each channel
                    byte channel = (byte) (randomness.nextInt((int) Math.pow(2, 8)));
                    imageData[index + i] = channel;

                }

            }
        }


        return imageData;

    }

    private byte[] addFilter(byte[] imageData){
        switch (filterMethod) {
            case 0:
                return noFilter(imageData);
            case 1:
                return subFilter(imageData);
            default:
                return noFilter(imageData);
        }
    }

    private byte[] noFilter(byte[] imageData){

        ByteArrayOutputStream filteredData = new ByteArrayOutputStream();

        int scanline = width * channels;

        for (int y = 0; y < height; y++) {

            filteredData.write(0x00);

            for (int x = 0; x < scanline; x++) {

                filteredData.write(imageData[y * scanline + x]);

            }
        }

        return filteredData.toByteArray();
    }

    private byte[] subFilter(byte[] imageData) { // seems right, doesn't work

        ByteArrayOutputStream filteredData = new ByteArrayOutputStream();

        int scanline = width * channels;

        for (int y = 0; y < height; y++) {

            filteredData.write(0x01);

            for (int x = 0; x < scanline; x++) {

                int position = y * scanline + x;
                if(x < channels) {
                    filteredData.write(imageData[position]);
                }
                else {
                    int sub = imageData[position] - imageData[position - channels];
                    int mod = Integer.remainderUnsigned(sub, 256);
                    filteredData.write(mod);
                }

            }
        }

        return filteredData.toByteArray();

    }

    private byte[] generateIEND(){

        return new byte[]{0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
                0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82};

    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02X ", b));
        return sb.toString();
    }

    public void debugHex(String name, byte[] bytes) {
        if(debugging)
            System.out.println(name + ": " + byteArrayToHex(bytes));
    }

    private static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private static byte[] calculateCRC(byte[] bytes){
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return intToByteArray((int)crc.getValue());
    }

    private static byte[] calculateCRC(byte[] bytes, int offset, int length){
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return intToByteArray((int)crc.getValue());
    }

    private static byte[] calculateCRC(ByteArrayOutputStream byteStream){
        return calculateCRC(byteStream.toByteArray());
    }

    private static byte[] calculateCRC(ByteArrayOutputStream byteStream, int offset, int length){
        return calculateCRC(byteStream.toByteArray(), offset, length);
    }

    private byte[] constructChunk(byte[] chunkType, ByteArrayOutputStream chunkContent) {

        ByteArrayOutputStream chunk = new ByteArrayOutputStream();

        try {

            chunk.write(intToByteArray(chunkContent.size()));
            chunk.write(chunkType);
            chunk.write(chunkContent.toByteArray());
            chunk.write(calculateCRC(chunk, 4, chunk.size() - 4));
            // calculates CRC without the length

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return chunk.toByteArray();

    }

    public static void main(String[] args) {
        PngDataGenerator gen = new PngDataGenerator(true);
        SourceOfRandomness randomness = new SourceOfRandomness(new Random());
        byte[] ihdr = gen.generateIHDR(randomness);
        byte[] idat = gen.generateIDAT(randomness);
        gen.debugHex("IHDR", ihdr);
        gen.debugHex("IDAT", idat);

        byte[] png = gen.generate(randomness);
        gen.debugHex("Png", png);

        try {

            FileOutputStream fos = new FileOutputStream("Debugging_Png.png");
            fos.write(png);
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}