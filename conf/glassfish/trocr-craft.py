import sys, io
from transformers import TrOCRProcessor, VisionEncoderDecoderModel
from PIL import Image
import cv2
import numpy as np
import torch
from craft_text_detector import Craft


# Segmentation params (tweak if needed)
ADAPTIVE_BLOCK = 35        # odd; larger handles uneven lighting
ADAPTIVE_C = 15            # subtract; bigger -> more foreground
CLOSE_KERNEL_X = 7         # horizontal close to connect strokes
SMOOTH_K = 15              # moving-average window over row-sum
MIN_LINE_HEIGHT = 10       # px; skip tiny bands
ROW_THRESH_FRACTION = 0.22 # fraction of max of smoothed row-sum to call "text"
PAD_Y = 3                  # px padding around line crops
PAD_X = 3

DEVICE = "cuda" if torch.cuda.is_available() else ("mps" if getattr(torch.backends, "mps", None) and torch.backends.mps.is_available() else "cpu")

# -----------------------------
# Line segmentation (projection)
# -----------------------------
def segment_lines(img_bgr):
    """
    Returns a list of line boxes (x1, y1, x2, y2) top-to-bottom.
    """
    craft = Craft(output_dir=None, crop_type="box", cuda=False)
    result = craft.detect_text(image_path)
    boxes = result["boxes"]
    # sort top->bottom
    boxes.sort(key=lambda b: (b[1] + b[3]) / 2.0)
    return boxes

def main():
    # 1) Read image bytes from stdin
    data = sys.stdin.buffer.read()
    if not data:
        print("No input received on stdin", file=sys.stderr)
        sys.exit(1)

    # 2) Open image
    #image = Image.open(io.BytesIO(data)).convert("RGB")
    arr = np.frombuffer(data, dtype=np.uint8)   # 1D uint8 array
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)   # decode to BGR image

    # 3) Segement lines
    boxes = segment_lines(img)
    print(f"Found {len(boxes)} line(s).")

    # 2) Prepare per-line crops (as PIL RGB)
    line_images = []
    for i, (x1,y1,x2,y2) in enumerate(boxes, 1):
        crop_bgr = img[y1:y2+1, x1:x2+1]
        crop_rgb = cv2.cvtColor(crop_bgr, cv2.COLOR_BGR2RGB)
        line_images.append(Image.fromarray(crop_rgb))
        #line_images.append(crop_rgb)

    if not line_images:
        print("No lines detected. Exiting.")
        return




    # 3) Load model & processor (handwritten specialization)
    processor = TrOCRProcessor.from_pretrained("microsoft/trocr-base-handwritten")
    model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-base-handwritten")


    # 4) Batch OCR for speed (you can adjust batch size if you have many lines)
    batch_size = 8
    all_text = []
    model.eval()
    if (len(line_images) == 1):
      image = Image.open(io.BytesIO(data)).convert("RGB")
      pixel_values = processor(image, return_tensors="pt").pixel_values.to(DEVICE)
      generated_ids = model.generate(pixel_values)
      text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
      print(text)
    else:
      with torch.inference_mode():
          for i in range(0, len(line_images), batch_size):
              batch = line_images[i:i+batch_size]
              pixel_values = processor(batch, return_tensors="pt").pixel_values.to(DEVICE)
              generated_ids = model.generate(
                  pixel_values,
                  # optional knobs:
                  # num_beams=3,
                  # max_new_tokens=128,
              )
              texts = processor.batch_decode(generated_ids, skip_special_tokens=True)
              # strip and keep order
              all_text.extend([t.strip() for t in texts])

      combined = "\n".join(all_text)
      print(combined)


if __name__ == "__main__":
    main()