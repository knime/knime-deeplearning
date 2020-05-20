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
@author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
'''


def is_keras_available(backend):
    try:
        import keras
        if backend == 'tensorflow':
            return is_tf1_available()
    except:
        return False


def is_tf2_available():
    return is_tf_version_available(2)


def is_tf1_available():
    return is_tf_version_available(1)


def is_tf_version_available(major_version):
    try:
        import tensorflow
        import TF2Network
        major_installed_version = int(tensorflow.__version__.split('.')[0])
        return major_installed_version == major_version
    except:
        return False


def read_keras(backend, path, compile, compatibility_mode):
    return {
        'tensorflow': read_keras_tensorflow,
        'cntk': read_keras_cntk,
        'theano': read_keras_theano
    }[backend](path, compile, compatibility_mode)


def read_keras_tensorflow(path, compile, compatibility_mode):
    from DLKerasTensorFlowNetwork import DLKerasTensorFlowNetworkReader
    return DLKerasTensorFlowNetworkReader().read(path, compile=compile,
                                                 compatibility_mode=compatibility_mode)


def read_keras_cntk(path, compile, compatibility_mode):
    from DLKerasCNTKNetwork import DLKerasCNTKNetworkReader
    return DLKerasCNTKNetworkReader().read(path, compile=compile,
                                           compatibility_mode=compatibility_mode)


def read_keras_theano(path, compile, compatibility_mode):
    from DLKerasTheanoNetwork import DLKerasTheanoNetworkReader
    return DLKerasTheanoNetworkReader().read(path, compile=compile,
                                             compatibility_mode=compatibility_mode)


def read_tf2(path, compile):
    from TF2Network import TF2NetworkReader
    return TF2NetworkReader().read(path, compile=compile)


class DLTF2NetworkReader(object):

    def __init__(self, backend):
        self.backend = backend

    def read(self, path, compile=True, compatibility_mode=False):
        # If keras is available we just use it and call the real reader
        if is_keras_available(self.backend):
            return read_keras(self.backend, path, compile, compatibility_mode)
        elif is_tf2_available():
            return read_tf2(path, compile)
        else:
            raise ValueError("Neither Keras nor TensorFlow 2 is available in the Python environment. " + \
                             "One of the libraries is needed to load the model.")
