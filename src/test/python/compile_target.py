import os
import sys
from Target import Target
from configEnum import buildSystem
import subprocess

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
		raise Exception("Target did not compiled: " + target.test_repo_name)

	os_cwd = os.getcwd()
	program = 'mvn'
	arg = 'dependency:tree'
	if target.buildSystem == buildSystem.GRADLE:
		program = 'gradle'
		arg = 'dependencies'

	dependencies = subprocess.run([program, arg], cwd = os_cwd + "/" + target.test_repo_name,capture_output=True, text=True).stdout

	if target.projectType == "Normal":
		#Ensure reactive frameworks are not used on Non reactive repos
		if "io.projectreactor" in dependencies:
			raise Exception("Found reactor core in a Non reactive project " + target.test_repo_name)
		else :
			print("Reactor core not found on NonReactive project")
	else:
		if "io.projectreactor" in dependencies:
			print("Reactor core found on Reactive project")
		else :
			raise Exception("Reactor core not found in reactive project " + target.test_repo_name)

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
			projectType="Normal"
		),
		Target(
			"https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
			"unlogged-spring-webflux-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
			buildSystem.MAVEN,
			projectType="Reactive"
		)
	]
		
	for local_target in target_list:
		compile_target(local_target)
