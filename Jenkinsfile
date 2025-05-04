pipeline {
  agent any

  environment {
    IMAGE_NAME = "resource-catalogue"
    REGISTRY = "docker.madgik.di.uoa.gr"
    REGISTRY_CRED = 'docker-registry'
    DOCKER_IMAGE = ''
    VERSION = ''
  }
  stages {
    stage('Read Project Version') {
      steps {
        script {
          VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
        }
      }
    }
    stage('Building image') {
      steps{
        script {
          DOCKER_IMAGE = docker.build("${REGISTRY}/${IMAGE_NAME}", "--build-arg profile=beyond .")
        }
      }
    }
    stage('Upload Image') {
      when {
        expression {
          return env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop'
        }
      }
      steps{
        script {
          if (env.BRANCH_NAME == 'master') { // pushes 'prod' and 'beta' images
            TAG = sh(script: "echo \"${VERSION}\" | sed -e 's/SNAPSHOT/beta/g'", returnStdout: true).trim()
          } else if (env.BRANCH_NAME == 'develop') { // pushes 'develop' images
            TAG = "dev"
          }
          withCredentials([usernamePassword(credentialsId: "${REGISTRY_CRED}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
              sh """
                  echo "Pushing image: ${REGISTRY}/${IMAGE_NAME}:${TAG}"
                  echo "$DOCKER_PASS" | docker login ${REGISTRY} -u "$DOCKER_USER" --password-stdin
              """
              DOCKER_IMAGE.push("${TAG}")
              sh "docker rmi ${DOCKER_IMAGE.id}"
          }
        }
      }
    }
  }
  // post-build actions
  post {
    success {
      echo 'Build Successful'
    }
    failure {
      echo 'Build Failed'
    }
  }
}
