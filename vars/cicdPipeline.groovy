def call(Map config) {
    pipeline {
        agent any

        environment {
            DOCKER_TAG = "${env.BUILD_NUMBER}"
        }

        stages {

            stage('Checkout Code') {
                steps {
                    script {
                        env.DOCKER_IMAGE_NAME = config.dockerImage
                        env.AWS_REGION        = config.awsRegion
                        env.ECR_URL           = config.ecrUrl
                        env.EKS_CLUSTER       = config.eksClusterName
                    }
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
                                echo "WARNING: Quality Gate failed, but continuing pipeline"
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


            stage('Create ECR Pull Secret') {
                steps {
                    script {
                        sh """
                        aws eks update-kubeconfig --region ${env.AWS_REGION} --name ${env.EKS_CLUSTER}
                        kubectl create secret docker-registry ecr-secret \
                          --docker-server=${env.ECR_URL} \
                          --docker-username=AWS \
                          --docker-password=\$(aws ecr get-login-password --region ${env.AWS_REGION}) \
                          --dry-run=client -o yaml | kubectl apply -f -
                        """
                    }
                }
            }


            stage('Push Docker Image') {
                steps {
                    sh """
                    aws ecr get-login-password --region ${env.AWS_REGION} | docker login --username AWS --password-stdin ${env.ECR_URL}
                    docker tag ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG} ${env.ECR_URL}/${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG}
                    docker push ${env.ECR_URL}/${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG}
                    """
                }
            }

            stage('Run Docker Container') {
              steps {
                script {
                sh """
                docker rm -f ${env.DOCKER_IMAGE_NAME} || true
                docker run -d --name ${env.DOCKER_IMAGE_NAME} -p 8081:8081 ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_TAG}
                """
                    }
               }
            }

            stage('Deploy to EKS') {
                steps {
                    script {
                        sh """
                        aws sts get-caller-identity
                        aws eks update-kubeconfig --region ${env.AWS_REGION} --name ${env.EKS_CLUSTER}
                        kubectl get nodes
                        kubectl apply -f deployment.yml
                        kubectl apply -f service.yml
                        kubectl get all
                        """
                    }
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
