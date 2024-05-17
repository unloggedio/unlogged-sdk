import os
import sys
import subprocess
from Target import Target
from configEnum import buildSystem


def print_installed_java_versions():
    jvm_path = "/usr/lib/jvm/"
    if not os.path.exists(jvm_path):
        print(f"Akshat No Java installations found at {jvm_path}")
        return

    java_versions = os.listdir(jvm_path)
    if not java_versions:
        print(f"Akshat No Java installations found in {jvm_path}")
    else:
        print("Akshat Installed Java versions:")
        for version in java_versions:
            print(f"- {version}")

def set_java_version(expected_version):
    try:
        # Check if the expected version of Java is already set
        current_version_output = subprocess.run(["java", "-version"], capture_output=True, text=True)
        current_version = current_version_output.stderr.split('\n')[0].split('"')[1]

        if current_version.startswith(expected_version):
            print(f"Java version {expected_version} is already set.")
            return

        # Use update-alternatives to set the Java version
        update_command = f"sudo update-alternatives --set java /usr/lib/jvm/java-{expected_version}-openjdk-amd64/bin/java"
        subprocess.run(update_command, shell=True, check=True, capture_output=True, text=True)

        # Verify the change
        verification_output = subprocess.run(["java", "-version"], capture_output=True, text=True)
        new_version = verification_output.stderr.split('\n')[0].split('"')[1]

        if not new_version.startswith(expected_version):
            raise Exception(f"Failed to set Java version to {expected_version}.")

        print(f"Java version set to {expected_version} successfully.")

    except Exception as e:
        raise Exception(f"Failed to set Java version to {expected_version}: {str(e)}")


def check_java_version(expected_version):
    result = subprocess.run(["java", "-version"], capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Failed to check Java version: {result.stderr}")

    version_output = result.stderr.split('\n')[0]
    version = version_output.split('"')[1]
    version = version.split('.')[0]

    if not version.startswith(expected_version):
        raise Exception(f"Java version {version} does not match expected version {expected_version} - Failing")
    print(f"Java version {version} matches expected version {expected_version} - Passing")

def compile_target(target, sdk_version):
    # Clone target repository
    clone_command = ["git", "clone", "--branch", target.branch_name, target.test_repo_url]
    result = subprocess.run(clone_command, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Failed to clone repository: {result.stderr}")


    expected_java_version = target.java_version
    set_java_version(expected_java_version)
    check_java_version(expected_java_version)

    # Modify build system file
    target.modify_main()
    compile_command = []
    if target.buildSystem == buildSystem.MAVEN:
        target.modify_pom(sdk_version)
        compile_command = ["mvn", "clean", "compile"]
    elif target.buildSystem == buildSystem.GRADLE:
        target.modify_gradle(sdk_version)
        compile_command = ["gradle", "clean", "compileJava"]

    # Compile the target
    compile_path = os.path.join(os.getcwd(), target.test_repo_name)
    result = subprocess.run(compile_command, cwd=compile_path, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Failed to compile target: {target.test_repo_name}\n{result.stderr}")
    print(f"Target compiled successfully: {target.test_repo_name}")

    # Check dependencies
    program = "mvn" if target.buildSystem == buildSystem.MAVEN else "gradle"
    arg = "dependency:tree" if target.buildSystem == buildSystem.MAVEN else "dependencies"

    result = subprocess.run([program, arg], cwd=compile_path, capture_output=True, text=True)
    dependencies = result.stdout
    if result.returncode != 0:
        raise Exception(f"Failed to list dependencies: {result.stderr}")

    if target.projectType == "Normal":
        if "io.projectreactor" in dependencies:
            raise Exception(f"Found reactor core in a Non-reactive project: {target.test_repo_name} - Failing")
        else:
            print("Reactor core not found in Non-reactive project - Passing")
    else:
        if "io.projectreactor" in dependencies:
            print("Reactor core found in Reactive project - Passing")
        else:
            raise Exception(f"Reactor core not found in Reactive project: {target.test_repo_name} - Failing")

    # Delete target
    result = subprocess.run(["rm", "-rf", target.test_repo_name], capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Failed to delete target directory: {result.stderr}")

if __name__ == "__main__":
    sdk_version = sys.argv[1]
    print_installed_java_versions()

    branch_java_version_map = {
        "java8": "8",
        "java11": "11",
        "java21": "21",
        "main": "17"
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
        target_list.append(
            Target(
                "https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
                "unlogged-spring-webflux-maven-demo",
                "/pom.xml",
                "/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
                buildSystem.MAVEN,
                projectType="Reactive",
                branch_name=branch_name,
                java_version=branch_java_version_map[branch_name]
            )
        )

    for local_target in target_list:
        compile_target(local_target, sdk_version)
