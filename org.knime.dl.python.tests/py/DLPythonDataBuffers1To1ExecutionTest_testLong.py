# -*- coding: utf-8 -*-

import numpy as np
from DLPythonDataBuffers import DLPythonLongBuffer

global test_out_data
test_out_data = test_in_data.iloc[:,0].map(
        lambda buff : DLPythonLongBuffer(np.vectorize(lambda x : x * 5)(buff.array))
    ).to_frame('test_out_data')
