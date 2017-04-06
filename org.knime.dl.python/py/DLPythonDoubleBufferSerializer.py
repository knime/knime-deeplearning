# -*- coding: utf-8 -*-

from io import BytesIO
import os
import sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from DLPythonDataBuffers import DLPythonDoubleBuffer

def serialize(value):
	if not value.array.dtype == np.float64:
		value = DLPythonDoubleBuffer(value.array.astype(np.float64))
	buffer = BytesIO(bytes())
	buffer.write(value.array.tobytes())
	return buffer.getvalue()

