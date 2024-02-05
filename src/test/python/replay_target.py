import os
import sys
from Target import Target, ReplayTest
from configEnum import buildSystem, TestResult
import subprocess

# TODO: add docker down 
# TODO: check all the scripts
# TODO: test for compile target 
# TODO: test for replay target
def replay_target (target):
	
	# clone target
	os.system("git clone " + target.test_repo_url)
	
	# modify build system file
	if (target.buildSystem == buildSystem.MAVEN):
		
		# start docker 
		docker_command = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
		val_1 = os.system(docker_command)
		print ("pipeline_log: [replay_target] docker is started")
		print ("pipeline_log: [replay_target] val_1 = " + str(val_1))

		# modify pom.xml
		target.modify_pom(sdk_version, True)

		# run test
		docker_container_id = target.get_docker_container_id()
		print ("pipeline_log: [replay_target] docker_container_id = " + docker_container_id)

		test_command = "docker exec -it " + docker_container_id + " mvn test --fail-never"
		val_2 = os.system(test_command)
		print ("pipeline_log: [replay_target] test_command = " + test_command)
		print ("pipeline_log: [replay_target] val_2 = " + str(val_2))

		# run command and get response
		response_code = val_1 or val_2


	# elif (target.buildSystem == buildSystem.GRADLE):
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
			buildSystem.MAVEN,
			[
				ReplayTest("test getAllCustomers returns expected value when", TestResult.PASS),
				ReplayTest("test getCustomerProfile returns expected value when", TestResult.FAIL)
			]
		)
	]
		
	for local_target in target_list:
		replay_target(local_target)
