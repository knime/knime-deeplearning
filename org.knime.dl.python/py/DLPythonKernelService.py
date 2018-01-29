# -*- coding: utf-8 -*-
from werkzeug.exceptions import NotImplemented

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
import math
import warnings

try:
    import threading
except ImportError:
    warnings.warn("Failed to import threading module. Using dummy_threading instead.")
    import dummy_threading as threading


class DLPythonKernelService(object):
    def __init__(self, workspace, java_send_func):
        assert workspace is not None
        assert java_send_func is not None
        self._workspace = workspace
        self._java_send_func = java_send_func
        self._java_send_lock = threading.Lock()

    @property
    def workspace(self):
        return self._workspace

    def send_to_java(self, msg):
        if msg.is_data_request():
            # async requests are not supported by the current messaging implementation of the Python kernel
            with self._java_send_lock:
                return self._java_send_func(msg)
        else:
            # should always return None, but let's stay defensive here
            return self._java_send_func(msg)


class DLPythonNetworkInputBatchGenerator(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self, input_names, target_names, steps, batch_size, kernel_service=None):
        assert len(input_names) > 0
        assert len(target_names) > 0
        assert steps > 0
        assert batch_size > 0
        self._input_names = input_names
        self._target_names = target_names
        self._size = steps * batch_size
        self._batch_size = batch_size
        self._steps = steps
        self._kernel_service = kernel_service

    @property
    def input_names(self):
        return self._input_names

    @property
    def target_names(self):
        return self._target_names

    @property
    def size(self):
        return self._size

    @property
    def batch_size(self):
        return self._batch_size

    @property
    def steps(self):
        return self._steps

    @property
    def kernel_service(self):
        return self._kernel_service

    def get_generator(self):
        i = 0
        while True:
            if i == self._steps:
                i = 0
            try:
                batch = self._get_batch(i)
            except Exception as e:
                warnings.warn("An exception of type " + str(type(e)) +
                              " occurred while fetching the next network input batch.\nCause: " + str(e))
                raise
            i += 1
            yield batch

    @abc.abstractmethod
    def _get_batch(self, batch_index):
        raise NotImplementedError()
