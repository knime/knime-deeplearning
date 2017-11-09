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

'''
@author Marcel Wiedenmann, KNIME, Konstanz, Germany
@author Christian Dietz, KNIME, Konstanz, Germany
'''

class DLPythonInstallationTester(object):

    # TODO: this class duplicates code of PythonKernelTester.py.
    # We should refactor the kernel tester for reusability and use that instead.
    
    def __init__(self):
        self._messages = []
    
    def get_report_lines(self):
        return list(self._messages)
    
    def check_lib(self, lib, cls=None, min_version=None, max_version=None):
        """
        Checks a specific library.

        :param lib: the library's name
        :param cls: a list of classes to check for availability 
        :param min_version: the minimum library version
        :param max_version: the maximum library version
        :returns: True if all constraints are fulfilled, False otherwise
        """
        
        if cls is None:
            cls = []
        error = False
        if not self._is_lib_available(lib):
            error = True
            msg = "Python library '" + lib + "' or one of its dependencies is missing."
            if min_version is not None:
                msg += ' Required minimum version is ' + min_version + '.'
            if max_version is not None:
                msg += ' Required maximum version is ' + max_version + '.'
            self._messages.append(msg)
        else:
            # TODO: check lib version bounds
            for cl in cls:
                if not self._is_class_available(lib, cl):
                    error = True
                    self._messages.append("Class '" + cl + "' in Python library '" + lib + "' or one of its dependencies is missing.")
        return not error
    
    def _is_lib_available(self, lib):
        """
        Checks if a specific library is available.
        
        :returns: True if the library is available, False otherwise.
        """
        
        local_env = {}
        exec('try:\n\timport ' + lib + '\n\tsuccess = True\nexcept:\n\tsuccess = False', {}, local_env)
        return local_env['success']
    
    def _is_class_available(self, lib, cls):
        """
        Checks if a specific class of a specific library is available.
        
        :param lib: the library name
        :param cls: the class name
        :returns: True if the class is available, False otherwise.
        """
        
        local_env = {}
        exec('try:\n\tfrom ' + lib + ' import ' + cls + '\n\tsuccess = True\nexcept:\n\tsuccess = False', {}, local_env)
        return local_env['success']
