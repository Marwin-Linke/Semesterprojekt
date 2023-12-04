package edu.berkeley.cs.jqf.examples.png;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class PngGenerator extends Generator<PngData> {

    PngDataGenerator generator;

    public PngGenerator() {
        super(PngData.class);
        generator = new PngDataGenerator();
    }

    @Override
    public PngData generate(SourceOfRandomness random, GenerationStatus __ignore__) {

        return new PngData(generator.generate(random));

    }

}
