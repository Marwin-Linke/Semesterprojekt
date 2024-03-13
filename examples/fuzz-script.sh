#!/bin/bash

# Loop for 10 times
for ((i=2; i<=10; i++)); do
    # Execute the mvn command
    mvn jqf:fuzz -Dclass="edu.berkeley.cs.jqf.examples.pngj.PngTest" -Dmethod=testRandomChunksPipeline -Dtime=1h

    cp "target/fuzz-results/edu.berkeley.cs.jqf.examples.pngj.PngTest/testRandomChunksPipeline/plot_data" "plot_data_automated/plot_data_probable_$i"

    echo "Copied plot data to plot_data_automated/plot_data_probable_$i"
done