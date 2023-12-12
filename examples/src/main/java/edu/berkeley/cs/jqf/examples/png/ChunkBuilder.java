package edu.berkeley.cs.jqf.examples.png;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

import static edu.berkeley.cs.jqf.examples.png.PngDataGenerator.intToByteArray;

public class ChunkBuilder {

    public static byte[] calculateCRC(byte[] bytes){
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return intToByteArray((int)crc.getValue());
    }

    public static byte[] calculateCRC(byte[] bytes, int offset, int length){
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return intToByteArray((int)crc.getValue());
    }

    public static byte[] calculateCRC(ByteArrayOutputStream byteStream){
        return calculateCRC(byteStream.toByteArray());
    }

    public static byte[] calculateCRC(ByteArrayOutputStream byteStream, int offset, int length){
        return calculateCRC(byteStream.toByteArray(), offset, length);
    }

    public static byte[] constructChunk(byte[] chunkType, byte[] chunkContent) {

        ByteArrayOutputStream chunk = new ByteArrayOutputStream();

        try {

            chunk.write(intToByteArray(chunkContent.length));
            chunk.write(chunkType);
            chunk.write(chunkContent);
            chunk.write(calculateCRC(chunk, 4, chunk.size() - 4));
            // calculates CRC without the length

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return chunk.toByteArray();

    }

    public static byte[] constructChunk(byte[] chunkType, ByteArrayOutputStream chunkContent) {
        return constructChunk(chunkType, chunkContent.toByteArray());
    }

}
