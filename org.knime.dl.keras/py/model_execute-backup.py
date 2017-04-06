# -*- coding: utf-8 -*-

import pandas as pd
import numpy as np
from DLPythonDataBuffers import DLPythonFloatBuffer

X = input_table.values

X = list(map((lambda b: b[0].array), X))
Y = np.vstack(X)

global model
prediction = model.predict_proba(Y, verbose=0)
print(prediction)

#output_table = pd.DataFrame(prediction, columns=[column_names])
output_column = []
output_column.append(DLPythonFloatBuffer(prediction))

# TODO currently we only support one output layer. 
globals()['dense'] = pd.DataFrame({column_names:output_column})
print(dense)
