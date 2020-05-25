# -*- coding: utf-8 -*-

import numpy as np
from DLPythonDataBuffers import DLPythonStringBuffer

global test_out_data
test_out_data = test_in_data.iloc[:,0].map(
        lambda buff : DLPythonStringBuffer(np.vectorize(lambda x : x + b'_suffix')(buff.array))
    ).to_frame('test_out_data')
