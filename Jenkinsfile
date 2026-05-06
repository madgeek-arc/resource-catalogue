pipeline {
  agent any

  environment {
    IMAGE_NAME = "resource-catalogue"
    REGISTRY = "docker.madgik.di.uoa.gr"
    REGISTRY_CRED = 'docker-registry'
    DOCKER_IMAGE = ''
    DOCKER_TAG = ''
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

    stage('Build Image') {
      steps{
        script {
          DOCKER_IMAGE = docker.build("${REGISTRY}/${IMAGE_NAME}:${DOCKER_TAG}", "--build-arg profile=beyond .")
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
  // post-build actions
  post {
    always {
      script {
        if (DOCKER_IMAGE) {
          sh "docker rmi -f \$(docker inspect --format='{{.Id}}' ${DOCKER_IMAGE.id})"
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
