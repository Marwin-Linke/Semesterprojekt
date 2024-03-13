package edu.berkeley.cs.jqf.examples.chunks;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.png.PngData;

public class RandomChunksGenerator extends Generator<PngData> {

    RandomChunksDataGenerator generator;

    public RandomChunksGenerator() {
        super(PngData.class);
        generator = new RandomChunksDataGenerator(true, true);
    }

    @Override
    public PngData generate(SourceOfRandomness random, GenerationStatus __ignore__) {

        return new PngData(generator.generate(random));

    }

}
