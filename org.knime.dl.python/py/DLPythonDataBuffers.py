# -*- coding: utf-8 -*-

# base
class DLPythonDataBuffer(object):  
	def __init__(self, array):
		"""
		Creates a new buffer that simply wraps a numpy.ndarray.
		:param array: The numpy.ndarray.
		"""
		self.array = array

	def __len__(self):
		return len(self.array)

	def __str__(self):
		return str(self.array)

# NB: the classes below are needed to unambiguously associate a buffer with its matching (de)serializer

# double
class DLPythonDoubleBuffer(DLPythonDataBuffer):
	def __init__(self, array):
		"""
		Creates a new double buffer that simply wraps a numpy.ndarray.
		:param array: The numpy.ndarray.
		"""
		super(DLPythonDoubleBuffer, self).__init__(array)

# float
class DLPythonFloatBuffer(DLPythonDataBuffer):
	def __init__(self, array):
		"""
		Creates a new float buffer that simply wraps a numpy.ndarray.
		:param array: The numpy.ndarray.
		"""
		super(DLPythonFloatBuffer, self).__init__(array)

# int
class DLPythonIntBuffer(DLPythonDataBuffer):
	def __init__(self, array):
		"""
		Creates a new int buffer that simply wraps a numpy.ndarray.
		:param array: The numpy.ndarray.
		"""
		super(DLPythonIntBuffer, self).__init__(array)

# long
class DLPythonLongBuffer(DLPythonDataBuffer):
	def __init__(self, array):
		"""
		Creates a new long buffer that simply wraps a numpy.ndarray.
		:param array: The numpy.ndarray.
		"""
		super(DLPythonLongBuffer, self).__init__(array)

