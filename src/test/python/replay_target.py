import os
import sys
from configEnum import buildSystem, TestResult, ReportType, StartMode
from markup_report_generator import ReportGenerator
from test_suite import ReplayTestSuite
from pathlib import Path
import subprocess

def replay_target(target):
    print(f"Starting replay test run for {target.test_repo_name} -> branch {target.target_run_properties.branch_name}")
    target_repo_folder = Path(target.test_repo_name)
    if not target_repo_folder.is_dir() :
        # Clone if the folder doesn't exist
        print(f"Cloning repo : {target.test_repo_name} -> branch : {target.target_run_properties.branch_name}")
        os.system(f"git clone -b {target.target_run_properties.branch_name} {target.test_repo_url}")
    else :
        # switch branch if folder exists
        print(f"Switching repo branch to {target.target_run_properties.branch_name}")
        os.system(f"cd {target.test_repo_name} && git restore . && git switch {target.target_run_properties.branch_name}")

    # set java version
    expected_java_version = target.target_run_properties.java_version
    target.set_java_home(f"/usr/lib/jvm/temurin-{expected_java_version}-jdk-amd64")
    target.check_java_version(expected_java_version)

    base_test_command = ""
    # modify build
    if (target.buildSystem == buildSystem.MAVEN):
        target.modify_pom(sdk_version)
        base_test_command = "./mvnw clean test"
    elif (target.buildSystem == buildSystem.GRADLE):
        target.modify_gradle(sdk_version)
        base_test_command = "./gradlew clean test"

    # server start
    if target.target_run_properties.start_mode == StartMode.DOCKER:
        docker_up_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml up -d"
        os.system(docker_up_cmd)

        proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
        (out_stream, err_stream) = proc.communicate()
        docker_container_name = "target-repo"

        # target replay
        test_command = "docker exec " + docker_container_name + " "+base_test_command
    else:
        test_command = "cd "+target.test_repo_name+" && "+base_test_command

    os.system(test_command)

    result_map = target.check_replay()
    # clean repo
    if target.target_run_properties.start_mode == StartMode.DOCKER:
        docker_down_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml down"
        os.system(docker_down_cmd)


    os.system("rm -rf " + target.test_repo_name)
    return result_map

if __name__ == "__main__":
    sdk_version = sys.argv[1]

    passing = True
    report_generator = ReportGenerator(ReportType.REPLAY)
    for local_target in ReplayTestSuite().get_replay_test_suite():
        result_map = replay_target(local_target)
        report_generator.write_replay_report(local_target, result_map)

        if result_map['status'] == TestResult.FAIL:
            passing = False

    if (passing):
        print("Test Passed")
    else:
        raise Exception("Test Failed")


