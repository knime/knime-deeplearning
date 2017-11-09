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

"""
@author Marcel Wiedenmann, KNIME, Konstanz, Germany
@author Christian Dietz, KNIME, Konstanz, Germany
"""

import abc

from DLPythonInstallationTester import DLPythonInstallationTester


_network_types = {}


def get_network_type(identifier):
    return _network_types[identifier]


def add_network_type(network_type):
    if network_type.identifier in _network_types:
        raise ValueError("Network type'" + network_type.identifier + "' already exists.")
    _network_types[network_type.identifier] = network_type


def remove_network_type(identifier):
    if identifier in _network_types:
        del _network_types[identifier]


def get_model_network_type(model):
    for _, network_type in _network_types.items():
        if network_type.supports_model(model):
            return network_type
    raise TypeError("No deep learning network type associated with Python type '" + str(type(model)) + "'.")


class DLPythonNetworkType(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self, identifier):
        self._identifier = identifier

    @property
    def identifier(self):
        return self._identifier

    @abc.abstractproperty
    def reader(self):
        raise NotImplementedError()

    def test_installation(self):
        tester = DLPythonInstallationTester()
        self._test_installation(tester)
        results = tester.get_report_lines()
        if results:
            # prepended message is expected on Java side - do not change
            results = "[DL Python installation test: FAIL]" + "\n".join(results)
        else:
            # message is expected on Java side - do not change
            results = "[DL Python installation test: OK]"
        return results

    @abc.abstractmethod
    def supports_model(self, model):
        raise NotImplementedError()

    @abc.abstractmethod
    def wrap_model(self, model):
        raise NotImplementedError()

    @abc.abstractmethod
    def _test_installation(self, tester):
        raise NotImplementedError()
