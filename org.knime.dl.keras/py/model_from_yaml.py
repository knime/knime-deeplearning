# -*- coding: utf-8 -*-

import sys
sys.stderr = sys.stdout
from keras.models import model_from_yaml
sys.stderr = sys.__stderr__

f = open(model_load_path, 'r')
model_yaml_string = f.read()
f.close()

global model
model = model_from_yaml(model_yaml_string)
