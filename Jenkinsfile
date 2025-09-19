#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-12'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream("knime-python-legacy/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
            ", knime-distance/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters([
        ignoreConfiguration: ['macosx-aarch']
    ])),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.deeplearning', 'maven && python3 && java17')

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-conda',
                'knime-datageneration',
                'knime-deeplearning',
                'knime-distance',
                'knime-ensembles',
                'knime-filehandling',
                'knime-jep',
                'knime-jfreechart',
                'knime-kerberos',
                'knime-python-legacy',
                'knime-streaming',
                'knime-tensorflow'
            ]
        ],
        ignoreConfiguration: ['macosx-aarch']
    )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
