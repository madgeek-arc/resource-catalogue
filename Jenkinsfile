pipeline {
  agent any

  environment {
    IMAGE_NAME = "resource-catalogue"
    REGISTRY = "docker.madgik.di.uoa.gr"
    REGISTY_CRED = 'docker-registry'
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
          DOCKER_IMAGE = docker.build("${IMAGE_NAME}", "--build-arg profile=beyond .")
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
            TAG = sh(script: 'echo "${VERSION}" | sed s/SNAPSHOT/beta/g', returnStdout: true).trim()
          } else if (env.BRANCH_NAME == 'develop') { // pushes 'develop' images
            TAG = "dev"
          }
          docker.withRegistry( REGISTRY, REGISTY_CRED ) {
            DOCKER_IMAGE.push(${TAG})
          }
          sh "docker rmi $REGISTRY/$IMAGE_NAME:$TAG"
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
