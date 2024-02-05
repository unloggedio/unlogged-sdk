import os
import sys
from Target import Target, ReplayTest
from build_system import build_system
from TestResult import TestResult
import subprocess
	
def replay_target (target):
	
	# clone target
	os.system("git clone " + target.test_repo_url)
	
	# modify build system file
	if (target.build_system == build_system.MAVEN):
		# target.modify_pom(sdk_version, True)
		
		# get command for docker start
		docker_command = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
		
		# get command for test
		proc = subprocess.Popen(["docker container ls --all --quiet --filter 'name=conf-demo-app'"], stdout=subprocess.PIPE, shell=True)
		(out_stream, err_stream) = proc.communicate()
		docker_container_id = str(out_stream)
		docker_container_id = docker_container_id[2:][:-3]
		print ("docker_container_id = " + docker_container_id)
		test_command = "docker exec -it " + docker_container_id + " mvn test --fail-never"
		print (test_command)

		# run command and get response
		val_1 = os.system(docker_command)
		target.modify_pom(sdk_version, True)
		print ("val_1 = " + str(val_1))
		val_2 = os.system(test_command)
		print ("val_2 = " + str(val_2))
		
		response_code = val_1 or val_2
		

	# elif (target.build_system == build_system.GRADLE):
	# 	target.modify_gradle(sdk_version)
	# 	test_command = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
	
	# target replay
	if (response_code == 0):
		print ("Test for target executed successfully: " +  target.test_repo_name)
	else:
		raise Exception("Test for target failed execution: " + target.test_repo_name)

	# surefire-report assertion
	target.check_replay()

	# delete target
	os.system("rm -rf " + target.test_repo_name)


if __name__=="__main__":

	sdk_version = sys.argv[1]
	target_list = [
		#unlogged-spring-maven-demo
		Target(
			"https://github.com/unloggedio/unlogged-spring-maven-demo",
			"unlogged-spring-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java",
			build_system.MAVEN,
			[
				ReplayTest("test_name-1", TestResult.PASS),
				ReplayTest("test_name-2", TestResult.FAIL)
			]
		)
	]
		
	for local_target in target_list:
		replay_target(local_target)
