package edu.berkeley.cs.jqf.examples.pngj;

import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;
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
    public void testOpeningPNG(@From(PngGenerator.class) PngData pngData) {

        readPng(pngData);

    }

    @Fuzz
    public void testEditingPNG(@From(PngGenerator.class) PngData pngData) {

        PngData editedPng = changeColors(pngData); // reads png and changes color

        readPng(editedPng); // reads png and closes reader

        createPngFile(pngData, "Original_Png"); // creates local png file

        createPngFile(editedPng, "Last_Converted_Png");

    }

    private PngData changeColors(PngData pngData) {

        InputStream stream = new ByteArrayInputStream(pngData.data);
        PngReader pngr = new PngReader(stream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();



        int channels = pngr.imgInfo.channels;
        Assume.assumeTrue(channels >= 3);

        if (channels < 3 || pngr.imgInfo.bitDepth != 8)
            throw new RuntimeException("This method is for RGB8/RGBA8 images");
        //File editedPng = new File("Edited_Png.png");
        PngWriter pngw = new PngWriter(outputStream, pngr.imgInfo);
        pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
        pngw.getMetadata().setText(PngChunkTextVar.KEY_Description, "Decreased red and increased green");
        for (int row = 0; row < pngr.imgInfo.rows; row++) { // also: while(pngr.hasMoreRows())
            IImageLine l1 = pngr.readRow();
            int[] scanline = ((ImageLineInt) l1).getScanline(); // to save typing
            for (int j = 0; j < pngr.imgInfo.cols; j++) {
                scanline[j * channels] /= 2;
                scanline[j * channels + 1] = ImageLineHelper.clampTo_0_255(scanline[j * channels + 1] + 20);
            }
            pngw.writeRow(l1);
        }

        pngr.end(); // it's recommended to end the reader first, in case there are trailing chunks to read
        pngw.end();

        PngData editedPngData = new PngData(outputStream.toByteArray());

        return editedPngData;

    }

    private void readPng (PngData pngData) {

        InputStream stream = new ByteArrayInputStream(pngData.data);
        PngReader pngr = new PngReader(stream);
        pngr.end();

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