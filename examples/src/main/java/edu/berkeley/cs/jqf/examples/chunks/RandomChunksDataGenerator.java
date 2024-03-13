package edu.berkeley.cs.jqf.examples.chunks;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;


/*
Script Testing

bin/jqf-zest -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.pngj.PngTest testPngPipeline
bin/jqf-jacoco-repro -c $(scripts/examples_classpath.sh) jacoco-output_baseline edu.berkeley.cs.jqf.examples.pngj.PngTest testPngPipeline fuzz-results/corpus/*
java -jar jacoco/lib/jacococli.jar report jacoco-output_baseline/jacoco.exec --html jacoco-output_baseline/report --classfiles examples/target/dependency/pngj-2.1.0.jar


POSSIBLE TESTS:
Signature + IEND: Heap error, around 700 coverage, then will slow down to 10 - 50 executions per second, ends in heap error from Java (maybe some way to enter a recursive endless loop?)
Signature + IHDR + IEND: Coverage but not valid
Signature + IHDR + IDAT + IEND: Partially valid Coverage

Found crash: class java.lang.OutOfMemoryError - Java heap space: failed reallocation of scalar replaced objects
*/



public class RandomChunksDataGenerator {

    int width = 0;
    int height = 0;

    boolean useValidIHDR;
    boolean useValidIDAT;

    /**
     * Generates PNG chunks filled with random content, adheres to valid checksums and chunk identifiers.
     * Ordering of chunks are partially adhered.
     * @param useValidIHDR If enabled, generates a valid IHDR chunk with true color and 8 bits per channel.
     * @param useValidIDAT If enabled, generates image data with the correct length and compresses it with DEFAULT_COMPRESSION. May include invalid filter methods, no filtering is taken into account.
     */
    public RandomChunksDataGenerator(boolean useValidIHDR, boolean useValidIDAT) {
        this.useValidIHDR = useValidIHDR;
        this.useValidIDAT = useValidIDAT;
    }

    String[] chunkNames = new String[]{
            "PLTE", "tRNS", "tEXt", "zTXt", "iTXt", "gAMA",
            "cHRM", "sRGB", "ICCP", "bGKD", "pHYs", "sBIT",
            "sPLT", "tIME", "hIST"
    };


    public byte[] generate(SourceOfRandomness randomness) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        width = 0;
        height = 0;

        try {
            bytes.write(generateSignature());

            if (useValidIHDR)
                bytes.write(generateIHDR(randomness));
            else
                bytes.write(generateRandomChunk("IHDR", randomness));

            for (int i = 0; i < randomness.nextInt(10); i++) {
                bytes.write(generateRandomChunk(chunkNames[randomness.nextInt(chunkNames.length)], randomness));
            }

            if(useValidIDAT)
                bytes.write(generateIDAT(randomness));
            else
                bytes.write(generateRandomChunk("IDAT", randomness));

            bytes.write(generateIEND());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes.toByteArray();

    }

    private byte[] generateRandomChunk(String chunkName, SourceOfRandomness randomness) {

        byte[] bytes = new byte[randomness.nextInt(32)];
        randomness.nextBytes(bytes);

        return ChunkBuilder.constructChunk(chunkName.getBytes(), bytes);

    }

    private byte[] generateSignature() {

        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private byte[] generateIHDR(SourceOfRandomness randomness){

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();

        byte[] imageWidth = intToByteArray(randomness.nextInt(10));
        byte[] imageHeight = intToByteArray(randomness.nextInt(10));

        ihdr.write(imageWidth, 0, 4);
        ihdr.write(imageHeight, 0, 4);
        ihdr.write(8);
        ihdr.write(2);
        ihdr.write(0x00);
        ihdr.write(0x00);
        ihdr.write(0);

        this.width = ByteBuffer.wrap(imageWidth).getInt();
        this.height = ByteBuffer.wrap(imageHeight).getInt();

        return ChunkBuilder.constructChunk("IHDR".getBytes(), ihdr);

    }

    private byte[] generateIDAT(SourceOfRandomness randomness) {

        byte[] filteredData = generateFilteredData(randomness);

        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        byte[] compressedData = ChunkBuilder.compressData(Deflater.DEFAULT_COMPRESSION, filteredData);
        idat.write(compressedData, 0, compressedData.length);

        return ChunkBuilder.constructChunk("IDAT".getBytes(), idat);
    }

    private byte[] generateFilteredData(SourceOfRandomness randomness) {

        byte[] data = new byte[(width * 3 + 1) * height];
        randomness.nextBytes(data);

        return data;

    }

    private byte[] generateIEND() {

        return new byte[]{0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
                0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82};

    }

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }


}