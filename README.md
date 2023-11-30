# KNIME® Deep Learning

[![Jenkins](https://jenkins.knime.com/buildStatus/icon?job=knime-deeplearning%2Fmaster)](https://jenkins.knime.com/job/knime-deeplearning/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KNIME_knime-deeplearning&metric=alert_status&token=55129ac721eacd76417f57921368ed587ad8339d)](https://sonarcloud.io/summary/new_code?id=KNIME_knime-deeplearning)

This repository is maintained by the [KNIME Team Rakete](mailto:team-rakete@knime.com).

This repository contains the plugins for the [KNIME Deep Learning Integration](https://www.knime.com/deeplearning) and the [KNIME Deep Learning - Keras Integration](https://www.knime.com/deeplearning/keras).

## Overview

This extension enables you to create deep neural networks, train them on training data and use them to predict on new data with the Keras API. Additionally, it provides core interfaces which can be used to integrate new deep learning frameworks into KNIME.

![Workflow Screenshot](https://files.knime.com/sites/default/files/img01.png)

## Content

This repository contains the source code for the [KNIME Deep Learning Integration](https://www.knime.com/deeplearning). The code is organized as follows:

* _org.knime.dl_: Generic deep learning framework
* _org.knime.dl.python_: Framework for Python based deep learning integrations
* _org.knime.dl.keras_: Keras Integration nodes

## Required Python Packages

* `keras` or `keras-gpu` (version: 2.1.6)

Additional information on how to install this extension and set up your python environment can be found on the [KNIME Deep Learning - Keras Website](https://www.knime.com/deeplearning/keras).

## Example Workflows

You can download the example workflows from the KNIME public example server (See [here how to connect...](https://www.knime.org/example-workflows)) or from the [KNIME node guide](https://www.knime.com/nodeguide/analytics/deep-learning).

## Development Notes

You can find instructions on how to work with our code or develop extensions for
KNIME Analytics Platform in the _knime-sdk-setup_ repository
on [BitBucket](https://bitbucket.org/KNIME/knime-sdk-setup)
or [GitHub](http://github.com/knime/knime-sdk-setup).

## Join the Community!

* [KNIME Forum](https://tech.knime.org/forum)
