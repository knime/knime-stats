#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
	pipelineTriggers([
		upstream('knime-javasnippet/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-svg/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-expressions/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-base/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-core/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-shared/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-tp/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
		upstream('knime-js-core/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
	]),
	buildDiscarder(logRotator(numToKeepStr: '5')),
	disableConcurrentBuilds()
])

try {
	knimetools.defaultTychoBuild('org.knime.update.stats')

	/* workflowTests.runTests( */
	/* 	"org.knime.features.stats.feature.group", */
	/* 	false, */
    	/* ["knime-core", "knime-shared", "knime-tp", "knime-base"], */
	/* ) */

	/* stage('Sonarqube analysis') { */
	/* 	env.lastStage = env.STAGE_NAME */
	/* 	workflowTests.runSonar() */
	/* } */
 } catch (ex) {
	 currentBuild.result = 'FAILED'
	 throw ex
 } finally {
	 notifications.notifyBuild(currentBuild.result);
 }

/* vim: set ts=4: */
