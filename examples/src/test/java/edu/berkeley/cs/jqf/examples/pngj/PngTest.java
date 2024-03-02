package edu.berkeley.cs.jqf.examples.pngj;

import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assume;
import org.junit.runner.RunWith;
import edu.berkeley.cs.jqf.examples.png.*;

import java.io.*;

@RunWith(JQF.class)
public class PngTest {


    @Fuzz
    public void testPngPipeline(@From(PngGenerator.class) PngData pngInput) {

        PngData currentData;
        PngReader currentReader;

        currentReader = readPng(pngInput);

        if(currentReader.imgInfo.indexed) { // Indexed => True Color
            currentData = convertToTrueColor(currentReader);
            currentReader = readPng(currentData);
        }

        if(currentReader.imgInfo.channels >= 3) { // True Color => Grayscale
            currentData = desaturateColors(currentReader);
            currentReader = readPng(currentData);
            currentData = convertToGrayscale(currentReader);
            currentReader = readPng(currentData);
        }

        currentData = mirrorPng(currentReader);
        currentReader = readPng(currentData);

        currentReader.close();

    }

    public PngData convertToTrueColor(PngReader pngr) {
        PngChunkPLTE plte = pngr.getMetadata().getPLTE();
        PngChunkTRNS trns = pngr.getMetadata().getTRNS(); // transparency metadata, can be null
        boolean alpha = trns != null;
        ImageInfo im2 = new ImageInfo(pngr.imgInfo.cols, pngr.imgInfo.rows, 8, alpha);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PngWriter pngw = new PngWriter(outputStream, im2);
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
        int[] buf = null;
        for (int row = 0; row < pngr.imgInfo.rows; row++) {
            ImageLineInt line1 = (ImageLineInt) pngr.readRow(row);
            buf = ImageLineHelper.palette2rgb(line1, plte, trns, buf);
            ImageLineInt line2 = new ImageLineInt(pngw.imgInfo, buf);
            pngw.writeRow(line2);
        }
        pngr.end();
        pngw.end();
        return new PngData(outputStream.toByteArray());
    }

    private PngData desaturateColors(PngReader pngr) {

        int channels = pngr.imgInfo.channels;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PngWriter pngw = new PngWriter(outputStream, pngr.imgInfo);
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
        pngw.getMetadata().setText(PngChunkTextVar.KEY_Description, "Decreased red and increased green");
        for (int row = 0; row < pngr.imgInfo.rows; row++) { // also: while(pngr.hasMoreRows())
            IImageLine l1 = pngr.readRow();
            int[] scanline = ((ImageLineInt) l1).getScanline(); // to save typing
            for (int j = 0; j < pngr.imgInfo.cols; j++) {

                int red = scanline[j * channels];
                int green = scanline[j * channels + 1];
                int blue = scanline[j * channels + 2];
                int maxValue = Math.max(red, Math.max(green, blue));
                scanline[j * channels] = ImageLineHelper.clampTo_0_65535(maxValue);
                scanline[j * channels + 1] = ImageLineHelper.clampTo_0_65535(maxValue);
                scanline[j * channels + 2] = ImageLineHelper.clampTo_0_65535(maxValue);

            }
            pngw.writeRow(l1);
        }
        pngr.end();
        pngw.end();

        return new PngData(outputStream.toByteArray());

    }

    public PngData convertToGrayscale(PngReader pngr) {

        ImageInfo grayscaleInfo = new ImageInfo(
                pngr.imgInfo.cols,
                pngr.imgInfo.rows,
                pngr.imgInfo.bitDepth,
                pngr.imgInfo.alpha,
                true,
                false
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PngWriter pngw = new PngWriter(outputStream, grayscaleInfo);
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
        for (int row = 0; row < pngr.imgInfo.rows; row++) { // also: while(pngr.hasMoreRows())
            IImageLine l1 = pngr.readRow();
            int[] scanline = ((ImageLineInt) l1).getScanline(); // to save typing
            int[] buffer = new int[pngw.imgInfo.bytesPerRow];
            for (int j = 0; j < pngr.imgInfo.cols; j++) {
                buffer[j] = scanline[j * pngr.imgInfo.channels];
            }
            ImageLineInt line2 = new ImageLineInt(pngw.imgInfo, buffer);
            pngw.writeRow(line2);
        }
        pngr.end();
        pngw.end();
        return new PngData(outputStream.toByteArray());
    }

    public PngData mirrorPng(PngReader pngr) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PngWriter pngw = new PngWriter(outputStream, pngr.imgInfo);
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
        for (int row = 0; row < pngr.imgInfo.rows; row++) {
            ImageLineInt line = (ImageLineInt) pngr.readRow(row);
            mirrorLineInt(pngr.imgInfo, line.getScanline());
            pngw.writeRow(line, row);
        }
        pngr.end();
        pngw.end();

        return new PngData(outputStream.toByteArray());
    }

    private static void mirrorLineInt(ImageInfo imgInfo, int[] line) { // unpacked line
        int channels = imgInfo.channels;
        for (int c1 = 0, c2 = imgInfo.cols - 1; c1 < c2; c1++, c2--) { // swap pixels (not samples!)
            for (int i = 0; i < channels; i++) {
                int aux = line[c1 * channels + i];
                line[c1 * channels + i] = line[c2 * channels + i];
                line[c2 * channels + i] = aux;
            }
        }
    }

    private PngReader readPng (PngData pngData) {

        InputStream stream = new ByteArrayInputStream(pngData.data);
        PngReader pngr = new PngReader(stream);
        return pngr;
    }

    public static byte[] extractBytes (File file) {

        byte[] bytes = new byte[(int) file.length()];

        try(FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    public static void createPngFile (PngData pngData, String fileName) {

        try {

            FileOutputStream fos = new FileOutputStream(fileName + ".png");
            fos.write(pngData.data);
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}