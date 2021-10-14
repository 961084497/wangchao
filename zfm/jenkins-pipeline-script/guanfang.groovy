node {
    // 指定环境变量
    environment {
        PROJECT_NAME = "测试项目"
    }

    // 参数变量调用
    stage('参数变量调用') {
        // 使用 echo 输出信息
        echo "引用变量或者参数：${env.PACKAGE_VERSION}"
    }
    
    // 拉取代码
    stage('GitLab 拉取代码') {
        git branch: 'develop', credentialsId: '19asd42c-2aee-4a2b-93dd-99dsdsxxb98', url: 'https://gitee.com/KU4NG/docsify-demo.git'
    }
    
    // 代码质量管理
    stage('代码质量管理') {
        tool name: 'SonarQube Scanner v4', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
        def SonarScannerHome = tool name: 'SonarQube Scanner v4'
        withSonarQubeEnv(credentialsId: '4f4d9adb-4505-42b8-8c93-4dddd36113dc') {
            sh "${SonarScannerHome}/bin/sonar-scanner -X "+
            "-Dsonar.host.url=http://192.168.200.101:9000 " +
            "-Dsonar.language=java " + 
            "-Dsonar.projectKey=PIPELINE-SONAR-TEST " + 
            "-Dsonar.projectName=PIPELINE-SONAR-TEST " + 
            "-Dsonar.sources=./ " + 
            "-Dsonar.sourceEncoding=UTF-8 " + 
            "-Dsonar.java.binaries=./ "    
        }
    }
    
    // 程序打包
    stage('程序打包') {
        // 指定全局工具
        tool name: 'JDK-8', type: 'jdk'
        def JAVA_HOME = tool name: 'JDK-8'
        // 局部变量重写
        withEnv('DEPLOY_PATH=/data/services', 'DEPLOY_PORT=latest']) {
            // 执行 shell 命令
            sh 'mvn clean install'
            // 等待 5 分钟，默认秒
            sleep time: 5, unit: 'MINUTES'
        }
    }
    
    // 发布到机器并存活性检测
    stage('发布到机器并存活性检测') {
        // 同类型多台服务器更新
        script {
            // 服务器名称列表
            def SERVER_LIST = ['192.168.200.101', '192.168.200.102', '192.168.200.103']
            for (EACH_SERVER in SERVER_LIST) {
                // SSH 发布
                sshPublisher(publishers: [sshPublisherDesc(configName: EACH_SERVER, transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: 'echo "Hello World"', execTimeout: 120000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: 'jenkins/', remoteDirectorySDF: false, removePrefix: 'target/', sourceFiles: 'target/ROOT.war')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)])
                
                // 存活性检测
                def SERVICE_PORT = 8080
                // 检测20次，每5秒检测一次
                for (int i = 1; i < 21; ++i) {
                    sleep 5
                    echo '检测节点' + EACH_SERVER
                    
                    try {
                        sh 'curl -s --connect-timeout 10 -m 10 http://' + EACH_SERVER + ':' + 8080
                        break
                    } catch (e) {
                        echo '节点检测失败，即将重试：' + EACH_SERVER
                        // 检测次数到达上限，检测失败
                        if (i == 20) {
                            echo '检测次数已达到上限，退出更新'
                            currentBuild.result = 'FAILURE'
                            throw e
                        }
                    }
                }
            }
        }
    }
    
    // 触发其它构建
    stage('触发其它构建') {
        build quietPeriod: 5, job: 'TEST-PROJECT-JAVA-BACKEND', parameters: [string(name: 'BUILD_ENV', value: 'product')]
    }    
}
