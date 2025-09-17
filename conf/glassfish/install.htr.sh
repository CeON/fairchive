yum install -y python
#yum install -y mesa-libGL glib2 # for opencv
#install pip
python3 -m ensurepip --upgrade

pip3 install -q transformers
pip3 install  -q -U sentencepiece
pip3 install pillow
pip3 install opencv-python-headless
pip3 install sklearn
pip3 install matplotlib

pip3 install https://github.com/rscipien/WordDetectorNN/archive/refs/heads/master.zip
#Requirements: pip install opencv-python pillow numpy transformers torch