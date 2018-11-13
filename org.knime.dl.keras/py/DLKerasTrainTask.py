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

"""
@author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
@author Christian Dietz, KNIME GmbH, Konstanz, Germany
"""

import DLPythonKernelGateway
from messaging.AbstractTaskHandler import AbstractTaskHandler
from messaging.Message import Message
from messaging.Message import PayloadEncoder
from messaging.Task import Task


class DLKerasTrainTask(Task):
    def __init__(self, reply_to, network, training_data_supplier, validation_data_supplier=None):
        self._kernel = DLPythonKernelGateway.global_workspace()['workspace']
        self._commands = self._kernel._commands
        self._messaging = self._commands._messaging
        super(DLKerasTrainTask, self).__init__(None, None, self._messaging, self._messaging,
                                               self._messaging.create_receive_queue(),
                                               self._messaging.create_next_message_id,
                                               self._kernel, _SynchronousExecutor())
        self._reply_to = str(reply_to)
        self._network = network
        training_data_supplier.request_from_java = self.request_from_java
        self._training_data_supplier = training_data_supplier
        if validation_data_supplier is not None:
            validation_data_supplier.request_from_java = self.request_from_java
        self._validation_data_supplier = validation_data_supplier

    def send_to_java(self, message_category, payload=None):
        message = self._create_message(message_category, payload)
        self._messaging.send(message)

    def request_from_java(self, message_category, payload=None):
        message = self._create_message(message_category, payload)
        self._commands.create_task(DLKerasTrainTask._RequestTaskHandler(), message).get()
        return

    def _create_message(self, message_category, payload=None):
        payload = PayloadEncoder().put_string(str(payload)).payload if payload is not None else None
        return Message(self._message_id_supplier(), self._reply_to, payload,
                       {AbstractTaskHandler.FIELD_KEY_MESSAGE_TYPE: message_category})

    def _run_internal(self):
        history = self._network.train(self._training_data_supplier,
                                      validation_data_supplier=self._validation_data_supplier,
                                      send_to_java=self.send_to_java)
        self._set_result(history)

    class _RequestTaskHandler(AbstractTaskHandler):
        def _handle_success_message(self, message):
            return None


# TODO: Remove. This was copied from knime-python. Tasks should not need to hold an executor.
class _SynchronousExecutor(object):
    """
    Dummy executor that mimics a part of the interface of Python 3 futures.ThreadPoolExecutor.
    """

    def __init__(self):
        self._shutdown = False

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    def submit(self, fn, *args, **kwargs):
        """
        Immediately computes the given function using the given arguments. Blocks until computation is completed.
        """
        if self._shutdown:
            raise RuntimeError('cannot schedule new futures after shutdown')
        future = _SynchronousExecutor._ImmediatelyCompletingFuture(fn, *args, **kwargs)
        if future.exception() is not None:
            raise future.exception()
        else:
            return future

    def shutdown(self, wait=True):
        self._shutdown = True

    class _ImmediatelyCompletingFuture(object):
        """
        Dummy future that mimics a part of the interface of Python 3 _base.Future.
        Immediately computes the given function using the given arguments. Blocks until computation is completed.
        """

        def __init__(self, fn, *args, **kwargs):
            self._result = None
            self._exception = None
            try:
                result = fn(*args, **kwargs)
            except BaseException as ex:
                self._exception = ex
            else:
                self._result = result

        def result(self, timout=None):
            if self._exception:
                raise self._exception
            else:
                return self._result

        def exception(self, timeout=None):
            return self._exception
