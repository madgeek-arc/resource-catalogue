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
    stage('Create Image Tag') {
      steps {
        script {
          if (env.BRANCH_NAME == 'master') { // creates 'prod' and 'beta' image tags
            TAG = sh(script: "echo \"${VERSION}\" | sed -e 's/SNAPSHOT/beta/g'", returnStdout: true).trim()
          } else if (env.BRANCH_NAME == 'develop') { // creates 'develop' image tag
            TAG = "dev"
          } else { // creates tags for all other branches
            TAG = env.BRANCH_NAME.replaceAll('/', '-')
          }
        }
      }
    }
    stage('Build Image') {
      steps{
        script {
          DOCKER_IMAGE = docker.build("${REGISTRY}/${IMAGE_NAME}:${TAG}", "--build-arg profile=beyond .")
        }
      }
    }
    stage('Upload Image') {
      when { // upload images only from 'develop' or 'master' branches
        expression {
          return env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop'
        }
      }
      steps{
        script {
          withCredentials([usernamePassword(credentialsId: "${REGISTRY_CRED}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
              sh """
                  echo "Pushing image: ${DOCKER_IMAGE.id}"
                  echo "$DOCKER_PASS" | docker login ${REGISTRY} -u "$DOCKER_USER" --password-stdin
              """
              DOCKER_IMAGE.push()
          }
        }
      }
    }
    stage('Remove Image') {
      steps{
        script {
          sh "docker rmi ${DOCKER_IMAGE.id}"
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
