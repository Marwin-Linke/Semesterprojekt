# Png-Fuzzer Implementation Using JQF

For our semester project we try to implement a fuzzer that fuzzes multiple png-libraries. Our main focus is `pngj` at the moment.
## Todo-List

Here you can find our current progress.

### Generator

* Interlacing (Completed)
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
  * `bKGD` (Background color)
  * `pHYs` (Physical pixel dimensions)
  * `iCCP` (Embedded ICC profile)
  * `sBIT` (Significant bits)
  * `sPLT` (Suggested palette)
  * `hIST` (Palette histogram)
  * `tIME` (Image last-modification time)