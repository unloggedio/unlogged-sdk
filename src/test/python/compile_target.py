import os
import sys
from Target import Target
from configEnum import buildSystem

def compile_target (target):
	
	# clone target
	os.system("git clone " + target.test_repo_url)
	
	# modify build system file
	target.modify_main()
	action_command=""
	if target.custom_start_command != "":
		action_command = " && "+target.custom_start_command
	if (target.buildSystem == buildSystem.MAVEN):
		target.modify_pom(sdk_version)
		if action_command == "":
			action_command = " && mvn clean compile"
		compile_command = "cd " + target.test_repo_name + action_command
	elif (target.buildSystem == buildSystem.GRADLE):
		target.modify_gradle(sdk_version)
		if action_command == "":
			action_command = " && gradle clean compileJava"
		compile_command = "cd " + target.test_repo_name + action_command
	
	# target compile
	response_code = os.system(compile_command)
	if (response_code == 0):
		print ("Target compiled succesfully: " +  target.test_repo_name)
	else:
		raise Exception("Target did not compiled: " + target.test_repo_name)

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
			custom_start_command=""
		),
		Target(
			"https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
			"unlogged-spring-webflux-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
			buildSystem.MAVEN,
			custom_start_command="mvn clean spring-boot:run"
		)
	]
		
	for local_target in target_list:
		compile_target(local_target)
