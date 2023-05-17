#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-07'

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
        dependencies: [ repositories: [
            'knime-stats', 'knime-python-legacy', 'knime-filehandling', 'knime-r', 'knime-js-core',
            'knime-js-base', 'knime-database', 'knime-kerberos', 'knime-jep', 'knime-xml',
            'knime-pmml', 'knime-expressions', 'knime-ensembles', 'knime-distance',
            'knime-datageneration', 'knime-chromium', 'knime-conda', 'knime-cef'],
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
