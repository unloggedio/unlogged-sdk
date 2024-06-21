import os
import sys
from Target import Target, ReplayTest
from configEnum import buildSystem, TestResult
import subprocess

def replay_target (target):
	
	# clone repo
	os.system("git clone " + target.test_repo_url)
	
	# modify build
	if (target.buildSystem == buildSystem.MAVEN):
		target.modify_pom(sdk_version)
	elif (target.buildSystem == buildSystem.GRADLE):
		target.modify_gradle(sdk_version)
	
	# server start
	docker_up_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml up -d"
	val_1 = os.system(docker_up_cmd)

	proc = subprocess.Popen(["docker ps -a"], stdout=subprocess.PIPE, shell=True)
	(out_stream, err_stream) = proc.communicate()
	docker_container_name = "target-repo"

	# target replay
	if (target.buildSystem == buildSystem.MAVEN):
		test_command = "docker exec " + docker_container_name + " ./mvnw clean install surefire:test --fail-never"
	elif (target.buildSystem == buildSystem.GRADLE):
		test_command = "docker exec " + docker_container_name + " ./gradlew test"
	val_2 = os.system(test_command)

	response_code = val_1 or val_2
	if (response_code == 0):
		print ("Test for target executed successfully: " +  target.test_repo_name)
	else:
		raise Exception("Test for target failed execution: " + target.test_repo_name)

	# assert and clean repo
	target.check_replay()
	docker_down_cmd = "cd " + target.test_repo_name + " && docker-compose -f conf/docker-compose.yml down"
	os.system(docker_down_cmd)
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
			[
                ReplayTest("FutureController.getFutureResult - normal", TestResult.PASS),
                ReplayTest("FutureController.getFutureResultOptional - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getDefaultModel - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelMiniDto - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelMiniDto - call from ModelMapper mocked", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelDto - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelDtoWithProvider - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getFromConverter - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.insertDefault - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.insertNew - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.getall - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.getById - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.updatePojo - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.deleteById - normal", TestResult.PASS),
                ReplayTest("FutureService.doSomething - normal", TestResult.PASS),
                ReplayTest("FutureService.doSomethingOptional - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.getDefaultUser - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.getEmptyOptionalUser - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.create1 - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.createNullable - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.getPresentStatus - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.getEmptyStatus - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.ifPresent - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.orElseCase - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.orElseGet - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.getUserUsage - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.filterUserOptional - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.countNameLength - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.flatMapUsage - normal", TestResult.PASS),
                ReplayTest("OptionalOpsController.chain - normal", TestResult.PASS),
                ReplayTest("VarOpsController.primitivesWrapped - normal", TestResult.PASS),
                ReplayTest("VarOpsController.getAUser - normal", TestResult.PASS),
                ReplayTest("VarOpsController.varListAndMap - normal", TestResult.PASS),
                ReplayTest("VarOpsController.getAsResponseEntity - normal", TestResult.PASS),
                ReplayTest("VarOpsController.getCustomers - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.getUserGroups - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.getUserList - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.forEachRun - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.forEachRunParallel - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.mapAndCollect - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.mapSet - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.mapVector - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.mapAndFilter - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.filterAndFindFirst - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.toArrayCollection_Usernames - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.flatmap_maxId - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.flatmap_minId - normal4", TestResult.PASS),
                ReplayTest("StreamOpsController.peek_all - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.countUsersInGroups - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.limitUsers - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.distinctUsage - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.matchCases - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.reduceUsage - normal", TestResult.PASS),
                ReplayTest("StreamOpsController.getSortedIdOrder - normal missing symbols", TestResult.PASS),
                ReplayTest("StreamOpsController.groupBy - normal", TestResult.PASS),
                ReplayTest("ResponseEntityOps.getOkString - normal", TestResult.PASS),
                ReplayTest("ResponseEntityOps.getOkUser - normal", TestResult.PASS),
                ReplayTest("ResponseEntityOps.getUserOf - normal", TestResult.PASS),
                ReplayTest("ResponseEntityOps.createWithCode - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getSquare - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.shapeSerial - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getRectangle - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getFilledRectangle - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getCircle - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.executorServiceCallablesAny - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.executorServiceRunnable - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.scheduledThread - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.scheduledThreadFixedRate - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.scheduledThreadFixedDelay - normal", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.findAll - normal", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.findById - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.findById - normal integration", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.deleteById - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.deleteById - normal integration", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.insertNew - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.insertNew - normal integration", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.update - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.update - normal integration", TestResult.PASS),
                ReplayTest("GlobalFilter.getGlobalFilterAdditive - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.getAll - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.getById - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.deleteById - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.insertNew - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.updateExisting - normal", TestResult.PASS)
			]
		),
		Target(
			"https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
			"unlogged-spring-webflux-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
			buildSystem.MAVEN,
			[
				ReplayTest("getStringFromObservable saved on 2024-03-28 12:51", TestResult.PASS),
				ReplayTest("enrichDefaultPerson saved on 2024-04-02 16:07", TestResult.PASS)
			]
		),
		Target(
			"https://github.com/unloggedio/unlogged-spring-mvc-maven-demo",
			"unlogged-spring-mvc-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/mvc/demo/Application.java",
			buildSystem.MAVEN,
			[
				ReplayTest("loadAllBooks - normal", TestResult.PASS),
				ReplayTest("getBookById - normal", TestResult.PASS),
				ReplayTest("getAllBooks - normal", TestResult.PASS),
				ReplayTest("insertBook - normal", TestResult.PASS),
				ReplayTest("updateBook - normal", TestResult.PASS),
				ReplayTest("deleteBook - normal", TestResult.PASS)
			]
		)
	]
		
	for local_target in target_list:
		replay_target(local_target)
