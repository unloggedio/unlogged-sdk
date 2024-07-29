import os
import sys
import time
from Target import Target
from configEnum import buildSystem
import subprocess

record_in_file = False

def autoexecute_target(target):

    print("AutoExecuting project : ["+target.test_repo_name+"]")
    os.system(f"git clone -b {target.branch_name} {target.test_repo_url}")

    if (target.buildSystem == buildSystem.MAVEN):
        target.modify_pom(sdk_version)
    elif (target.buildSystem == buildSystem.GRADLE):
        target.modify_gradle(sdk_version)

    docker_up_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
    val_1 = os.system(docker_up_cmd)

    proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
    (out_stream, err_stream) = proc.communicate()

    # wait till project has started
    time.sleep(120)

    results_folder = "autoexecutor_results"
    if not os.path.exists(results_folder):
        os.makedirs(results_folder)

    output_filename = results_folder+"/"+target.test_repo_name+"_"+sdk_version+"_autoexecutor_results.txt"
    test_command = "mvn test -Dtest=AutoExecutorCITest#"+target.autoexecutor_test_method

    if record_in_file:
        run_command = f"{test_command} > {output_filename}"
    else:
        run_command = test_command

    status = os.system(run_command)

    docker_down_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml down"
    os.system(docker_down_cmd)
    os.system("rm -rf " + target.test_repo_name)


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

    for target in target_list:
        autoexecute_target(target)