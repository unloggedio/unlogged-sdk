import os
import sys
from Target import Target, ReplayTest
from configEnum import buildSystem, TestResult
import subprocess

def replay_target (target):
	
	# clone repo
	os.system("git clone " + target.test_repo_url)
	
	# modify build
	if (target.buildSystem == buildSystem.MAVEN):
		target.modify_pom(sdk_version, False)
	elif (target.buildSystem == buildSystem.GRADLE):
		target.modify_gradle(sdk_version, False)
	
	# server start
	docker_up_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
	val_1 = os.system(docker_up_cmd)

	proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
	(out_stream, err_stream) = proc.communicate()
	docker_container_id = "target-repo"

	# target replay
	if (target.buildSystem == buildSystem.MAVEN):
		test_command = "docker exec " + docker_container_id + " ./mvnw surefire:test --fail-never"
	elif (target.buildSystem == buildSystem.GRADLE):
		test_command = "docker exec " + docker_container_id + " ./gradlew test"
	val_2 = os.system(test_command)

	print ("--------")
	print ("out_stream = " + str(out_stream))
	print ("err_stream = " + str(err_stream))
	print ("docker_up_cmd = " + docker_up_cmd)
	print ("val_1 = " + str(val_1))
	print ("test_command = " + test_command)
	print ("val_2 = " + str(val_2))
	print ("--------")
	response_code = val_1 or val_2
	if (response_code == 0):
		print ("Test for target executed successfully: " +  target.test_repo_name)
	else:
		raise Exception("Test for target failed execution: " + target.test_repo_name)

	# assert and clean repo
	target.check_replay()
	docker_down_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml down"
	os.system(docker_down_cmd)
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
