# Python for local docker installation, python3.9, pip should be already on test, prod environment
# Command for docker
if [ "$1" = "docker" ]; then
    echo "Docker"
    yum install -y python
    python3 -m ensurepip --upgrade
fi
# Create virtual env
python3 -m venv /opt/glassfish/python
# activate venv
source /opt/glassfish/python/bin/activate

# on production install using glassfish user
pip3 install -q transformers
pip3 install  -q -U sentencepiece
pip3 install pillow
pip3 install opencv-python-headless
pip3 install scikit-learn
pip3 install matplotlib

pip3 install https://github.com/rscipien/WordDetectorNN/archive/refs/heads/master.zip
#Requirements: pip install opencv-python pillow numpy transformers torch

curl -L -o trocr-line.py https://raw.githubusercontent.com/CeON/fairchive/refs/heads/develop/conf/glassfish-handwritting-detection/trocr-line.py

curl -L -o weights https://github.com/rscipien/WordDetectorNN/raw/refs/heads/master/model/weights

curl -L -o test-htr.jpg https://raw.githubusercontent.com/CeON/fairchive/refs/heads/develop/conf/glassfish-handwritting-detection/computer-architecture-test-htr.jpg

# after installation remove pip cache, we should save ~3GB from cache
pip cache purge