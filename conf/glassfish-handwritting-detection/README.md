# Handwritten detection installation

To allow detecting from handwriting image text you need to install following libraries:
* TrOcr library base on transformer which can translate line of image with handwriting text to text
* WordDetectorNN - neural network whci hallow to detect words in image, theris bounding boxes

All dependencies are in shell script `install.htr.sh` which are needed by TrOcr and WordDetectorNN

### Configuration
* place `trocr-line.py` on server where glassfish is running in `/opt` directory
* set path to htr script in database `settings` table key `:HTRCommand` value: `/opt/trocr-line.py`
* install all dependencies from `install.htr.sh`
* you can test if script is working running `cat some-image.jpg | python trocr-line.py`

### Script structure trocr-line.py

To allow TrOcr translate image with handwritten text to text we need to prepare line of text. 
We have two main parts
* preparing image for trocr
* executing trocr on cropped image

Preparing image steps:
* deskew image
* segment page of text to line of text
  * find all word on page with bounding boxes
  * base on bounding boxes find lines of text
  * crop line of text and pass to trocr


