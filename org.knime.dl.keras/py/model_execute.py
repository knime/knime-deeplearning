# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME GmbH, Konstanz, Germany
#  Website: http://www.knime.org; Email: contact@knime.org
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License, Version 3, as
#  published by the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, see <http://www.gnu.org/licenses>.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
#  Hence, KNIME and ECLIPSE are both independent programs and are not
#  derived from each other. Should, however, the interpretation of the
#  GNU GPL Version 3 ("License") under any applicable laws result in
#  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
#  you the additional permission to use and propagate KNIME together with
#  ECLIPSE with only the license terms in place for ECLIPSE applying to
#  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
#  license terms of ECLIPSE themselves allow for the respective use and
#  propagation of ECLIPSE together with KNIME.
#
#  Additional permission relating to nodes for KNIME that extend the Node
#  Extension (and in particular that are based on subclasses of NodeModel,
#  NodeDialog, and NodeView) and that only interoperate with KNIME through
#  standard APIs ("Nodes"):
#  Nodes are deemed to be separate and independent programs and to not be
#  covered works.  Notwithstanding anything to the contrary in the
#  License, the License does not apply to Nodes, you are not required to
#  license Nodes under the License, and you are granted a license to
#  prepare and propagate Nodes, in each case even if such Nodes are
#  propagated with or for interoperation with KNIME.  The owner of a Node
#  may freely choose the license terms applicable to such Node, including
#  when such Node is propagated with or for interoperation with KNIME.
# ------------------------------------------------------------------------

import pandas as pd
import numpy as np
from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer

import debug_util

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

Y = model.predict(X, verbose=0)  # don't change to predict_proba

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
