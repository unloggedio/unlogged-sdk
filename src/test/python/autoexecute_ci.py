import os
import sys
import time
from Target import Target
from configEnum import buildSystem
import subprocess

def autoexecute_target(target):

    os.system(f"git clone -b {target.branch_name} {target.test_repo_url}")

    if (target.buildSystem == buildSystem.MAVEN):
        target.modify_pom(sdk_version)
    elif (target.buildSystem == buildSystem.GRADLE):
        target.modify_gradle(sdk_version)

    docker_up_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml up -d"
    os.system(docker_up_cmd)

    proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
    (out_stream, err_stream) = proc.communicate()

    # wait till project has started
    time.sleep(140)

    test_command = "mvn test -Dtest=AutoExecutorCITest#"+target.autoexecutor_test_method
    status = os.system(test_command)

    docker_down_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml down"
    os.system(docker_down_cmd)
    os.system("rm -rf " + target.test_repo_name)

    passing = True
    if status!=0:
        print("AutoExecution run for "+target.test_repo_name+" exited with non zero exit code")
        passing = False

    return passing


if __name__=="__main__":

    sdk_version = sys.argv[1]

    target_list = []
    target_list.append(
        Target(
            "https://github.com/unloggedio/unlogged-spring-maven-demo",
            "unlogged-spring-maven-demo",
            "/pom.xml",
            "/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java",
            buildSystem.MAVEN,
            branch_name="main",
            autoexecutor_test_method="startUnloggedMavenDemoTest"
        )
    )
    target_list.append(
        Target(
            "https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
            "unlogged-spring-webflux-maven-demo",
            "/pom.xml",
            "/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
            buildSystem.MAVEN,
            branch_name="dockerization",
            autoexecutor_test_method="startWebfluxDemoTest"
        )
    )

    result_stats = []
    for target in target_list:
        result_stats.append(autoexecute_target(target))

    failing_suite_count = result_stats.count(False)
    if failing_suite_count > 0:
        raise Exception(f"There are {failing_suite_count} failing test suites in AutoExecutor CI Pipeline")