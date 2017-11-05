# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME GmbH, Konstanz, Germany
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

'''
@author Marcel Wiedenmann, KNIME, Konstanz, Germany
@author Christian Dietz, KNIME, Konstanz, Germany
'''

import abc


_networks = {}


def get_network(identifier):
    return _networks[identifier]


def add_network(identifier, network):
    if identifier in _networks:
        raise ValueError("Network '" + identifier + "' already exists.")
    _networks[identifier] = network


def remove_network(identifier):
    if identifier in _networks:
        del _networks[identifier]


class DLPythonNetworkReader(object):
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def read(self, path):
        raise NotImplementedError()


class DLPythonNetwork(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self, model):
        self._model = model
        self._spec = None

    @property
    def model(self):
        return self._model

    @abc.abstractproperty
    def spec(self):
        raise NotImplementedError()

    @abc.abstractmethod
    def execute(self, in_data, batch_size):
        raise NotImplementedError()

    @abc.abstractmethod
    def save(self, path):
        raise NotImplementedError()


class DLPythonNetworkSpec(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self, input_specs, intermediate_output_specs, output_specs):
        self._input_specs = input_specs
        self._intermediate_output_specs = intermediate_output_specs
        self._output_specs = output_specs

    @abc.abstractproperty
    def network_type(self):
        return

    @property
    def input_specs(self):
        return self._input_specs

    @property
    def intermediate_output_specs(self):
        return self._intermediate_output_specs

    @property
    def output_specs(self):
        return self._output_specs


class DLPythonTensorSpec(object):

    def __init__(self, name, batch_size, shape, element_type):
        self._name = name
        self._batch_size = batch_size
        self._shape = shape
        self._element_type = element_type

    @property
    def name(self):
        return self._name

    @property
    def batch_size(self):
        return self._batch_size

    @property
    def shape(self):
        return self._shape

    @property
    def element_type(self):
        return self._element_type


class DLPythonTrainingConfig(object):
    __metaclass__ = abc.ABCMeta
    
    def __init__(self):
        pass
