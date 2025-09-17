import sys, io
from transformers import TrOCRProcessor, VisionEncoderDecoderModel
from PIL import Image
import cv2
import numpy as np
import torch
from worddetectornn import DataLoaderImgBytes, WordDetectorNet, evaluate


DEVICE = "cuda" if torch.cuda.is_available() else ("mps" if getattr(torch.backends, "mps", None) and torch.backends.mps.is_available() else "cpu")

# -----------------------------
# Line segmentation (projection)
# -----------------------------
def segment_lines(img_bgr):
    """
    Returns a list of line boxes (x1, y1, x2, y2) top-to-bottom.
    """
    net = WordDetectorNet()
    net.load_state_dict(torch.load('weights', map_location=DEVICE))
    net.eval()
    net.to(DEVICE)

    loader = DataLoaderImgBytes([img_bgr], net.input_size, DEVICE)
    res = evaluate(net, loader, max_aabbs=1000)

    aabbs = []
    for i, (img, aabbs) in enumerate(zip(res.batch_imgs, res.batch_aabbs)):
        f = loader.get_scale_factor(i)
        aabbs = [aabb.scale(1 / f, 1 / f) for aabb in aabbs]

    line_box = group_boxes_dynamic(aabbs)
    line_box = merge_boxes_to_lines(line_box)
    return line_box

def group_boxes_dynamic(boxes, min_overlap=0.3):
    """
    Group AABB boxes into text lines dynamically, based on vertical overlap.
    """
    boxes_sorted = sorted(boxes, key=lambda b: b.ymin)
    lines = []
    current_line = [boxes_sorted[0]]

    def vertical_overlap_ratio(b1, b2):
        overlap = max(0, min(b1.ymax, b2.ymax) - max(b1.ymin, b2.ymin))
        height = min(b1.ymax - b1.ymin, b2.ymax - b2.ymin)
        return overlap / height if height > 0 else 0

    for b in boxes_sorted[1:]:
        prev = current_line[-1]
        if vertical_overlap_ratio(prev, b) > min_overlap:
            current_line.append(b)
        else:
            lines.append(sorted(current_line, key=lambda x: x.xmin))
            current_line = [b]

    lines.append(sorted(current_line, key=lambda x: x.xmin))
    return lines

def merge_boxes_to_lines(lines):
    """
    Take grouped lines (list of list of AABB) and return one AABB per line.
    """
    merged = []
    for line in lines:
        xmin = min(b.xmin for b in line)
        xmax = max(b.xmax for b in line)
        ymin = min(b.ymin for b in line)
        ymax = max(b.ymax for b in line)
        merged.append((int(xmin), int(xmax), int(ymin), int(ymax)))
    return merged

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

    # 3) Segment lines
    line_box = segment_lines(img)

    # 4) Prepare per-line crops (as PIL RGB)
    line_images = []
    for (xmin, xmax, ymin, ymax) in line_box:
        crop_bgr = img[ymin:ymax+1, xmin:xmax+1]
        crop_rgb = cv2.cvtColor(crop_bgr, cv2.COLOR_BGR2RGB)
        line_images.append(Image.fromarray(crop_rgb))
        # debug purpose
        #cv2.rectangle(img, (aabb.xmin, aabb.ymin), (aabb.xmax, aabb.ymax), (255, 0, 255), 2)
        #crop_bgr = img[int(box.ymin):int(box.ymax), int(box.xmin):int(box.xmax)]
        #cv2.imwrite(f'output-{i}.jpg', crop_bgr)    # saves as JPEG


    if not line_images:
        return


    # 5) Load model & processor (handwritten specialization)
    processor = TrOCRProcessor.from_pretrained("microsoft/trocr-base-handwritten")
    model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-base-handwritten")


    # 6) Batch OCR for speed (you can adjust batch size if you have many lines)
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