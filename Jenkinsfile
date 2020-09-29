#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

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
    knimetools.defaultTychoBuild('org.knime.update.deeplearning', 'maven && python3')

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-deeplearning', 'knime-python', 'knime-filehandling', 'knime-jfreechart', 'knime-distance',
                'knime-jep', 'knime-ensembles', 'knime-tensorflow', 'knime-datageneration', 'knime-streaming'
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
