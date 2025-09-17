yum install -y python
yum install -y mesa-libGL glib2 # for opencv
python3 -m ensurepip --upgrade

pip3 install -q transformers
pip3 install  -q -U sentencepiece
pip3 install pillow
pip3 install opencv-python-headless
pip install craft-text-detector

Requirements: pip install opencv-python pillow numpy transformers torch