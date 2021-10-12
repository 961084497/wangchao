#!/usr/bin/env groovy
@Library('shareMaven') _
manager.addShortText("$env.BRANCH_NAME")
node('master') {
        wrap([$class: 'BuildUser']) {
        def user_user = env.BUILD_USER_ID
            currentBuild.description = "projects:${env.Project_name} user:${user_user}"
        }

        stage("第一阶段") {
                sh "echo start pull code"
                def project_list_all =  "${env.Project_name}".toLowerCase().split(",")
                //println "${project_list_all}"
                //println  project_list_all.size()
                def Git_url_obj = readYaml file: '/var/jenkins_home/git_data/test_all_pipeline.yaml'
                //println Git_url_obj
                def tasks = [:]
                for (int i = 0; i < project_list_all.size(); i++) {
                                def index = i+0
                                def Git_url = Git_url_obj.Env.test.project."${project_list_all[index]}".giturl
                                def Branch = "${env.BRANCH_NAME}"
                                //println "${project_list_all[index]}"
                                // 这边会对发布项目做一个判断 区分apollo 等一些项目
                                def Grand_cmd = Git_url_obj.Env.test.project."${project_list_all[index]}".cmd
                                //def Host = Git_url_obj.Env.test.project."${project_list_all[index]}".host
                                                                        
                                //println "${index},${Git_url},${Branch}"
                                tasks["Task ${i}"] = {
                                        stage("${project_list_all[index]}拉取代码") {
                                                Git_pull(project_list_all[index],Git_url,Branch)
                                                //Mvn_cmd(Grand_cmd,project_list_all[index])
                                        }
                                }
                        }
                parallel(tasks)
        }

        stage("第二阶段") {
                def project_list_all =  "${env.Project_name}".toLowerCase().split(",")
                //println "${project_list_all}"
                //println  project_list_all.size()
                Fabu_fangshi = "${env.CI_CD}"
                def Git_url_obj = readYaml file: '/var/jenkins_home/git_data/test_all_pipeline.yaml'
                //println Git_url_obj
                def tasks = [:]
                for (int i = 0; i < project_list_all.size(); i++) {
                                def index = i+0
                                def Git_url = Git_url_obj.Env.test.project."${project_list_all[index]}".giturl
                                def Branch = "${env.BRANCH_NAME}"
                                //println "${project_list_all[index]}"
                                // 这边会对发布项目做一个判断 区分apollo 等一些项目
                                def Grand_cmd = Git_url_obj.Env.test.project."${project_list_all[index]}".cmd
                                def Migration__cmd = Git_url_obj.Env.test.project."${project_list_all[index]}".migrate
                                //def Host = Git_url_obj.Env.test.project."${project_list_all[index]}".host

                                //println "${index},${Git_url},${Branch}"
                                tasks["Task ${i}"] = {
                                        stage("${project_list_all[index]}数据库magrate同步且编译") { 
                                                //Git_pull(project_list_all[index],Git_url,Branch)
                                                if (Fabu_fangshi == "完整发布") {
                                                        Mvn_cmd(Grand_cmd,project_list_all[index])
                                                } else {
                                                Migrate_cmd(Migration__cmd,project_list_all[index])
                                                Mvn_cmd(Grand_cmd,project_list_all[index])
                                                }
                                        }
                                }
                        }
                parallel(tasks)
        }

        stage("第三阶段") {
                def project_list_all_sec  = "${env.Project_name}".toLowerCase().split(",")
                def Git_url_obj = readYaml file: '/var/jenkins_home/git_data/test_all_pipeline.yaml'
                def tasks = [:]
                for (int i = 0; i < project_list_all_sec.size(); i++) {
                                def index = i+0
                                def Host = Git_url_obj.Env.test.project."${project_list_all_sec[index]}".host
                                def Scp_dir_cmd = Git_url_obj.Env.test.project."${project_list_all_sec[index]}".scp_dir_cmd
                                //println "${index},${Git_url},${Branch}"
                                tasks["Task ${i}"] = {
                                        stage("${project_list_all_sec[index]}远端传输代码") {
                                                Scp_code(Scp_dir_cmd)
                                        }
                                }
                        }
                parallel(tasks)         
        }

        stage("第四阶段") {
                def project_list_all_third  = "${env.Project_name}".toLowerCase().split(",")
                def Git_url_obj = readYaml file: '/var/jenkins_home/git_data/test_all_pipeline.yaml'
                def tasks = [:]
                for (int i = 0; i < project_list_all_third.size(); i++) {
                                def index = i+0
                                def Start_cmd_sh = Git_url_obj.Env.test.project."${project_list_all_third[index]}".start_cmd_sh
                                def Host = Git_url_obj.Env.test.project."${project_list_all_third[index]}".host
                                def Check_ping = Git_url_obj.Env.test.project."${project_list_all_third[index]}".check_ping
                                //println "${index},${Git_url},${Branch}"
                                tasks["Task ${i}"] = {
                                        stage("${project_list_all_third[index]}启动服务和心跳检测") {
                                                Start_cmd(Start_cmd_sh,project_list_all_third[index],Host)
                                                sh "sleep 120"
                                                Check_ping_cmd(Check_ping)
                                        }
                                }
                        }
                parallel(tasks)         
        }

}

