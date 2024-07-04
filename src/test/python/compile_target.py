import os
import sys
from Target import Target
from configEnum import buildSystem
import subprocess

# Path to Fernflower JAR file after building from source
FERNFLOWER_JAR_PATH = "fernflower/build/libs/fernflower.jar"

def set_java_home(java_home):
    os.environ["JAVA_HOME"] = java_home
    os.environ["PATH"] = os.path.join(java_home, "bin") + ":" + os.environ["PATH"]

def check_java_version(expected_version):
    result = subprocess.run(["java", "-version"], capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Failed to check Java version: {result.stderr}")

    version_output = result.stderr.split('\n')[0]
    version = version_output.split('"')[1]

    if version.startswith("1.8"):
        version = "8"
    else:
        version = version.split('.')[0]

    if not version == expected_version:
        raise Exception(f"Java version {version} does not match expected version {expected_version} - Failing")
    print(f"Java version {version} matches expected version {expected_version} - Passing")

def clone_and_build_fernflower():
    # Clone Fernflower repository
    if not os.path.exists("fernflower"):
        subprocess.run(["git", "clone", "https://github.com/fesh0r/fernflower"])
    else:
        print("Fernflower repository already cloned")

    # Build Fernflower using Gradle
    os.chdir("fernflower")
    result = subprocess.run(["./gradlew", "build"], capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Fernflower build failed: {result.stderr}")
    os.chdir("..")


def compile_target (target):

	# clone target
	os.system(f"git clone -b {target.branch_name} {target.test_repo_url}")
	expected_java_version = target.java_version
	set_java_home(f"/usr/lib/jvm/temurin-{expected_java_version}-jdk-amd64")
	check_java_version(expected_java_version)

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
			raise Exception("Found reactor core in a Non reactive project " + target.test_repo_name + " - Failing")
		else :
			print("Reactor core not found on NonReactive project - Passing")
	else:
		if "io.projectreactor" in dependencies:
			print("Reactor core found on Reactive project - Passing")
		else :
			raise Exception("Reactor core not found in reactive project " + target.test_repo_name + " - Failing")

    # Check insertion of probe in FutureController class
    check_probe_insertion(target)


    # delete target
	os.system("rm -rf " + target.test_repo_name)

def check_probe_insertion(target):
    # Paths for FutureController class file and decompiled output
    class_file_path = os.path.join("target/classes/org/unlogged/demo/controller", "FutureController.class")
    decompiled_dir = os.path.join("decompiled", target.test_repo_name)
    os.makedirs(decompiled_dir, exist_ok=True)

    # Decompile the class file
    decompile_class_file(class_file_path, decompiled_dir)

    # Verify the logging statements in the decompiled file
    java_file_path = os.path.join(decompiled_dir, "FutureController.java")
    if os.path.exists(java_file_path):
        verify_logs_in_file(java_file_path)
    else:
        raise Exception(f"Decompiled file {java_file_path} not found")

def decompile_class_file(class_file, output_dir):
    # Command to decompile using fernflower
    decompiler_command = [
        "java", "-jar", FERNFLOWER_JAR_PATH,
        class_file, output_dir
    ]
    result = subprocess.run(decompiler_command, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Decompilation failed: {result.stderr}")
    print(f"Decompilation successful for {class_file}")

def verify_logs_in_file(java_file):
    expected_log = 'Runtime.registerClass'  # Add your specific log statement to verify
    with open(java_file, 'r') as file:
        content = file.read()
    if expected_log in content:
        print(f"Log verification passed for: {java_file}")
    else:
        raise Exception(f"Log verification failed for: {java_file}")


if __name__=="__main__":

    sdk_version = sys.argv[1]
    branch_java_version_map = {
        "java8" : "8",
        "java11" : "11",
        "java21" : "21",
        "main" : "17"
        }

    target_list = []

    for branch_name in branch_java_version_map:
        target_list.append(
            Target(
                "https://github.com/unloggedio/unlogged-spring-maven-demo",
                "unlogged-spring-maven-demo",
                "/pom.xml",
                "/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java",
                buildSystem.MAVEN,
                projectType="Normal",
                branch_name=branch_name,
                java_version=branch_java_version_map[branch_name]
            )
        )
    #     target_list.append(
    #         Target(
    #             "https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
    #             "unlogged-spring-webflux-maven-demo",
    #             "/pom.xml",
    #             "/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
    #             buildSystem.MAVEN,
    #             projectType="Reactive",
    #             branch_name=branch_name,
    #             java_version=branch_java_version_map[branch_name]
    #         )
    #     )
    # target_list.append(
    #     Target(
    #         "https://github.com/unloggedio/unlogged-spring-mvc-maven-demo",
    #         "unlogged-spring-mvc-maven-demo",
    #         "/pom.xml",
    #         "/src/main/java/org/unlogged/mvc/demo/Application.java",
    #         buildSystem.MAVEN,
    #         projectType="Normal",
    #         branch_name="main",
    #         java_version="17"
    #     )
    # )

    # Clone and build Fernflower if not already done
    clone_and_build_fernflower()

    for local_target in target_list:
        compile_target(local_target)

    # Clean up Fernflower directory after processing all targets
    os.system("rm -rf fernflower")