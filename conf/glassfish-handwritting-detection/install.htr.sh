# Python for local docker installation, python3.9, pip should be already on test, prod environment
yum install -y python
python3 -m ensurepip --upgrade

pip3 install -q transformers
pip3 install  -q -U sentencepiece
pip3 install pillow
pip3 install opencv-python-headless
pip3 install scikit-learn
pip3 install matplotlib

pip3 install https://github.com/rscipien/WordDetectorNN/archive/refs/heads/master.zip
#Requirements: pip install opencv-python pillow numpy transformers torch

curl -L -o trocr-line.py https://github.com/CeON/fairchive/blob/feature/2957-htr/conf/glassfish-handwritting-detection/trocr-line.py

curl -L -o weights https://github.com/rscipien/WordDetectorNN/blob/master/model/weights