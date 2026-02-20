def call(Map config) {

    pipeline {
        agent any

        environment {
            // Only dynamic environment Jenkins allows: literals or env variables
            DOCKER_TAG = "${env.BUILD_NUMBER}"
        }

        stages {

            stage('Checkout Code') {
                steps {
                    script {
                        // Dynamic variables from config
                        env.DOCKER_IMAGE_NAME = config.dockerImage
                        env.AWS_REGION        = config.awsRegion
                        env.ECR_URL           = config.ecrUrl
                    }

                    // Checkout code
                    git branch: 'main', url: config.gitRepo
                }
            }

            stage('Maven Build') {
                steps {
                    sh 'mvn clean install -DskipTests'
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    withSonarQubeEnv('sonarqube') {
                        sh """
                        mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=${config.projectKey} \
                        -Dsonar.projectName=${config.projectName}
                        """
                    }
                }
            }

            stage('Quality Gate') {
    steps {
        script {
            timeout(time: 5, unit: 'MINUTES') {
                def qg = waitForQualityGate abortPipeline: false
                echo "Quality Gate status: ${qg.status}"
                if (qg.status != 'OK') {
                    echo "WARNING: Quality Gate failed, but continuing pipeline for practice"
                }
            }
        }
    }
}

            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG} ."
                }
            }

            stage('Push Docker Image') {
                steps {
                    sh """
                    # Authenticate Docker to ECR using IAM role
                    aws ecr get-login-password --region ${env.AWS_REGION} | docker login --username AWS --password-stdin ${env.ECR_URL}

                    # Tag and push Docker image
                    docker tag ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG} ${env.ECR_URL}/${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG}
                    docker push ${env.ECR_URL}/${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG}
                    """
                }
            }

            stage('Run Docker Container') {
              steps {
                 sh """
                 docker rm -f ${env.DOCKER_IMAGE_NAME} || true
                 docker run -d -p 8081:8081 --name ${env.DOCKER_IMAGE_NAME} ${env.ECR_URL}/${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG}
                 """
                 }
             }



        }

        post {
            success {
                echo "Pipeline successfully completed!"
            }
            failure {
                echo "Pipeline failed!"
            }
        }
    }
}
