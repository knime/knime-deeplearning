# -*- coding: utf-8 -*-

from io import BytesIO
import os
import sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from DLPythonDataBuffers import DLPythonFloatBuffer

def serialize(value):
	if not value.array.dtype == np.float32:
		value = DLPythonFloatBuffer(value.array.astype(np.float32))
	buffer = BytesIO(bytes())
	buffer.write(value.array.tobytes())
	return buffer.getvalue()

