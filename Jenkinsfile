#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2021-12'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream("knime-python/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
            ", knime-distance/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.deeplearning', 'maven && python3 && java11')

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-datageneration',
                'knime-deeplearning',
                'knime-distance',
                'knime-ensembles',
                'knime-filehandling',
                'knime-jep',
                'knime-jfreechart',
                'knime-kerberos',
                'knime-python',
                'knime-streaming',
                'knime-tensorflow'
            ]
        ]
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
