import sys, io
from transformers import TrOCRProcessor, VisionEncoderDecoderModel
from PIL import Image
import cv2
import numpy as np
import torch


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
    h, w = img_bgr.shape[:2]
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)

    # 1) Adaptive binarization + invert (text=255)
    bw = cv2.adaptiveThreshold(
        gray, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV,
        ADAPTIVE_BLOCK, ADAPTIVE_C
    )

    # 2) Strengthen horizontal continuity
    bw = cv2.morphologyEx(
        bw, cv2.MORPH_CLOSE,
        cv2.getStructuringElement(cv2.MORPH_RECT, (CLOSE_KERNEL_X, 1)),
        iterations=1
    )

    # 3) Horizontal projection profile
    row_sum = bw.sum(axis=1) / 255.0  # count of foreground pixels per row
    k = max(3, SMOOTH_K | 1)          # ensure odd
    smooth = np.convolve(row_sum, np.ones(k, dtype=np.float32)/k, mode="same")

    # 4) Threshold rows into text vs whitespace
    tau = ROW_THRESH_FRACTION * float(smooth.max() if smooth.max() > 0 else 1.0)
    text_rows = smooth > tau

    # 5) Find contiguous text bands
    edges = np.diff(text_rows.astype(np.int8), prepend=0, append=0)
    starts = np.where(edges == 1)[0]
    ends   = np.where(edges == -1)[0] - 1

    boxes = []
    for y1, y2 in zip(starts, ends):
        if y2 - y1 + 1 < MIN_LINE_HEIGHT:
            continue

        # refine left/right inside the band via vertical projection
        band = bw[y1:y2+1, :]
        x_profile = band.sum(axis=0) / (255.0 * (y2 - y1 + 1))
        # columns with some text
        if x_profile.max() <= 0:
            continue
        xmask = x_profile > (0.15 * x_profile.max())
        if not xmask.any():
            continue

        x_edges = np.diff(xmask.astype(np.int8), prepend=0, append=0)
        xs = np.where(x_edges == 1)[0]
        xe = np.where(x_edges == -1)[0] - 1
        if len(xs) == 0 or len(xe) == 0:
            continue

        x1 = int(xs[0]); x2 = int(xe[-1])
        x1 = max(0, x1 - PAD_X)
        x2 = min(w - 1, x2 + PAD_X)
        y1p = max(0, y1 - PAD_Y)
        y2p = min(h - 1, y2 + PAD_Y)
        boxes.append((x1, y1p, x2, y2p))

    # sort top->bottom
    boxes.sort(key=lambda b: (b[1] + b[3]) / 2.0)
    return boxes

def main():
    try:
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

    except Exception as e:
        print(f"OCR error: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()