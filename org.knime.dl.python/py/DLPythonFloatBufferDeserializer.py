# -*- coding: utf-8 -*-

from io import BytesIO
import os
import sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from DLPythonDataBuffers import DLPythonFloatBuffer

def deserialize(bytes):
	return DLPythonFloatBuffer(np.frombuffer(bytes, dtype=np.float32))

