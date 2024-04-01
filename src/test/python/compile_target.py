import os
import sys
from Target import Target
from configEnum import buildSystem

def compile_target (target):
	
	# clone target
	os.system("git clone " + target.test_repo_url)
	
	# modify build system file
	target.modify_main()
	if (target.buildSystem == buildSystem.MAVEN):
		target.modify_pom(sdk_version)
		compile_command = "cd " + target.test_repo_name + " && mvn clean compile"
	elif (target.buildSystem == buildSystem.GRADLE):
		target.modify_gradle(sdk_version)
		compile_command = "cd " + target.test_repo_name + " && gradle clean compileJava"
	
	# target compile
	response_code = os.system(compile_command)
	if (response_code == 0):
		print ("Target compiled succesfully: " +  target.test_repo_name)
	else:
		raise Exception("Target did not compile: " + target.test_repo_name)

	if response_code == 0:
		print("starting application")
		start_cmds = ""
		for command in target.start_commands:
			start_cmds+=command+"&&"
		start_cmds = start_cmds[:-2]
		start_response_code = os.system(start_cmds)
		if start_response_code == 0:
			print ("Target started successfully : "+target.test_repo_name)
		else:
			raise Exception("Target did not start successfully : "+target.test_repo_name)

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
			start_commands = ["docker compose -f conf/docker-compose.yml up"]
		)
	]
		
	for local_target in target_list:
		compile_target(local_target)
