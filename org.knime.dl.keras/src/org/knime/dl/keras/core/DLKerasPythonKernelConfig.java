package org.knime.dl.keras.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.util.DLUtils;

public class DLKerasPythonKernelConfig {

	public static final String MODEL_NAME = "model";

	public static final String INPUT_SPECS_NAME = "input_specs";

	public static final String INTERMEDIATE_OUTPUT_SPECS_NAME = "intermediate_output_specs";

	public static final String OUTPUT_SPECS_NAME = "output_specs";

	public static final String MODEL_WEIGHTS_NAME = "model_weights";

	public static final String INPUT_TABLE_NAME = "input_table";

	public static final String OUTPUT_TABLE_NAME = "output_table";

	private static final String MODEL_LOAD_PATH_NAME = "model_load_path";

	private static final String BUNDLE_ID = "org.knime.dl.keras";

	public String getTestKerasInstallationCode() throws IOException {
		final String snippetPath = getFile("/py/keras_test.py").getAbsolutePath();
		return readSnippet(snippetPath);
	}

	public String getLoadFromH5Code(final String loadPath) throws IOException {
		final String snippetPath = getFile("/py/model_from_h5.py").getAbsolutePath();
		final String snippet = prependModelLoadPath(loadPath, readSnippet(snippetPath));
		return snippet;
	}

	public String getLoadSpecFromJsonCode(final String loadPath) throws IOException {
		final String snippetPath = getFile("/py/model_from_json.py").getAbsolutePath();
		final String snippet = prependModelLoadPath(loadPath, readSnippet(snippetPath));
		return snippet;
	}

	public String getLoadSpecFromYamlCode(final String loadPath) throws IOException {
		final String snippetPath = getFile("/py/model_from_yaml.py").getAbsolutePath();
		final String snippet = prependModelLoadPath(loadPath, readSnippet(snippetPath));
		return snippet;
	}

	public String getLoadWeightsCode(final String loadPath) throws IOException {
		final String snippetPath = getFile("/py/model_weights_from_h5.py").getAbsolutePath();
		final String snippet = prependModelLoadPath(loadPath, readSnippet(snippetPath));
		return snippet;
	}

	public String getSaveToH5Code(final String savePath) throws IOException {
		final String snippetPath = getFile("/py/model_to_h5.py").getAbsolutePath();
		final String snippet = prependModelLoadPath(savePath, readSnippet(snippetPath));
		return snippet;
	}

	public String getExtractWeightsCode() throws IOException {
		final String snippetPath = getFile("/py/model_extract_weights.py").getAbsolutePath();
		return readSnippet(snippetPath);
	}

	public String getExtractSpecsCode() throws IOException {
		final String snippetPath = getFile("/py/model_extract_specs.py").getAbsolutePath();
		return readSnippet(snippetPath);
	}

	public String getSetBatchSizeCode(final long batchSize) throws IOException {
		final String snippetPath = getFile("/py/model_set_batch_size.py").getAbsolutePath();
		return readSnippet(snippetPath).trim().concat("" + batchSize);
	}

	public String getExecuteCode(final Set<DLLayerDataSpec> singleOutputColumnName) throws IOException {
		final String snippetPath = getFile("/py/model_execute.py").getAbsolutePath();
		// TODO: multiple columns are already possible on Python side, adapt here
		return "output_column_name = '" + singleOutputColumnName.stream().findFirst().get().getName() + "'\n"
				+ readSnippet(snippetPath);
	}

	private static File getFile(final String relativePath) throws IOException {
		return DLUtils.Files.getFileFromBundle(BUNDLE_ID, relativePath);
	}

	private static String readSnippet(final String snippetPath) throws IOException {
		return new String(Files.readAllBytes(Paths.get(snippetPath)), StandardCharsets.UTF_8);
	}

	private static String prependModelLoadPath(final String loadPath, final String snippetCode) {
		// NB: 'r' interprets the loadPath as raw string. This is needed to properly handle Windows path separators.
		return MODEL_LOAD_PATH_NAME + " = r'" + loadPath + "'\n" + snippetCode;
	}
}
