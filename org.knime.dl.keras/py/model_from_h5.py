# -*- coding: utf-8 -*-

import sys
sys.stderr = sys.stdout
from keras.models import load_model
sys.stderr = sys.__stderr__

global model
model = load_model(model_load_path)
