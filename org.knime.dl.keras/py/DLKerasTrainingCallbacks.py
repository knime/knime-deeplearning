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
import sys

from keras.callbacks import Callback
from keras.callbacks import EarlyStopping
from keras.callbacks import ReduceLROnPlateau
from keras.callbacks import TerminateOnNaN


class DLKerasAbstractTrainingCallback(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self):
        self._send_to_java = None

    @property
    def send_to_java(self):
        return self._send_to_java

    @send_to_java.setter
    def send_to_java(self, send_to_java):
        self._send_to_java = send_to_java


class DLKerasEarlyStopping(EarlyStopping, DLKerasAbstractTrainingCallback):
    def __init__(self, monitor='val_loss', min_delta=0, patience=0, verbose=0, mode='auto'):
        super().__init__(monitor, min_delta, patience, verbose, mode)

    def on_train_end(self, logs=None):
        super().on_train_end(logs)
        if self.stopped_epoch > 0:
            sys.stdout.flush()  # flush Keras info message
            self.send_to_java('early_stopping', self.stopped_epoch)


class DLKerasReduceLROnPlateau(ReduceLROnPlateau, DLKerasAbstractTrainingCallback):
    def __init__(self, monitor='val_loss', factor=0.1, patience=10, verbose=0, mode='auto', epsilon=1e-4, cooldown=0,
                 min_lr=0):
        super().__init__(monitor, factor, patience, verbose, mode, epsilon, cooldown, min_lr)

    def on_epoch_end(self, epoch, logs=None):
        super().on_epoch_end(epoch, logs)
        sys.stdout.flush()  # flush Keras info message
        # NB: do nothing for the moment, this is a placeholder (forward compatibility)


class DLKerasTerminateOnNaN(TerminateOnNaN, DLKerasAbstractTrainingCallback):
    def __init__(self):
        super().__init__()

    def on_batch_end(self, batch, logs=None):
        already_stopped = self.model.stop_training
        super().on_batch_end(batch, logs)
        if self.model.stop_training and not already_stopped:
            sys.stdout.flush()  # flush Keras info message
            self.send_to_java('terminate_on_nan', batch)


class DLKerasTrainingMonitor(Callback, DLKerasAbstractTrainingCallback):
    def __init__(self, network):
        super().__init__()
        self._network = network
        self._stop_training = False

    def stop_early(self):
        self._stop_training = True
        self._network.model.stop_training = True

    def on_train_begin(self, logs=None):
        # metrics_names = self.params['metrics']
        # self._metrics = pd.DataFrame(index=[0], columns=metrics_names)
        self._stop_training = False

    def on_train_end(self, logs=None):
        if self._stop_training:
            # flush pending Keras logs before printing our own status message
            sys.stdout.flush()
            print('Training was stopped by the user.')

    def on_epoch_begin(self, epoch, logs=None):
        self.send_to_java('epoch_begin')

    def on_epoch_end(self, epoch, logs=None):
        if logs:
            loss = logs.get('val_loss')
            acc = logs.get('val_acc')
            if acc is None:
                # Multi-output networks only have an accuracy metric per output. Average over them and use the result as
                # accuracy for the entire network. TODO: Note that this is a temporary workaround. Per-output metric
                # reporting is pending.
                accs = [v for k, v in logs.items() if k.startswith('val_') and k.endswith('_acc')]
                len_accs = len(accs)
                if len_accs > 0:
                    acc = sum(accs) / len_accs

            self.send_to_java('epoch_end', str(acc) + ';' + str(loss))

    def on_batch_begin(self, batch, logs=None):
        self.send_to_java('batch_begin')

    def on_batch_end(self, batch, logs=None):
        if logs:
            loss = logs.get('loss')
            acc = logs.get('acc')
            if acc is None:
                # Multi-output networks only have an accuracy metric per output. Average over them and use the result as
                # accuracy for the entire network. TODO: Note that this is a temporary workaround. Per-output metric
                # reporting is pending.
                accs = [v for k, v in logs.items() if k.endswith('_acc')]
                acc = sum(accs) / len(accs)

            self.send_to_java('batch_end', str(acc) + ';' + str(loss))
