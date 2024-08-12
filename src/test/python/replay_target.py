import os
import sys
from Target import Target, ReplayTest, TargetRunProperties, ReplayTestOptions
from configEnum import buildSystem, TestResult, ReportType, StartMode
from markup_report_generator import Report_Generator
from test_suite import ReplayTestSuite
import subprocess

def replay_target(target):
    # clone repo
    print(f"Starting replay test run for {target.test_repo_name} -> branch {target.target_run_properties.branch_name}")
    clone_status = os.system(f"git clone -b {target.target_run_properties.branch_name} {target.test_repo_url}")

    print(clone_status)
    if clone_status == 32768:
        os.system(f"cd {target.test_repo_name} && git checkout . && git checkout {target.target_run_properties.branch_name}")

    # set java version
    expected_java_version = target.target_run_properties.java_version
    target.set_java_home(f"/usr/lib/jvm/temurin-{expected_java_version}-jdk-amd64")
    target.check_java_version(expected_java_version)

    # modify build
    if (target.buildSystem == buildSystem.MAVEN):
        target.modify_pom(sdk_version)
    elif (target.buildSystem == buildSystem.GRADLE):
        target.modify_gradle(sdk_version)

    # server start
    if target.target_run_properties.start_mode == StartMode.DOCKER:
        docker_up_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml up -d"
        os.system(docker_up_cmd)

        proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
        (out_stream, err_stream) = proc.communicate()
        docker_container_name = "target-repo"

        # target replay
        if (target.buildSystem == buildSystem.MAVEN):
            test_command = "docker exec " + docker_container_name + " ./mvnw clean install surefire:test --fail-never"
        elif (target.buildSystem == buildSystem.GRADLE):
            test_command = "docker exec " + docker_container_name + " ./gradlew clean test"
        os.system(test_command)
    else:
        if (target.buildSystem == buildSystem.MAVEN):
            test_command = "cd "+target.test_repo_name+" && ./mvnw clean test"
        elif (target.buildSystem == buildSystem.GRADLE):
            test_command = "cd "+target.test_repo_name+" && ./gradlew clean test"
        os.system(test_command)


    # assert and clean repo
    result_map = target.check_replay()
    if target.target_run_properties.start_mode == StartMode.DOCKER:
        docker_down_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml down"
        os.system(docker_down_cmd)

    os.system("rm -rf " + target.test_repo_name)
    return result_map

if __name__ == "__main__":
    sdk_version = sys.argv[1]

    passing = True
    result_maps = []
    report_generator = Report_Generator(ReportType.REPLAY)
    for local_target in ReplayTestSuite().get_suite():
        result_map = replay_target(local_target)
        report_generator.reset_map()
        report_generator.add_replay_result_status_entry(local_target, result_map)
        result_maps.append(result_map)
        report_generator.generate_and_write_report()

        if result_map['status'] == TestResult.FAIL:
            passing = False

    if (passing):
        print("Test Passed")
    else:
        raise Exception("Test Failed")


