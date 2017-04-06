# -*- coding: utf-8 -*-

import pandas as pd

def get_specs_for(layerData):
	specs = pd.DataFrame(index=range(len(layerData)), columns=('name', 'batch_size', 'shape', 'type'))
	for i, mIn in enumerate(layerData):
		shape = mIn.shape.as_list()
		specs.iloc[i] = [mIn.name, shape[0], None, mIn.dtype.name]
		specs.set_value(i, 'shape', mIn.shape.as_list()[1:])  # don't change
	return specs.convert_objects(convert_numeric=True)

global input_specs
input_specs = get_specs_for(model.inputs)

global intermediate_output_specs
intermediate_outputs = []
for l in model.layers:
	for idx in range (0, len(l.inbound_nodes)):
		o = l.get_output_at(idx)
		if o not in model.outputs:
			intermediate_outputs.append(o)
intermediate_output_specs = get_specs_for(intermediate_outputs)

global output_specs
output_specs = get_specs_for(model.outputs)
