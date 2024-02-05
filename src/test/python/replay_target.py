import os
import sys
from Target import Target
from build_system import build_system
	
def replay_target (target):
	
	# clone target
	os.system("git clone " + target.test_repo_url)
	
	# modify build system file
	if (target.build_system == build_system.MAVEN):
		target.modify_pom(sdk_version)
		test_command = "cd " + target.test_repo_name + " && docker-compose up -f conf/docker-compose.yml && mvn test"
	elif (target.build_system == build_system.GRADLE):
		target.modify_gradle(sdk_version)
		test_command = "cd " + target.test_repo_name + " && docker-compose up -f conf/docker-compose.yml && gradle test"
	
	# target replay
	response_code = os.system(test_command)
	if (response_code == 0):
		print ("Test for target executed successfully: " +  target.test_repo_name)
	else:
		raise Exception("Test for target failed execution: " + target.test_repo_name)

	# surefire-report assertion
	# TODO

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
				TestResponse("test_name-1", "test_result-1"),
				TestResponse("test_name-2", "test_result-2")
			]
		)
	]
		
	for local_target in target_list:
		replay_target(local_target)
