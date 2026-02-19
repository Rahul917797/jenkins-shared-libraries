def call(Map config) {

    pipeline {
        agent any

        environment {
            DOCKER_IMAGE_NAME = config.dockerImage
            DOCKER_TAG = "${env.BUILD_NUMBER}"
            AWS_REGION = config.awsRegion
            ECR_URL = config.ecrUrl
        }

        stages {

            stage('Checkout Code') {
                steps {
                    git branch: 'master', url: config.gitRepo
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
                    timeout(time: 2, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }

            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_TAG} ."
                }
            }

            stage('Push Docker Image') {
                steps {
                    sh """
                    # Authenticate Docker to ECR using IAM role
                    aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_URL}

                    # Tag and push Docker image
                    docker tag ${DOCKER_IMAGE_NAME}:${DOCKER_TAG} ${ECR_URL}/${DOCKER_IMAGE_NAME}:${DOCKER_TAG}
                    docker push ${ECR_URL}/${DOCKER_IMAGE_NAME}:${DOCKER_TAG}
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
