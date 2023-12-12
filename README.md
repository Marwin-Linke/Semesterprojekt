# Png-Fuzzer Implementation Using JQF

For our semester project we try to implement a fuzzer that fuzzes multiple png-libraries. Our main focus is `pngj` at the moment, but we plan to test other lesser known libraries too.

## Todo-List

Here you can find our current progress.

### Generator

As for the generator, we still need to add a lot of functionality, this includes:

* Interlacing
* Implemented chunks
  * `IHDR` (Image header)
  * `IDAT` (Image data)
  * `IEND` (Image trailer)
  * `PLTE` (Palette)
  * `tEXt` (Textual data)
  * `zTXt` (Compressed textual data)
  * `tRNS` (Transparency)
  * `gAMA` (Image gamma)
  * `cHRM` (Primary chromaticities)
  * `sRGB` (Standard RGB color space)
  * `iTXt` (International textual data)
* Implemented chunks but with unique failures
  * `testMirroringPng` causes the error message "missing palette", `PLTE` is probably not working correctly
* Remaining chunks
  * `iCCP` (Embedded ICC profile)
  * `bKGD` (Background color)
  * `pHYs` (Physical pixel dimensions)
  * `sBIT` (Significant bits)
  * `sPLT` (Suggested palette)
  * `hIST` (Palette histogram)
  * `tIME` (Image last-modification time)
* Chunk ordering
* Compatibility checks
* `APNG`

### Driver

* Writing a driver that reaches a high coverage and tests most functionalities of `pngj`
* Testing other libraries

### Performance

* Refactoring the generator to improve performance
* Improving the usage of `randomness` to reach more coverage

### Evaluation

* Writing an evaluation driver that runs and tests our fuzzer and collects statistical data
* Comparing it to a baseline implementation that only creates minimal Pngs

### Report

* Writing the report