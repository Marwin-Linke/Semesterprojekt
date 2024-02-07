#!/bin/bash

# Loop for 10 times
for ((i=4; i<=10; i++)); do
    # Execute the mvn command
    mvn jqf:fuzz -Dclass="edu.berkeley.cs.jqf.examples.pngj.PngTest" -Dmethod=testPngPipeline -Dtime=1h

    cp "target/fuzz-results/edu.berkeley.cs.jqf.examples.pngj.PngTest/testPngPipeline/plot_data" "plot_data_automated/plot_data_baseline_$i"

    echo "Copied plot data to plot_data_automated/plot_data_baseline_$i"
done