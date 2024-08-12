import os
import sys
from Target import Target, TargetRunProperties, CompileTestOptions
from configEnum import buildSystem, ReportType, TestResult, StartMode, ProjectType
from markup_report_generator import Report_Generator
import subprocess

def compile_target (target):

    print(f"Starting compile test run for {target.test_repo_name} -> branch {target.target_run_properties.branch_name}")
    # clone target
    os.system(f"git clone -b {target.target_run_properties.branch_name} {target.test_repo_url}")
    expected_java_version = target.target_run_properties.java_version
    target.set_java_home(f"/usr/lib/jvm/temurin-{expected_java_version}-jdk-amd64")
    target.check_java_version(expected_java_version)

    # modify build system file
    target.modify_main()
    if (target.buildSystem == buildSystem.MAVEN):
        target.modify_pom(sdk_version)
        compile_command = "cd " + target.test_repo_name + " && mvn clean compile"
    elif (target.buildSystem == buildSystem.GRADLE):
        target.modify_gradle(sdk_version)
        compile_command = "cd " + target.test_repo_name + " && gradle clean compileJava"

    # target compile
    information = "done"
    passing = True
    response_code = os.system(compile_command)
    if (response_code == 0):
        information = "Successfully compiled"
        print ("Target compiled successfully : " +  target.test_repo_name)
    else:
        information = "Did not compile"
        print ("Target did not compile : " + target.test_repo_name)
        passing = False

    os_cwd = os.getcwd()
    program = 'mvn'
    arg = 'dependency:tree'
    if target.buildSystem == buildSystem.GRADLE:
        program = 'gradle'
        arg = 'dependencies'

    dependencies = subprocess.run([program, arg], cwd = os_cwd + "/" + target.test_repo_name,capture_output=True, text=True).stdout

    if target.compile_test_options.project_type == ProjectType.NORMAL:
        #Ensure reactive frameworks are not used on Non-Reactive repos
        if "io.projectreactor" in dependencies:
            information = "Reactor core found in non reactive project, not expected"
            print("Found reactor core in a Non reactive project " + target.test_repo_name + " - Failing")
            passing = False
        else:
            print("Reactor core not found on NonReactive project - Passing")
    else:
        if "io.projectreactor" in dependencies:
            print("Reactor core found on Reactive project - Passing")
        else:
            information = "Reactor core not found in reactive project, not expected"
            print("Reactor core not found in reactive project " + target.test_repo_name + " - Failing")
            passing = False

    # delete target
    os.system("rm -rf " + target.test_repo_name)
    result = dict()
    result['status'] = TestResult.PASS if passing else TestResult.FAIL
    result['information'] = information
    return result


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
                target_run_properties=TargetRunProperties(branch_name,branch_java_version_map[branch_name], StartMode.CMD),
                compile_test_options=CompileTestOptions(ProjectType.NORMAL)
            )
        )
        target_list.append(
            Target(
                "https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
                "unlogged-spring-webflux-maven-demo",
                "/pom.xml",
                "/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
                buildSystem.MAVEN,
                target_run_properties=TargetRunProperties(branch_name,branch_java_version_map[branch_name], StartMode.CMD),
                compile_test_options=CompileTestOptions(ProjectType.REACTIVE)
            )
        )
    target_list.append(
        Target(
            "https://github.com/unloggedio/unlogged-spring-mvc-maven-demo",
            "unlogged-spring-mvc-maven-demo",
            "/pom.xml",
            "/src/main/java/org/unlogged/mvc/demo/Application.java",
            buildSystem.MAVEN,
            target_run_properties=TargetRunProperties("main","17", StartMode.CMD),
            compile_test_options=CompileTestOptions(ProjectType.NORMAL)
        )
    )

    report_generator = Report_Generator(ReportType.COMPILE)
    results = []
    for local_target in target_list:
        result = compile_target(local_target)
        results.append(result)
        report_generator.add_compile_result_status_entry(local_target, result['status'], result['information'])

    report_generator.generate_and_write_report()
    for summary in results:
        if summary['status'] == False:
            raise Exception("Compile Pipeline has failing cases")
