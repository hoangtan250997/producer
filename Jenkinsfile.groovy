node('built-in') {

    jdk = tool name: 'JDK-17'
    env.JAVA_HOME = "${jdk}"

    /* Stage checkout, will get the source code from git server */
    stage('Checkout') {
        checkout scm
        currentPomVersion = readMavenPom().getVersion() // Get current pom version after checkout the project
    }

    /* Stage build, build the project to generate jar file */
    stage('Build') {
        withMaven(maven: 'MAVEN') {
            sh "chmod +x mvnw"
            sh "./mvnw clean install -D maven.test.skip=true"
        }
    }

    stage('Check Sonar') {
        FAILED_STAGE = 'Check sonar'
        def SONAR_MODULE = "dms"

        def scannerHome = tool 'SonarQubeScanner'
        withSonarQubeEnv('SonarQube') {
            sh "${scannerHome}/bin/sonar-scanner -Dsonar.language=java -Dsonar.projectName=${env.JOB_BASE_NAME} -Dsonar.projectKey=${env.JOB_BASE_NAME} -Dsonar.tests=src/test/java -Dsonar.sources=src/main/java -Dsonar.java.libraries=/var/jenkins_home/.m2/repository/org/projectlombok/lombok/1.18.2/lombok-1.18.2.jar -Dsonar.java.binaries=. -Dsonar.java.coveragePlugin=jacoco "

        }

    }
    stage('Push image to docker registry') {
        /*
            Login to Docker registry and push this images was built in stage Buid
            docker-registry-user: The credential that defined on Jenkins server
        */

        if (params.TEST_OR_PRODUCTION == 'PROD') {
            print 'Build with production configuration.....'
            docker.withRegistry('https://aavn-registry.axonactive.vn.local/', 'docker-registry-user') {
                def dockerImage = docker.build("aavn-registry.axonactive.vn.local/my-tv-show-services/my-tvshow-services-image:${currentPomVersion}", "--build-arg CER_PATH=src/main/resources/keystore/axonactive.vn.local.cer -f Dockerfile .")
                dockerImage.push()
            }
        } else {
            print 'Build with staging configuration.......'
            docker.withRegistry('https://aavn-registry.axonactive.vn.local/', 'docker-registry-user') {
                def tvShowImage = docker.build("aavn-registry.axonactive.vn.local/my-tv-show-services/my-tvshow-services-image:${currentPomVersion}")
                tvShowImage.push()
            }
        }
        sh "docker rmi my-tv-show-services/my-tvshow-services-image:${currentPomVersion} -f || true"
        sh "docker rmi my-tv-show-services/my-tvshow-services-image:latest -f || true"
        sh "docker rmi aavn-registry.axonactive.vn.local/my-tv-show-services/my-tvshow-services-image:${currentPomVersion} || true"
    }

    stage('Pull and start new container') {
        /*
        Switch to Test server pull and run image from registry
        Before build, must stop and remove container that already run. then remove old image in the dev server
        After remove container and image, pull new image from registry and rerun the container
        */
        withCredentials([usernamePassword(credentialsId: SERVER_ACCOUNT, usernameVariable: 'serverUsername', passwordVariable: 'serverPassword')]) {
            def remote = [:]
            remote.name = SERVER_HOSTNAME
            remote.host = SERVER_HOSTNAME
            remote.user = serverUsername
            remote.password = serverPassword
            remote.allowAnyHosts = true
            sshCommand remote: remote, command: """docker stop ${CONTAINER_NAME} || true && docker rm ${CONTAINER_NAME} || true"""
            sshCommand remote: remote, command: """docker rmi aavn-registry.axonactive.vn.local/my-tv-show-services/my-tvshow-services-image:${currentPomVersion} -f || true"""
            withCredentials([usernamePassword(credentialsId: 'docker-registry-user', usernameVariable: 'dockerRegistryAccountName', passwordVariable: 'dockerRegistryAccountPassword'),
                             usernamePassword(credentialsId: DB_ACCOUNT, usernameVariable: 'dbAccountName', passwordVariable: 'dbAccountPassword'),
                             usernamePassword(credentialsId: SMB_ACCOUNT, usernameVariable: 'smbAccountName', passwordVariable: 'smbAccountPassword')]) {
                sshCommand remote: remote, command: """docker login aavn-registry.axonactive.vn.local --username ${dockerRegistryAccountName} --password '${dockerRegistryAccountPassword}'"""
                sshCommand remote: remote, command: """docker pull aavn-registry.axonactive.vn.local/my-tv-show-services/my-tvshow-services-image:${currentPomVersion}"""
                sshCommand remote: remote, command: """docker run -d --name ${CONTAINER_NAME} \
					-e TZ=Asia/Ho_Chi_Minh\
                    -e SPRING_APPLICATION_JSON='{
                        "spring.datasource.url": "${DB_URL}",
                        "spring.datasource.username": "${dbAccountName}",
                        "spring.datasource.password": "${dbAccountPassword}",
                        "spring.security.oauth2.resourceserver.jwt.issuer-uri": "${KEYCLOAK_SERVER_URI}",
                        "spring.security.oauth2.client.provider.keycloak.issuer-uri": "${KEYCLOAK_SERVER_URI}",
                        "spring.security.oauth2.client.registration.keycloak.client-id": "${KEYCLOAK_CLIENT_NAME}",
                        "my-employee-book.baseUri": "${MY_EMPLOYEE_BOOK_URI}",
                        "mytime.baseUri": "${MY_TIME_SERVICES_URI}"
                    }'\
   					-p ${SPRING_BOOT_PORT}:8080 \
                    -p ${SAYHI_SOCKET_SERVER_PORT}:4001\
					-v ${SERVER_LOG_FILE_PATH}:/tmp/logs \
					-v ${MEDIA_FOLDER_PATH}:/opt/resources/mytvshow \
					--restart unless-stopped \
					aavn-registry.axonactive.vn.local/my-tv-show-services/my-tvshow-services-image:${currentPomVersion}"""
            }
        }
    }
}




