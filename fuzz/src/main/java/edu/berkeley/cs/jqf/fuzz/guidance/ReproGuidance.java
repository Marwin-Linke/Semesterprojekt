/*
 * Copyright (c) 2017, University of California, Berkeley
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.fuzz.guidance;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * A front-end that provides a specified set of inputs for test
 * case reproduction,
 *
 * This class enables reproduction of a test case with an input file
 * generated by a guided fuzzing front-end such as AFL.
 *
 * @author Rohan Padhye
 */
public class ReproGuidance implements Guidance {
    private final File[] inputFiles;
    private final File traceDir;
    private int nextFileIdx = 0;
    private List<PrintStream> traceStreams = new ArrayList<>();
    private InputStream inputStream;

    /**
     * Constructs an instance of ReproGuidance with a list of
     * input files to replay and a directory where the trace
     * events may be logged.
     *
     * @param inputFiles a list of input files
     * @param traceDir an optional directory, which if non-null will
     *                 be the destination for log files containing event
     *                 traces
     */
    public ReproGuidance(File[] inputFiles, File traceDir) {
        this.inputFiles = inputFiles;
        this.traceDir = traceDir;
    }

    /**
     * Returns an input stream corresponding to the next input file.
     *
     * @return an input stream corresponding to the next input file
     */
    @Override
    public InputStream getInput() {
        try {
            File inputFile = inputFiles[nextFileIdx];
            this.inputStream = new BufferedInputStream(new FileInputStream(inputFile));
            return this.inputStream;
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    /**
     * Returns <tt>true</tt> if there are more input files to replay.
     * @return <tt>true</tt> if there are more input files to replay
     */
    @Override
    public boolean hasInput() {
        return nextFileIdx < inputFiles.length;
    }

    /**
     * Logs the end of run in the log files, if any.
     *
     * @param result   the result of the fuzzing trial
     * @param error    the error thrown during the trial, or <tt>null</tt>
     */
    @Override
    public void handleResult(Result result, Throwable error) {
        // Print footer in log files
        String footer = String.format("# End %s", inputFiles[nextFileIdx].toString());
        traceStreams.forEach((out) -> out.println(footer));

        // Increment file
        nextFileIdx++;

        // Show errors if any
        if (error != null) {
            error.printStackTrace();
        }


    }

    /**
     * Returns a callback that can log trace events to a trace file.
     *
     * <p>If <tt>traceDir</tt> was non-null during the construction of
     * this Guidance instance, then one log file per thread of
     * execution is created in this directory. The callbacks generated
     * by this method write trace event descriptions in sequence to
     * their own thread's log files.
     *
     * @param threadName  the name of the thread whose events to handle
     * @return  callback that can log trace events to a trace file
     */
    @Override
    public Consumer<TraceEvent> generateCallBack(String threadName) {
        // Create trace file if available
        if (traceDir != null) {
            File traceFile = new File(traceDir, threadName + ".log");
            try {
                PrintStream out = new PrintStream(traceFile);
                traceStreams.add(out);

                // Return an event logging callback
                return (e) -> {
                    out.println(e);
                };
            } catch (FileNotFoundException e) {
                // Note the exception, but ignore trace events
                System.err.println("Could not open trace file: " + traceFile.getAbsolutePath());
            }
        }

        // Ignore trace events
        return (e) -> {};
    }


}
