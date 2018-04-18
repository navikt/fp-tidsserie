import java.security.MessageDigest;

timestamps {
    def skipITests = '-DskipITs'
    def skipUTests = '-DskipUTs'
	def schemaNavn = ''
    def revision = ''
    def version = ''
    def sha = ''

    properties([disableConcurrentBuilds(), parameters([
            booleanParam(defaultValue: false, description: '', name: 'skip_UTests'),
            booleanParam(defaultValue: false, description: '', name: 'skip_ITests')])
            ])

    if (!params.skip_UTests) {
        skipUTests = ''
    }

    if (!params.skip_ITests) {
        skipITests = ''
    }

    node () {
        try {
            env.LANG = "nb_NO.UTF-8"

            stage("Init") {
                printStage("Init")
                info(schemaNavn)
                env.JAVA_HOME = "${tool 'jdk-1.8'}"
                env.PATH = "${tool 'default-maven'}/bin:${env.PATH}"
                step([$class: 'WsCleanup'])
                checkout scm

                revision = sh(returnStdout: true, script: 'cat .mvn/version').trim()
                commitHash = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                timestamp = new Date().format("yyyyMMddHHmmss", TimeZone.getTimeZone("UTC"))
                sha = '_' + timestamp + '_' + commitHash
                version = ' -Drevision="' + revision + '" -Dchangelist="" -Dsha1="' + sha + '" '

            }

            stage("Build") {
                printStage("Build")
                info("Build: " + revision + sha)
                configFileProvider(
                        [configFile(fileId: 'navMavenSettingsUtenProxy', variable: 'MAVEN_SETTINGS')]) {

                    println"-------------"
                    println("Versjon: " + revision + sha)
                    println"-------------"
                    mavenProps=" -Dfile.encoding=UTF-8 -Djava.security.egd=file:///dev/urandom -DinstallAtEnd=true -DdeployAtEnd=true "
                    sh 'mvn -B ' + version + ' -s $MAVEN_SETTINGS ' + skipUTests + ' ' + skipITests + ' ' + mavenProps + ' clean deploy'
                }

                if (!skipITests) {
                    publishHTML(target: [
                            allowMissing         : true,
                            alwaysLinkToLastBuild: false,
                            keepAll              : true,
                            reportDir            : '**/target/failsafe-reports',
                            reportFiles          : '*.html',
                            reportName           : "Failsafe Report"
                    ])
                }
            }

        } catch(error) {
            emailext (
                    subject: "[AUTOMAIL] Feilet jobb ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                    body: "<p>Hei,<br><br>har du til til å ta en titt på hva som kan være feil?<br>" +
                            "<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a><br><br>" +
                            "Tusen takk på forhånd,<br>Miljø</p>",
                    recipientProviders: [[$class: 'DevelopersRecipientProvider']]
            )
            throw error
        }
    }
}


void info(msg) {
    ansiColor('xterm') {
        println "\033[45m\033[37m " + msg + " \033[0m"
    }
    currentBuild.description = msg
}
void printStage(stage) {
    ansiColor('xterm') {
        println "\033[46m Entered stage " + stage + " \033[0m"
    }
}

@NonCPS
def String schema() {
	def s = env.BRANCH_NAME;
	return MessageDigest
	.getInstance("MD5")
	.digest(s.bytes)
	.encodeHex()
	.toString()
	.substring(0,15)
	.toLowerCase();
}
