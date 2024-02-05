package edu.berkeley.cs.jqf.examples.png;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;


/*
Script Testing

bin/jqf-zest -f -c $(scripts/examples_classpath.sh) edu.berkeley.cs.jqf.examples.pngj.PngTest testPngPipeline
bin/jqf-jacoco-repro -c $(scripts/examples_classpath.sh) jacoco-output5 edu.berkeley.cs.jqf.examples.pngj.PngTest testPngPipeline fuzz-results/corpus/*
java -jar jacoco/lib/jacococli.jar report jacoco-output5/jacoco.exec --html jacoco-output5/report --classfiles examples/target/dependency/pngj-2.1.0.jar

 */

public class PngDataGenerator{

    // IHDR values
    private byte[] imageWidth = new byte[4];
    private byte[] imageHeight = new byte[4];
    private byte bitsPerChannel, colorType, interlace;

    // image data values
    private int width, height, channels, scanline;

    // chunks
    private boolean PLTEUsed, tRNSUsed;
    private int transparencyMethod;
    private boolean tEXtUsed, zTXtUsed, iTXtUsed;
    private boolean gAMAUsed, cHRMUsed, sRGBUsed, iCCPUsed;
    private boolean bGKDUsed, pHYsUsed, sBITUsed;
    private boolean sPLTUsed, tIMEUsed, hISTUsed;
    private int backgroundMethod;
    private int PLTEEntries;

    // debugging
    private final boolean debugging;
    private final boolean fullRange;
    /**
     * BASELINE CRITERIA:
     * The baseline includes:
     * - All critical chunks: IHDR, IDAT, PLTE, IEND
     * - All color types
     * - Only bit-depth of 8
     * - Only default compression
     * - no interlacing
     * - no filtering
     */
    private final boolean baseline;



    public PngDataGenerator(boolean debugging, boolean fullRange, boolean baseline){
        this.debugging = debugging;
        this.fullRange = fullRange;
        this.baseline = baseline;
    }

    public byte[] generate(SourceOfRandomness randomness) {

        ByteArrayOutputStream png = new ByteArrayOutputStream();

        resetParameters();

        try {

            initializeParameters(randomness);

            png.write(generateSignature());
            png.write(generateIHDR(randomness));
            if(sBITUsed)
                png.write(generateSBIT(randomness));
            if(sRGBUsed)
                png.write(generateSRGB(randomness));
            if(gAMAUsed)
                png.write(generateGAMA(randomness));
            if(cHRMUsed)
                png.write(generateCHRM(randomness));
            if(iCCPUsed)
                png.write(generateICCP(randomness));
            if(PLTEUsed)
                png.write(generatePLTE(randomness));
            if(tRNSUsed)
                png.write(generateTRNS(randomness));
            if(bGKDUsed)
                png.write(generateBKGD(randomness));
            if(pHYsUsed)
                png.write(generatePHYS(randomness));
            if(hISTUsed)
                png.write(generateHIST(randomness));
            if(tIMEUsed)
                png.write(generateTIME(randomness));
            if(sPLTUsed)
                for(int i = 0; i < generateRandomInt(1, 3, randomness); i++) {
                    png.write(generateSPLT(randomness));
                }
            if(tEXtUsed)
                for(int i = 0; i < generateRandomInt(1, 3, randomness); i++) {
                    png.write(generateTEXT(randomness));
                }
            if(zTXtUsed)
                for(int i = 0; i < generateRandomInt(1, 3, randomness); i++) {
                    png.write(generateZTXT(randomness));
                }

            if(interlace == 0x01)
                png.write(generateInterlacedIDATChunks(randomness));
            else
                png.write(generateIDAT(randomness));

            if(iTXtUsed)
                for(int i = 0; i < generateRandomInt(1, 3, randomness); i++) {
                    png.write(generateITXT(randomness));
                }
            png.write(generateIEND());

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return png.toByteArray();
    }

    private void resetParameters() {
        imageWidth = new byte[4];
        imageHeight = new byte[4];
        bitsPerChannel = 0;
        colorType = 0;
        interlace = 0;

        width = 0;
        height = 0;
        channels = 0;
        scanline = 0;

        PLTEUsed = false;
        tRNSUsed = false;
        transparencyMethod = 0;
        tEXtUsed = false;
        zTXtUsed = false;
        iTXtUsed = false;
        gAMAUsed = false;
        cHRMUsed = false;
        sRGBUsed = false;
        bGKDUsed = false;
        backgroundMethod = 0;
        pHYsUsed = false;
        sBITUsed = false;
        sPLTUsed = false;
        tIMEUsed = false;
        hISTUsed = false;
        iCCPUsed = false;
    }

    private byte[] generateSignature(){

        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private void initializeParameters(SourceOfRandomness randomness) {

        this.imageWidth = intToByteArray(generateRandomInt(1, 10, randomness));
        this.imageHeight = intToByteArray(generateRandomInt(1, 10, randomness));

        if(baseline) {
            initializeBaselineColoring(randomness);
        }
        else {
            initializeRandomColoring(randomness);

            this.interlace = randomness.nextBoolean() ? (byte) 1 : (byte) 0;

            this.tEXtUsed = randomness.nextBoolean();
            this.zTXtUsed = randomness.nextBoolean();
            this.iTXtUsed = randomness.nextBoolean();
            this.gAMAUsed = randomness.nextBoolean();
            this.cHRMUsed = randomness.nextBoolean();
            this.sRGBUsed = randomness.nextBoolean();
            if(sRGBUsed) {
                this.gAMAUsed = true;
                this.cHRMUsed = true;
            }
            this.iCCPUsed = randomness.nextBoolean();
            this.bGKDUsed = randomness.nextBoolean();
            this.pHYsUsed = randomness.nextBoolean();
            this.sBITUsed = randomness.nextBoolean();
            this.sPLTUsed = randomness.nextBoolean();
            if(colorType == 0x03)
                this.hISTUsed = randomness.nextBoolean();
            this.tIMEUsed = randomness.nextBoolean();
        }


        // DEBUGGING AREA
        /*
        this.initializeRandomColoring(randomness, 4, 1);
        this.tEXtUsed = false;
        this.zTXtUsed = false;
        this.iTXtUsed = false;
        this.gAMAUsed = false;
        this.cHRMUsed = false;
        this.sRGBUsed = false;
        this.PLTEUsed = true;
        this.tRNSUsed = false;
        this.imageHeight = intToByteArray(10);
        this.imageWidth = intToByteArray(10);
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
                if(randomness.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 1;
                }
                this.backgroundMethod = 1;
                break;
            case 1: // grayscale with alpha
                this.bitsPerChannel = randomness.nextBoolean() ? (byte) 8 : (byte) 16;
                this.colorType = 0x04;
                this.channels = 2;
                this.backgroundMethod = 1;
                break;
            case 2: // true color
                this.bitsPerChannel = randomness.nextBoolean() ? (byte) 8 : (byte) 16;
                this.colorType = 0x02;
                this.channels = 3;
                if(randomness.nextBoolean())
                    PLTEUsed = true;
                if(randomness.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 2;
                }
                this.backgroundMethod = 2;
                break;
            case 3: // true color with alpha
                this.bitsPerChannel = randomness.nextBoolean() ? (byte) 8 : (byte) 16;
                this.colorType = 0x06;
                this.channels = 4;
                if(randomness.nextBoolean())
                    PLTEUsed = true;
                this.backgroundMethod = 2;
                break;
            case 4: // indexed color, palette used
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(4)));
                this.colorType = 0x03;
                this.channels = 1;
                this.PLTEUsed = true;
                if(randomness.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 0;
                }
                this.backgroundMethod = 0;
                break;

        }

        if (bitDepth != -1)
            this.bitsPerChannel = (byte) bitDepth;
    }

    private void initializeBaselineColoring(SourceOfRandomness randomness) {

        int colorMethod = randomness.nextInt(5);

        switch (colorMethod) {
            case 0: // grayscale
                this.colorType = 0x00;
                this.channels = 1;
                break;
            case 1: // grayscale with alpha
                this.colorType = 0x04;
                this.channels = 2;
                break;
            case 2: // true color
                this.colorType = 0x02;
                this.channels = 3;
                break;
            case 3: // true color with alpha
                this.colorType = 0x06;
                this.channels = 4;
                break;
            case 4: // indexed color, palette used
                this.colorType = 0x03;
                this.channels = 1;
                this.PLTEUsed = true;
                break;

        }

        this.bitsPerChannel = (byte) 8;
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

        byte[] ihdrBytes = ChunkBuilder.constructChunk("IHDR".getBytes(), ihdr);

        debugHex("IHDR", ihdrBytes);

        return ihdrBytes;

    }

    private byte[] generateSBIT(SourceOfRandomness randomness) {

        ByteArrayOutputStream sBit = new ByteArrayOutputStream();

        switch (colorType) {
            case 0:
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                break;
            case 2:
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                break;
            case 3:
                sBit.write(generateRandomInt(1, 8, randomness));
                sBit.write(generateRandomInt(1, 8, randomness));
                sBit.write(generateRandomInt(1, 8, randomness));
                break;
            case 4:
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                break;
            case 6:
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                sBit.write(generateRandomInt(1, bitsPerChannel, randomness));
                break;
        }

        return ChunkBuilder.constructChunk("sBIT".getBytes(), sBit);
    }

    private byte[] generateGAMA(SourceOfRandomness randomness) {

        if(sRGBUsed) { // sRGB uses a fixed gAMA value
            return ChunkBuilder.constructChunk("gAMA".getBytes(), intToByteArray(45455));
        }

        byte[] gAMA = intToByteArray(generateRandomInt(0, 100000, randomness));
        return ChunkBuilder.constructChunk("gAMA".getBytes(), gAMA);
    }

    private byte[] generateCHRM(SourceOfRandomness randomness) {

        ByteArrayOutputStream cHRM = new ByteArrayOutputStream();

        try {

            if(sRGBUsed) { // sRGB uses fixed cHRM values
                cHRM.write(intToByteArray(31270)); // white point x
                cHRM.write(intToByteArray(32900)); // white point y
                cHRM.write(intToByteArray(64000)); // red x
                cHRM.write(intToByteArray(33000)); // red y
                cHRM.write(intToByteArray(30000)); // green x
                cHRM.write(intToByteArray(60000)); // green y
                cHRM.write(intToByteArray(15000)); // blue x
                cHRM.write(intToByteArray(6000)); // blue y
            }
            else {
                for(int i = 0; i < 8; i++) {
                    cHRM.write(intToByteArray(generateRandomInt(0, 100000, randomness))); // randomized values
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return ChunkBuilder.constructChunk("cHRM".getBytes(), cHRM);
    }

    private byte[] generateSRGB(SourceOfRandomness randomness) {

        byte[] sRGB = new byte[]{(byte) randomness.nextInt(4)};
        return ChunkBuilder.constructChunk("sRGB".getBytes(), sRGB);

    }

    private byte[] generateICCP(SourceOfRandomness randomness) {

        ByteArrayOutputStream iCCP = new ByteArrayOutputStream();
        ByteArrayOutputStream iCCP_text = new ByteArrayOutputStream();

        // Keyword
        appendRandomString(iCCP, 1, 79, randomness);
        // Null separator
        iCCP.write(0x00);
        // Compression method
        iCCP.write(0x00);
        // Text
        for(int i = 0; i < generateRandomInt(1, 256, randomness); i++) { // 256 (could be maximum chunk size - 80)
            int j = generateRandomInt(1, 100, randomness);
            if(j < 50) {
                iCCP_text.write((byte) generateRandomInt(32, 126, randomness));
            } else if(j < 99) {
                iCCP_text.write((byte) generateRandomInt(161, 255, randomness));
            } else {
                iCCP_text.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }

        int compressionMethod = 0; // 0 is the only defined compression method

        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, iCCP_text.toByteArray());
        iCCP.write(compressedData, 0, compressedData.length);

        return ChunkBuilder.constructChunk("iCCP".getBytes(), iCCP);
    }


    private byte[] generatePLTE(SourceOfRandomness randomness) {

        ByteArrayOutputStream PLTE = new ByteArrayOutputStream();

        PLTEEntries = (int) Math.pow(2, bitsPerChannel);

        for(int i = 0; i < PLTEEntries; i++) {

            PLTE.write((byte) (generateRandomIntBase2(8, randomness))); // red
            PLTE.write((byte) (generateRandomIntBase2(8, randomness))); // green
            PLTE.write((byte) (generateRandomIntBase2(8, randomness))); // blue
        }

        byte[] PLTEChunk = ChunkBuilder.constructChunk("PLTE".getBytes(), PLTE);

        debugHex("PLTE", PLTEChunk);

        return PLTEChunk;

    }

    private byte[] generateTRNS(SourceOfRandomness randomness) {

        ByteArrayOutputStream tRNS = new ByteArrayOutputStream();

        // dependent on the bit-depth, all irrelevant zeros shall be 0
        // the channel size stays 2 bytes for case 1 and 2
        try {
            switch (transparencyMethod) {
                case 0: // for indexed colors, artificial alpha palette
                    for (int i = 0; i < 256; i++) {
                        tRNS.write((byte) (generateRandomIntBase2(8, randomness)));
                    }
                    break;
                case 1: // for grayscale, single alpha value for specified bytes
                    tRNS.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness)));
                    break;
                case 2: // for true color, single alpha value for specified bytes
                    tRNS.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness))); // red
                    tRNS.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness))); // green
                    tRNS.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness))); // blue
                    break;
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("tRNS".getBytes(), tRNS);

    }

    private byte[] generateBKGD(SourceOfRandomness randomness) {

        ByteArrayOutputStream bKGD = new ByteArrayOutputStream();

        try {
            switch (backgroundMethod) {
                case 0: // for indexed colors
                    bKGD.write((byte) (generateRandomIntBase2(8, randomness)));
                    break;
                case 1: // for grayscale
                    bKGD.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness)));
                    break;
                case 2: // for true color
                    bKGD.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness))); // red
                    bKGD.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness))); // green
                    bKGD.write(int2ToByteArray(generateRandomIntBase2(bitsPerChannel, randomness))); // blue
                    break;
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("bKGD".getBytes(), bKGD);

    }

    private byte[] generatePHYS(SourceOfRandomness randomness) {

        ByteArrayOutputStream pHYs = new ByteArrayOutputStream();

        // specifies the aspect ratio between x and y in pixels
        try {
            pHYs.write(intToByteArray(generateRandomInt(0, 99, randomness))); // x
            pHYs.write(intToByteArray(generateRandomInt(0, 99, randomness))); // y
            pHYs.write(generateRandomInt(0, 1, randomness)); // unknown unit or meter
        }
        catch (IOException e) { e.printStackTrace(); }

        debugHex("pHYs", pHYs.toByteArray());

        return ChunkBuilder.constructChunk("pHYs".getBytes(), pHYs);

    }

    private byte[] generateSPLT(SourceOfRandomness randomness) {

        ByteArrayOutputStream sPLT = new ByteArrayOutputStream();

        try {
            // Keyword
            appendRandomString(sPLT, 1, 79, randomness);
            // Null separator
            sPLT.write(0x00);

            sPLT.write(bitsPerChannel);

            for (int i = 0; i < generateRandomInt(0, 10, randomness); i++) {

                if (bitsPerChannel == 16) {
                    sPLT.write(int2ToByteArray(generateRandomIntBase2(16, randomness))); // red
                    sPLT.write(int2ToByteArray(generateRandomIntBase2(16, randomness))); // green
                    sPLT.write(int2ToByteArray(generateRandomIntBase2(16, randomness))); // blue
                }
                else {
                    sPLT.write((byte) (generateRandomIntBase2(8, randomness))); // green
                    sPLT.write((byte) (generateRandomIntBase2(8, randomness))); // red
                    sPLT.write((byte) (generateRandomIntBase2(8, randomness))); // blue
                }

                sPLT.write(int2ToByteArray(generateRandomIntBase2(16, randomness))); // frequency

            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("sPLT".getBytes(), sPLT);

    }

    private byte[] generateHIST(SourceOfRandomness randomness) {

        ByteArrayOutputStream hIST = new ByteArrayOutputStream();

        try {
            for(int i = 0; i < PLTEEntries; i++) {
                hIST.write(int2ToByteArray(generateRandomIntBase2(16, randomness)));
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("hIST".getBytes(), hIST);

    }

    private byte[] generateTIME(SourceOfRandomness randomness) {

        ByteArrayOutputStream tIME = new ByteArrayOutputStream();

        try {
            tIME.write(int2ToByteArray(generateRandomIntBase2(16, randomness))); // years
            tIME.write((byte) generateRandomInt(1, 12, randomness)); // months
            tIME.write((byte) generateRandomInt(1, 31, randomness)); // days
            tIME.write((byte) generateRandomInt(0, 23, randomness)); // hours
            tIME.write((byte) generateRandomInt(0, 59, randomness)); // minutes
            tIME.write((byte) generateRandomInt(0, 60, randomness)); // seconds (plus leap second)
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("tIME".getBytes(), tIME);

    }

    private byte[] generateTEXT(SourceOfRandomness randomness) {

        ByteArrayOutputStream tEXt = new ByteArrayOutputStream();

        // Keyword
        appendRandomString(tEXt, 1, 79, randomness);

        // Null separator
        tEXt.write(0x00);
        // Text
        for(int i = 0; i < generateRandomInt(1, 256, randomness); i++) { // 256 (could be maximum chunk size - 80)
            int j = generateRandomInt(1, 100, randomness);
            if(j < 50) {
                tEXt.write((byte) generateRandomInt(32, 126, randomness));
            } else if(j < 99) {
                tEXt.write((byte) generateRandomInt(161, 255, randomness));
            } else {
                tEXt.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }

        byte[] tEXtBytes = ChunkBuilder.constructChunk("tEXt".getBytes(), tEXt);

        debugHex("tEXt", tEXtBytes);
        
        return tEXtBytes;
    }

    private byte[] generateZTXT(SourceOfRandomness randomness) {

        ByteArrayOutputStream zTXt = new ByteArrayOutputStream();
        ByteArrayOutputStream zTXt_text = new ByteArrayOutputStream();

        // Keyword
        appendRandomString(zTXt, 1, 79, randomness);
        // Null separator
        zTXt.write(0x00);
        // Compression method
        zTXt.write(0x00);
        // Text
        for(int i = 0; i < generateRandomInt(1, 256, randomness); i++) { // 256 (could be maximum chunk size - 80)
            int j = generateRandomInt(1, 100, randomness);
            if(j < 50) {
                zTXt_text.write((byte) generateRandomInt(32, 126, randomness));
            } else if(j < 99) {
                zTXt_text.write((byte) generateRandomInt(161, 255, randomness));
            } else {
                zTXt_text.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }

        int compressionMethod = 0; // 0 is the only defined compression method

        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, zTXt_text.toByteArray());
        zTXt.write(compressedData, 0, compressedData.length);

        byte[] zTXtBytes = ChunkBuilder.constructChunk("zTXt".getBytes(), zTXt);

        debugHex("zTXt", zTXtBytes);
        
        return zTXtBytes;
    }

    private byte[] generateITXT(SourceOfRandomness randomness) {

        ByteArrayOutputStream iTXt = new ByteArrayOutputStream();
        ByteArrayOutputStream iTXt_text = new ByteArrayOutputStream();

        try {
            // Keyword
            iTXt.write(create_utf8(randomness, generateRandomInt(1, 79, randomness)));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // Null separator
        iTXt.write(0x00);

        // Compression flag (0 for uncompressed, 1 for compressed)
        boolean isCompressed = randomness.nextBoolean();
        iTXt.write((byte) (isCompressed ? 1 : 0));

        // Compression method
        int compressionMethod = 0; // 0 is the only defined compression method
        iTXt.write(0x00);

        try{
            // Language tag
            String[] languages = {"cn", "en-uk", "no-bok", "x-klingon"}; // hardcoded
            if(randomness.nextBoolean()) {
                iTXt.write(languages[generateRandomInt(0, 3, randomness)].getBytes());
            }
            // Null separator
            iTXt.write(0x00);

            // Translated keyword
            iTXt.write(create_utf8(randomness, generateRandomInt(0, 100, randomness))); // this keyword can be longer than 79 bytes
            // Null separator
            iTXt.write(0x00);

            // Text
            iTXt_text.write(create_utf8(randomness, generateRandomInt(0, 256, randomness))); // 256 (could be maximum chunk size - other data bytes of this chunk)
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        if(isCompressed) {

            byte[] compressedData = ChunkBuilder.compressData(compressionMethod, iTXt_text.toByteArray());
            iTXt.write(compressedData, 0, compressedData.length);

        } else {
            try{
                iTXt.write(iTXt_text.toByteArray());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] iTXtBytes = ChunkBuilder.constructChunk("iTXt".getBytes(), iTXt);

        debugHex("iTXt", iTXtBytes);
        
        return iTXtBytes;
    }

    private byte[] generateInterlacedIDATChunks(SourceOfRandomness randomness) {

        ByteArrayOutputStream IDATChunks = new ByteArrayOutputStream();

        int originalWidth = width;
        int originalHeight = height;

        try{

            // Pass 1
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 8, 8, randomness
            ));

            // Pass 2
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 4, 8, randomness
            ));

            // Pass 3
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 4, 4, randomness
            ));

            // Pass 4
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 2, 4, randomness
            ));

            // Pass 5
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 2, 2, randomness
            ));

            // Pass 6
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 1, 2, randomness
            ));

            // Pass 7
            IDATChunks.write(generateInterlacedIDAT(
                    originalWidth, originalHeight, 1, 1, randomness
            ));
        }
        catch (IOException e) { e.printStackTrace(); }

        return IDATChunks.toByteArray();
    }

    private byte[] generateInterlacedIDAT(int originalWidth, int originalHeight, int widthDivisor, int heightDivisor, SourceOfRandomness randomness) {
        width = (int) Math.ceil((float)originalWidth / widthDivisor);
        height = (int) Math.ceil((float)originalHeight / heightDivisor);
        return generateIDAT(randomness);
    }

    private byte[] generateIDAT(SourceOfRandomness randomness) {

        byte[] filteredData = generateFilteredData(randomness);

        debugHex("filtered data", filteredData);

        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        int compressionMethod = baseline ? 0 : generateRandomInt(-1, 8, randomness);
        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, filteredData);
        idat.write(compressedData, 0, compressedData.length);

        byte[] idatChunk = ChunkBuilder.constructChunk("IDAT".getBytes(), idat);

        debugHex("IDAT", idatChunk);

        return idatChunk;
    }

    private byte[] generateFilteredData(SourceOfRandomness randomness){

        // the scanline indicates the length of one horizontal line in the image (filter byte included)

        float channelSize = (float) bitsPerChannel / 8;
        scanline = (int) Math.ceil(width * channels * channelSize) + 1;
        byte[] imageData = new byte[height * scanline];

        for (int y = 0; y < height; y++) {

            // each line can opt for a different filter method
            int filterMethod = baseline ? 0 : (byte) randomness.nextInt(5);

            //filterMethod = 0;
            // the first byte of each scanline defines the filter method
            imageData[y * scanline] = (byte) filterMethod;

            for (int x = 1; x < scanline; x++) {

                // the position of each byte in the image data
                int position = y * scanline + x;

                // each byte is randomized, based on the channelSize (bit-depth)
                // ... multiple channels or pixel can be in one byte
                byte imageByte = (byte) (generateRandomIntBase2(8, randomness));
                imageData[position] = imageByte;

                // the filter is added onto each byte based on the filter method
                byte filteredImageByte = Filter.addFilter(filterMethod, imageData, position, scanline, channels);
                imageData[position] = filteredImageByte;
            }
        }

        return imageData;

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

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] int2ToByteArray(int value) {
        return ByteBuffer.allocate(2).putShort((short) value).array();
    }

    public void appendRandomString(ByteArrayOutputStream stream, int min, int max, SourceOfRandomness randomness) {

        for(int i = 0; i < generateRandomInt(min, max, randomness); i++) {
            if(randomness.nextBoolean()) {
                stream.write((byte) generateRandomInt(32, 126, randomness));
            } else {
                stream.write((byte) generateRandomInt(161, 255, randomness));
            }
        }
    }

    public byte[] create_utf8(SourceOfRandomness randomness, int max_byte_number) {
        String str = "";
        for(int i = 0; i < max_byte_number - 1; i++) {
            str = str + randomness.nextChar((char) 0x0001, (char) 0x10FFFF); // uses maximum range of Unicode
        }
        byte[] bytes = Arrays.copyOfRange(str.getBytes(StandardCharsets.UTF_8), 0, str.length());
        if(bytes.length > 0 && bytes.length < 5) {
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) generateRandomInt(1, 127, randomness);
            }
            return bytes;
        }
        return utf8_correction(bytes);
    }

    public byte[] utf8_correction(byte[] b) { // removes wrong bytes from the end
        if(b.length == 0) {return b;}
        if((b[b.length-1] >> 7) == 0) {
            return b;
        }
        if((b[b.length-1] >> 6) == -1) {
            return Arrays.copyOfRange(b, 0, b.length - 1);
        }
        return utf8_correction(Arrays.copyOfRange(b, 0, b.length - 1));
    }

    private int generateRandomIntBase2(int power, SourceOfRandomness randomness){

        return generateRandomInt(0, (int) Math.pow(2, power) - 1, randomness);
    }

    /**
     *
     * @param min inclusive min
     * @param max inclusive max
     * @param randomness
     * @param amountOfValues
     * @return
     */
    private int generateRandomInt(int min, int max, SourceOfRandomness randomness, int amountOfValues){

        if(fullRange) {
            int value = randomness.nextInt(min, max + 1);
            if(value == max + 1)
                System.out.println("INCLUSIVE!!! Randomness is suddenly inclusive");
            return value;
        }
        return min + (max - min) / amountOfValues * randomness.nextInt(amountOfValues);
    }

    private int generateRandomInt(int min, int max, SourceOfRandomness randomness){
        return generateRandomInt(min, max, randomness, 10);
    }

    public static void main(String[] args) {

        for(int i = 0; i < 1; i++) {

            PngDataGenerator gen = new PngDataGenerator(true, true,true);
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
}