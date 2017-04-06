# -*- coding: utf-8 -*-

import pandas as pd
import numpy as np
from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer

# TODO: this does not yet take (predefined) batch size (of the input) into account
def reshapeInputs():
	X = []
	for _ , row in input_specs.iterrows():
		name = row['name']
		shape = row['shape']
		data = globals()[name].values
		data = list(map((lambda b: b[0].array.reshape([1] + shape)), data))
		X.append(np.vstack(data))
	return X

X = reshapeInputs()

Y = model.predict(X, verbose=0) # don't change to predict_proba

# put single outputs in list to have a common way of handling single and multiple outputs
if not isinstance(Y, (list, tuple)):
	Y = [Y]

def putInMatchingBuffer(y):
	t = y.dtype
	if t == np.float64:
		return DLPythonDoubleBuffer(y)
	elif t == np.float32:
		return DLPythonFloatBuffer(y)
	elif t == np.int32:
		return DLPythonIntBuffer(y)
	elif t == np.int64:
		return DLPythonLongBuffer(y)
	# TODO: support more types
	else:
		# TODO: warning to stderr?
		return DLPythonDoubleBuffer(y)

# TODO: output selected outputs only
for idx, row in output_specs.iterrows():
	name = row['name']
	shape = row['shape']
	batchSize = 1
	if len(Y[idx].shape) > len(shape):
		batchSize = Y[idx].shape[0]
	out = []
	for i in range(0, batchSize):
		out.append(putInMatchingBuffer(Y[idx][i]))
	out = pd.DataFrame({name:out})
	globals()[name] = out;
