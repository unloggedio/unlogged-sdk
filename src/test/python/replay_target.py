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
		target.modify_pom(sdk_version)
	elif (target.buildSystem == buildSystem.GRADLE):
		target.modify_gradle(sdk_version)
	
	# server start
	docker_up_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
	val_1 = os.system(docker_up_cmd)

	proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
	(out_stream, err_stream) = proc.communicate()
	docker_container_name = "target-repo"

	# target replay
	if (target.buildSystem == buildSystem.MAVEN):
		test_command = "docker exec " + docker_container_name + " ./mvnw clean install surefire:test --fail-never"
	elif (target.buildSystem == buildSystem.GRADLE):
		test_command = "docker exec " + docker_container_name + " ./gradlew test"
	val_2 = os.system(test_command)

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
				ReplayTest("test-pass", TestResult.PASS),
				ReplayTest("test-fail", TestResult.FAIL),
				ReplayTest("get default customer unit - pass", TestResult.PASS),
				ReplayTest("Handling characters case - a", TestResult.PASS),
				ReplayTest("Case to test #624", TestResult.PASS),
				ReplayTest("Case - 595", TestResult.PASS),
				ReplayTest("StartsWith should be the assertion type - 590", TestResult.PASS)
			]
		),
		Target(
			"https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
			"unlogged-spring-webflux-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
			buildSystem.MAVEN,
			[
				ReplayTest("getStringFromObservable saved on 2024-03-28 12:51", TestResult.PASS),
				ReplayTest("enrichDefaultPerson saved on 2024-04-02 16:07", TestResult.PASS)
			]
		),
		Target(
			"https://github.com/unloggedio/unlogged-spring-mvc-maven-demo",
			"unlogged-spring-mvc-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/mvc/demo/Application.java",
			buildSystem.MAVEN,
			[
				ReplayTest("loadAllBooks - normal", TestResult.PASS),
				ReplayTest("getBookById - normal", TestResult.PASS),
				ReplayTest("getAllBooks - normal", TestResult.PASS),
				ReplayTest("insertBook - normal", TestResult.PASS),
				ReplayTest("updateBook - normal", TestResult.PASS),
				ReplayTest("deleteBook - normal", TestResult.PASS)
			]
		)
	]
		
	for local_target in target_list:
		replay_target(local_target)
