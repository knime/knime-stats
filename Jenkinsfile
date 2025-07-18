#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2025-12'

library "knime-pipeline@$BN"

properties([
	pipelineTriggers([
		upstream("knime-javasnippet/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
		    ", knime-svg/${env.BRANCH_NAME.replaceAll('/', '%2F')}" + 
		    ", knime-js-core/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
	]),
    parameters(workflowTests.getConfigurationsAsParameters()),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

try {
	knimetools.defaultTychoBuild('org.knime.update.stats')

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-cef',
                'knime-conda',
                'knime-conda-channels',
                'knime-database',
                'knime-datageneration',
                'knime-distance',
                'knime-ensembles',
                'knime-expressions',
                'knime-filehandling',
                'knime-jep',
                'knime-js-base',
                'knime-js-core',
                'knime-kerberos',
                'knime-pmml',
                'knime-python',
                'knime-python-legacy',
                'knime-r',
                'knime-stats',
                'knime-scripting-editor',
                'knime-xml'
            ],
            ius: ['org.knime.features.browser.cef.feature.group']
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
