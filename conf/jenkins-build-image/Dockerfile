FROM openjdk:8u342-jdk

RUN addgroup --gid 1000 jenkins && \
    adduser --uid 1000 --ingroup jenkins --no-create-home --disabled-password jenkins

RUN addgroup jenkins root

# Install Tesseract and language packs
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \  # English language pack 
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENTRYPOINT []
