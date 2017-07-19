# -*- coding: utf-8 -*-

import numpy as np
from DLPythonDataBuffers import DLPythonIntBuffer

global test_out_data
test_out_data = test_in_data.applymap(lambda buff : DLPythonIntBuffer(np.vectorize(lambda x : x * 5)(buff.array)))
test_out_data.rename(columns={'test_in_data' : 'test_out_data'}, inplace=True)
