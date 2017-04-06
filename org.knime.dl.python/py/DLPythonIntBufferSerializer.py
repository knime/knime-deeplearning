# -*- coding: utf-8 -*-

from io import BytesIO
import os
import sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from DLPythonDataBuffers import DLPythonIntBuffer

def serialize(value):
	if not value.array.dtype == np.int32:
		value = DLPythonIntBuffer(value.array.astype(np.int32))
	buffer = BytesIO(bytes())
	buffer.write(value.array.tobytes())
	return buffer.getvalue()

