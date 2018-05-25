# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME AG, Zurich, Switzerland
#  Website: http://www.knime.com; Email: contact@knime.com
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
#  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

'''
@author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
@author Christian Dietz, KNIME GmbH, Konstanz, Germany
'''

import abc
import keras

from DLKerasNetwork import DLKerasNetworkSpec
from DLKerasNetwork import DLKerasTrainingConfig
from DLPythonNetwork import DLPythonTensorSpec


class DLKerasNetworkSpecExtractor(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self, model):
        self._model = model
        self._model_inputs = model.inputs
        self._model_outputs = model.outputs

        self._input_specs = [None] * len(self._model_inputs)
        self._intermediate_output_specs = list()
        self._output_specs = [None] * len(self._model_outputs)

        # tensors:
        self._visited_inputs = set()
        self._visited_outputs = set()

        self._output_layer_tensor_ids = {}

        self._dimension_order = self._determine_dimension_order()

    '''
    Returns the tensor's element type as string.
    The string must comply with the string representation of NumPy data types. E.g. 'float32', 'float64', ...
    '''
    @abc.abstractmethod
    def _get_tensor_element_type(self, tensor):
        raise NotImplementedError()

    def extract_spec(self):
        model = self._model

        # tensor specs:
        for layer in model.layers:
            # "inbound_nodes" became private API with Keras 2.1.3
            inbound_nodes = layer.inbound_nodes if hasattr(layer, 'inbound_nodes') else layer._inbound_nodes
            for node_idx in range(0, len(inbound_nodes)):
                self._extract_node_input_tensor_specs(layer, node_idx)
                self._extract_node_output_tensor_specs(layer, node_idx)
        assert all(spec is not None for spec in self._input_specs)
        assert all(spec is not None for spec in self._intermediate_output_specs)
        assert all(spec is not None for spec in self._output_specs)

        # training configuration:
        training_config = None
        from keras.models import Sequential
        if isinstance(model, Sequential):
            model = model.model
        if hasattr(model, 'optimizer') and hasattr(model, 'loss') and hasattr(model, 'metrics'):
            # optimizer:
            optimizer = model.optimizer
            # loss functions:
            losses = self._extract_losses()
            # metrics:
            metrics = self._extract_metrics()

            training_config = DLKerasTrainingConfig()
            training_config.optimizer = optimizer
            training_config.loss = losses
            training_config.metrics = metrics
        # else, no training configuration is available

        return DLKerasNetworkSpec(self._input_specs, self._intermediate_output_specs, self._output_specs,
                                  training_config)

    def _determine_dimension_order(self):
        data_format = self._determine_data_format()
        if data_format is 'channels_first':
            return 'TCDHW'
        else:
            return 'TDHWC'

    def _determine_data_format(self):
        data_formats = []
        for layer in self._model.layers:
            if hasattr(layer, 'data_format'):
                data_formats.append(layer.data_format)
        if len(data_formats) == 0:
            # use data format specified in keras config
            return keras.backend.image_data_format()
        elif len(set(data_formats)) > 1:
            raise ValueError("The network contains conflicting data_formats.")
        else:
            # we checked that data_formats is not empty and all data_formats are the same
            return data_formats[0]

    def _extract_node_input_tensor_specs(self, layer, node_idx):
        input_tensors = layer.get_input_at(node_idx)
        input_shapes = layer.get_input_shape_at(node_idx)
        # some layers have multiple inputs, some do not
        if not isinstance(input_tensors, list):
            input_tensors = [input_tensors]
            input_shapes = [input_shapes]
        for tensor_idx, input_tensor in enumerate(input_tensors):
            if input_tensor in self._model_inputs and input_tensor not in self._visited_inputs:
                self._visited_inputs.add(input_tensor)
                tensor_shape = input_shapes[tensor_idx]
                tensor_spec = self._create_tensor_spec(layer, node_idx, tensor_idx, input_tensor,
                                                       tensor_shape, self._dimension_order)
                # preserve order of input tensors
                self._input_specs[self._model_inputs.index(input_tensor)] = tensor_spec

    def _extract_node_output_tensor_specs(self, layer, node_idx):
        output_tensors = layer.get_output_at(node_idx)
        output_shapes = layer.get_output_shape_at(node_idx)
        # some layers have multiple outputs, some do not
        if not isinstance(output_tensors, list):
            output_tensors = [output_tensors]
            output_shapes = [output_shapes]
        for tensor_idx, output_tensor in enumerate(output_tensors):
            if output_tensor not in self._visited_outputs:
                self._visited_outputs.add(output_tensor)
                tensor_shape = output_shapes[tensor_idx]
                tensor_spec = self._create_tensor_spec(layer, node_idx, tensor_idx, output_tensor,
                                                       tensor_shape, self._dimension_order)
                if output_tensor in self._model_outputs:
                    # preserve order of output tensors
                    self._output_specs[self._model_outputs.index(output_tensor)] = tensor_spec
                    self._output_layer_tensor_ids.setdefault(layer.name, []).append(tensor_spec.identifier)
                else:
                    self._intermediate_output_specs.append(tensor_spec)

    def _create_tensor_spec(self, layer, node_idx, tensor_idx, tensor, shape, dimension_order):
        # back end independent 'canonical' id
        # equals the naming scheme in org.knime.dl.keras.util.DLKerasUtils on Java side
        id = layer.name + '_' + str(node_idx) + ':' + str(tensor_idx)
        # back end dependent tensor name
        if hasattr(tensor, 'name') and tensor.name:
            name = tensor.name
        else:
            name = id
        element_type = self._get_tensor_element_type(tensor)
        return DLPythonTensorSpec(id, name, shape[0], list(shape[1:]), element_type, dimension_order)

    def _extract_losses(self):
        # Can be either a string, function, list or dict according to the Keras API. In the end, we want a dictionary
        # that maps an output tensor id to a loss.
        if isinstance(self._model.loss, str) or callable(self._model.loss):
            # normalize to list
            losses = [self._model.loss] * len(self._output_specs)
        else:
            losses = self._model.loss
        if not isinstance(losses, dict):
            # must be a list, normalize to dict
            losses = {self._output_specs[i].identifier: losses[i] for i in range(len(self._output_specs))}
        else:
            # Keras stores dicts that map an output layer name to a loss. We want an output tensor id.
            temp = {}
            for layer_name, loss in losses.items():
                for tensor_id in self._output_layer_tensor_ids[layer_name]:
                    temp[tensor_id] = loss
            losses = temp
        import keras.losses as kl
        for tensor, loss in losses.items():
            losses[tensor] = kl.get(loss).__name__
        return losses

    def _extract_metrics(self):
        # We want a dictionary that maps an output tensor id to a list of metrics.
        metrics = self._model.metrics
        if metrics is None:
            metrics = {}
        if not isinstance(metrics, dict):
            metrics = {self._output_specs[i].identifier: metrics for i in range(len(self._output_specs))}
        else:
            # Keras stores dicts that map an output layer name to a (list of) metric(s).
            # We want an output tensor id.
            temp = {}
            for layer_name, metric in metrics.items():
                if isinstance(metric, str) or callable(metric):
                    # normalize to list
                    metric = [metric]
                for tensor_id in self._output_layer_tensor_ids[layer_name]:
                    temp[tensor_id] = metric
            metrics = temp
            import keras.metrics as km
            for tensor, metric in metrics.items():
                for idx, met in enumerate(metric):
                    metrics[tensor][idx] = km.get(met).__name__ if met != 'accuracy' and met != 'acc' else 'acc'
        return metrics
