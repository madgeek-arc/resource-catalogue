def DOCKER_IMAGE = null
def DOCKER_TAG = ''
def DOCKER_IMAGE_SHA = ''

pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds()
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
  }

  environment {
    IMAGE_NAME = "resource-catalogue"
    REGISTRY = "docker.madgik.di.uoa.gr"
    REGISTRY_CRED = 'docker-registry'
    DOCKER_BUILDKIT = '1'
  }

  stages {

    stage('Determine Docker Tag') {
      steps {
        script {
          DOCKER_TAG = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
          echo "Docker tag: ${DOCKER_TAG}"
          currentBuild.displayName = "${currentBuild.displayName}-${DOCKER_TAG}"
        }
      }
    }

    stage('Test and Build Image') {
      parallel {

        stage('Test') {
          when { expression { return env.TAG_NAME == null } }
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
              withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                sh 'mvn -B -T 1C verify -DnvdApiKey=$NVD_API_KEY -DfailBuildOnCVSS=11'
              }
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml'
              recordCoverage(
                tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml, **/target/site/jacoco-it/jacoco.xml, **/target/site/jacoco-aggregate/jacoco.xml']],
                sourceDirectories: [
                  [path: 'resource-catalogue-api/src/main/java'],
                  [path: 'resource-catalogue-elastic/src/main/java'],
                  [path: 'resource-catalogue-jms/src/main/java'],
                  [path: 'resource-catalogue-model/src/main/java'],
                  [path: 'resource-catalogue-model-lot1/src/main/java'],
                  [path: 'resource-catalogue-rest/src/main/java'],
                  [path: 'resource-catalogue-service/src/main/java']
                ]
              )
              archiveArtifacts allowEmptyArchive: true, artifacts: '**/dependency-check-report.*'
              dependencyCheckPublisher(
                pattern: '**/dependency-check-report.xml',
                failedTotalCritical: 1,
                unstableTotalHigh: 3
              )
            }
          }
        }

        stage('Build Image') {
          steps {
            script {
              DOCKER_IMAGE = docker.build("${REGISTRY}/${IMAGE_NAME}:${DOCKER_TAG}", "--build-arg profile=beyond --build-arg skipTests=true .")
              DOCKER_IMAGE_SHA = sh(script: "docker inspect --format='{{.Id}}' ${DOCKER_IMAGE.id}", returnStdout: true).trim()
            }
          }
        }

      }
    }

    stage('Upload Image') {
      when { // upload images only from 'develop' or 'master' branches and TAG builds
        expression {
          return env.TAG_NAME != null || env.BRANCH_NAME == 'develop' || env.BRANCH_NAME == 'master'
        }
      }
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: "${REGISTRY_CRED}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh """
              echo "\$DOCKER_PASS" | docker login ${REGISTRY} -u "\$DOCKER_USER" --password-stdin
            """
            DOCKER_IMAGE.push()
            if (env.TAG_NAME) {
              def minorTag = DOCKER_TAG.tokenize('.').take(2).join('.')
              DOCKER_IMAGE.push(minorTag)
              DOCKER_IMAGE.push("latest")
            } else if (DOCKER_TAG.endsWith('-SNAPSHOT')) {
              DOCKER_IMAGE.push("dev")
            }
          }
        }
      }
    }

    stage('Handle Releases') {
      when {
        allOf {
          branch 'master'
          not { changeRequest() }  // skip PR builds
        }
      }
      steps {
        lock(resource: "release-${IMAGE_NAME}") {
          retry(5) {
            script {
              try {
                withCredentials([string(credentialsId: 'jenkins-github-pat', variable: 'GH_TOKEN')]) {
                  sh '''
                    [ -f /etc/profile.d/load_nvm.sh ] || { echo "ERROR: /etc/profile.d/load_nvm.sh not found. NVM is required on this agent."; exit 1; }
                    . /etc/profile.d/load_nvm.sh
                    nvm install --lts
                    npx release-please@17 github-release --repo-url ${GIT_URL} --token ${GH_TOKEN}

                    npx release-please@17 release-pr --repo-url ${GIT_URL} --token ${GH_TOKEN}
                  '''
                }
              } catch (e) {
                sleep time: 45, unit: 'SECONDS'
                throw e
              }
            }
          }
        }
      }
    }

  }

  post {
    always {
      script {
        if (DOCKER_IMAGE_SHA) {
          sh "docker rmi -f ${DOCKER_IMAGE_SHA} || true"
        }
      }
    }
    failure {
      emailext(
        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """<p>Build <b>${env.JOB_NAME} #${env.BUILD_NUMBER}</b> failed.</p>
                 <p>Branch: <b>${env.BRANCH_NAME}</b></p>
                 <p>Check the details: <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>""",
        mimeType: 'text/html',
        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
        to: '$DEFAULT_RECIPIENTS'
      )
    }
    fixed {
      emailext(
        subject: "FIXED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """<p>Build <b>${env.JOB_NAME} #${env.BUILD_NUMBER}</b> is back to normal.</p>
                 <p>Branch: <b>${env.BRANCH_NAME}</b></p>
                 <p>Check the details: <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>""",
        mimeType: 'text/html',
        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
        to: '$DEFAULT_RECIPIENTS'
      )
    }
  }
}
