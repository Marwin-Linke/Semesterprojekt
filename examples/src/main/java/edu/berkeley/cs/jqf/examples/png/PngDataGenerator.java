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
    private byte interlace;

    // image data values
    private int width;
    private int height;
    private int channels;
    private int scanline;
    private boolean paletteUsed;

    // debugging
    private boolean debugging;

    public PngDataGenerator(boolean debugging){
        this.debugging = debugging;
    }

    public byte[] generate(SourceOfRandomness randomness) {

        ByteArrayOutputStream png = new ByteArrayOutputStream();

        try {

            initializeParameters(randomness);

            png.write(generateSignature());
            png.write(generateIHDR(randomness));
            if(paletteUsed) {
                png.write(generatePLTE(randomness));
            }
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

    private void initializeParameters(SourceOfRandomness randomness) {

        this.imageWidth = intToByteArray(randomness.nextInt(1, 100));
        this.imageHeight = intToByteArray(randomness.nextInt(1, 100));

        this.interlace = (byte) 0x00;

        initializeRandomColoring(randomness);

        // DEBUGGING AREA
        /*
        this.imageHeight = intToByteArray(2);
        this.imageWidth = intToByteArray(2);
        initializeRandomColoring(randomness, 2, 8);
        */
        // END OF DEBUGGING AREA

        this.width = ByteBuffer.wrap(imageWidth).getInt();
        this.height = ByteBuffer.wrap(imageHeight).getInt();

    }

    private void initializeRandomColoring(SourceOfRandomness randomness, int colorMethod, int bitDepth) {

        if (colorMethod == -1)
            colorMethod = randomness.nextInt(5);


        switch (colorMethod) {
            case 0: // grayscale
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(5)));
                this.colorType = 0x00;
                this.channels = 1;
                break;
            case 1: // grayscale with alpha
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(3,4)));
                this.colorType = 0x04;
                this.channels = 2;
                break;
            case 2: // true color
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(3,4)));
                this.colorType = 0x02;
                this.channels = 3;
                if(randomness.nextBoolean())
                    paletteUsed = true;
                break;
            case 3: // true color with alpha
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(3,4)));
                this.colorType = 0x06;
                this.channels = 4;
                if(randomness.nextBoolean())
                    paletteUsed = true;
                break;
            case 4: // indexed color, palette used
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(4)));
                this.colorType = 0x03;
                this.channels = 1;
                this.paletteUsed = true;
                break;

        }

        if (bitDepth != -1)
            this.bitsPerChannel = (byte) bitDepth;
    }

    private void initializeRandomColoring (SourceOfRandomness randomness) {
        initializeRandomColoring(randomness, -1, -1);
    }

    private byte[] generateIHDR(SourceOfRandomness randomness){

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();

        try {

            // writes options into the IHDR chunk
            ihdr.write(imageWidth);
            ihdr.write(imageHeight);
            ihdr.write(bitsPerChannel);
            ihdr.write(colorType);
            // compression method is fixed at 0x00
            ihdr.write(0x00);
            // filter methods are always 0 in the IHDR, the difference comes in the image data!
            ihdr.write(0x00);
            ihdr.write(interlace);

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        byte[] ihdrBytes = constructChunk("IHDR".getBytes(), ihdr);

        debugHex("IHDR", ihdrBytes);

        return ihdrBytes;

    }

    private byte[] generatePLTE(SourceOfRandomness randomness) {

        ByteArrayOutputStream plte = new ByteArrayOutputStream();

        for(int i = 0; i < 256; i++) {

            plte.write((byte) (randomness.nextInt((int) Math.pow(2, 8)))); // red
            plte.write((byte) (randomness.nextInt((int) Math.pow(2, 8)))); // green
            plte.write((byte) (randomness.nextInt((int) Math.pow(2, 8)))); // blue
        }

        return constructChunk("PLTE".getBytes(), plte);

    }

    private byte[] generateIDAT(SourceOfRandomness randomness) {

        byte[] filteredData = generateFilteredData(randomness);

        debugHex("filtered data", filteredData);

        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        int compressionMethod = randomness.nextInt(-1,9);

        Deflater deflater = new Deflater(compressionMethod);
        deflater.setInput(filteredData);
        deflater.finish();

        byte[] compressedData = new byte[filteredData.length * 100 + 10];
        int compressedLength = deflater.deflate(compressedData);
        deflater.end();

        idat.write(compressedData, 0, compressedLength);

        byte[] idatBytes = constructChunk("IDAT".getBytes(), idat);

        debugHex("IDAT", idatBytes);

        return idatBytes;
    }

    private byte[] generateFilteredData(SourceOfRandomness randomness){

        // the scanline indicates the length of one horizontal line in the image (filter byte included)

        float channelSize = (float) bitsPerChannel / 8;
        scanline = (int) Math.ceil(width * channels * channelSize) + 1;
        byte[] imageData = new byte[height * scanline];

        for (int y = 0; y < height; y++) {

            // each line can opt for a different filter method
            int filterMethod = (byte) randomness.nextInt(5);

            //filterMethod = 0;
            // the first byte of each scanline defines the filter method
            imageData[y * scanline] = (byte) filterMethod;

            for (int x = 1; x < scanline; x++) {

                // the position of each byte in the image data
                int position = y * scanline + x;

                // each byte is randomized, based on the channelSize (bit-depth)
                // ... multiple channels or pixel can be in one byte
                byte imageByte = (byte) (randomness.nextInt((int) Math.pow(2, 8)));
                imageData[position] = imageByte;

                // the filter is added onto each byte based on the filter method
                byte filteredImageByte = addFilter(filterMethod, imageData, position);
                imageData[position] = filteredImageByte;
            }
        }

        return imageData;

    }


    private byte addFilter(int filterMethod, byte[] imageData, int position){
        switch (filterMethod) {
            case 1:
                return subFilter(imageData, position);
            case 2:
                return upFilter(imageData, position);
            case 3:
                return averageFilter(imageData, position);
            case 4:
                return paethFilter(imageData, position);
            default:
                return imageData[position];
        }
    }

    private byte subFilter(byte[] imageData, int position) {

        byte filteredImageByte;

        // first pixel of each scanline is ignored
        if(position % scanline < channels) {
            filteredImageByte = imageData[position];
        }
        else {
            int sub = imageData[position] - imageData[position - channels];
            int mod = Integer.remainderUnsigned(sub, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private byte upFilter(byte[] imageData, int position) {

        byte filteredImageByte;

        // first scanline is ignored
        if(position < scanline) {
            filteredImageByte =  imageData[position];
        }
        else {
            int sub = imageData[position] - imageData[position - scanline];
            int mod = Integer.remainderUnsigned(sub, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private byte averageFilter(byte[] imageData, int position) {

        byte filteredImageByte;

        // first pixel of each scanline and the first scanline itself are ignored
        if(position < scanline || position % scanline < channels) {
            filteredImageByte = imageData[position];
        }
        else {
            int left = imageData[position - channels];
            int up = imageData[position - scanline];
            int subAverage = imageData[position] - (left + up) / 2;
            int mod = Integer.remainderUnsigned(subAverage, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private byte paethFilter(byte[] imageData, int position) {

        byte filteredImageByte;

        // first pixel of each scanline and the first scanline itself are ignored
        if(position < scanline || position % scanline < channels) {
            filteredImageByte = imageData[position];
        }
        else {
            int left = imageData[position - channels];
            int above = imageData[position - scanline];
            int upperLeft = imageData[position - scanline - channels];
            int subPaeth = imageData[position] - PaethPredictor(left, above, upperLeft);
            int mod = Integer.remainderUnsigned(subPaeth, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private int PaethPredictor(int left, int above, int upperLeft) {

        // paeth predictor is an algorithm used for the paeth filter

        int p = left + above - upperLeft;
        int pLeft = Math.abs(p - left);
        int pAbove = Math.abs(p - above);
        int pUpperLeft = Math.abs(p - upperLeft);
        if(pLeft <= pAbove && pLeft <= pUpperLeft) {
            return left;
        }
        else if(pAbove <= pUpperLeft) {
            return above;
        }
        return upperLeft;
    }

    /*
    PSEUDO-CODE by libpng

    function PaethPredictor (a, b, c)
       begin
            ; a = left, b = above, c = upper left
            p := a + b - c        ; initial estimate
            pa := abs(p - a)      ; distances to a, b, c
            pb := abs(p - b)
            pc := abs(p - c)
            ; return nearest of a,b,c,
            ; breaking ties in order a,b,c.
            if pa <= pb AND pa <= pc then return a
            else if pb <= pc then return b
            else return c
       end
     */

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