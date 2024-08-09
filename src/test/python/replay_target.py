import os
import sys
from Target import Target, ReplayTest, TargetRunProperties, ReplayTestOptions
from configEnum import buildSystem, TestResult, ReportType, StartMode
from markup_report_generator import Report_Generator
import subprocess

def replay_target(target):
    # clone repo
    print(f"Starting replay test run for {target.test_repo_name} -> branch {target.target_run_properties.branch_name}")
    os.system(f"git clone -b {target.target_run_properties.branch_name} {target.test_repo_url}")

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
    else:
        if (target.buildSystem == buildSystem.MAVEN):
            test_command = "cd "+target.test_repo_name+" && ./mvnw clean install surefire:test --fail-never"
        elif (target.buildSystem == buildSystem.GRADLE):
            test_command = "cd "+target.test_repo_name+" && ./gradlew test"
        val_1 = 0
        val_2 = os.system(test_command)

    response_code = val_1 or val_2
    if (response_code == 0):
        print("Test for target executed successfully: " + target.test_repo_name)
    else:
        print("Test for target failed execution: " + target.test_repo_name)
        result_map = dict()
        result_map['java_version'] = target.target_run_properties.java_version
        result_map['status'] = TestResult.FAIL
        result_map['tot'] = "0"
        result_map['passing'] = "0"
        result_map['case_result'] = []
        result_map['run_state'] = False
        return result_map

    # assert and clean repo
    result_map = target.check_replay()
    if target.target_run_properties.start_mode == StartMode.DOCKER:
        docker_down_cmd = "cd " + target.test_repo_name + " && docker compose -f conf/docker-compose.yml down"
        os.system(docker_down_cmd)

    os.system("rm -rf " + target.test_repo_name)
    return result_map


if __name__ == "__main__":
    sdk_version = sys.argv[1]
    target_list = [
        # unlogged-spring-maven-demo
        Target(
            "https://github.com/unloggedio/unlogged-spring-maven-demo",
            "unlogged-spring-maven-demo",
            "/pom.xml",
            "/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java",
            buildSystem.MAVEN,
            target_run_properties = TargetRunProperties("main","17", StartMode.DOCKER),
            replay_test_options= ReplayTestOptions(
            [
                ReplayTest("org.unlogged.demo.controller.CustomerController.saveCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.CustomerController.removeCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.CustomerController.generateNeReferralCode - normal",
                           TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.controller.CustomerController.isCustomerEligibleForLoyaltyProgram - normal",
                    TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.CustomerController.getDummyProfile - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.CustomerController.getScoreMaps - normal", TestResult.PASS),
                ReplayTest("FutureController.getFutureResult - normal", TestResult.PASS),
                ReplayTest("FutureController.getFutureResultOptional - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.GreetingController.getGreeting - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.InternalClassController.getL1Object - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.InternalClassController.getMapValue - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.InternalClassController.getTimeObjects - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.InternalClassController.returnChar - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.InternalClassController.cmm - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getDefaultModel - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelMiniDto - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelMiniDto - call from ModelMapper mocked",
                           TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelDto - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getUserModelDtoWithProvider - normal", TestResult.PASS),
                ReplayTest("ModelMapperOpsController.getFromConverter - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.insertDefault - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.insertNew - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.getall - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.getById - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.updatePojo - normal", TestResult.PASS),
                ReplayTest("MongoOpsController.deleteById - normal", TestResult.PASS),
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
                ReplayTest("org.unlogged.demo.controller.RecursionController.getIsPalindrome - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.RecursionController.getFibonacciSeries - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.RecursionController.isPalindrome - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.RecursionController.factorial - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ResponseEntityOps.getOkString - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ResponseEntityOps.getOkUser - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ResponseEntityOps.getUserOf - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ResponseEntityOps.createWithCode - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getSquare - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.shapeSerial - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getRectangle - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getFilledRectangle - normal", TestResult.PASS),
                ReplayTest("SealedOpsController.getCircle - normal", TestResult.PASS),
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
                ReplayTest("ThreadingOpsController.executorServiceCallablesAny - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.executorServiceRunnable - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.scheduledThread - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.scheduledThreadFixedRate - normal", TestResult.PASS),
                ReplayTest("ThreadingOpsController.scheduledThreadFixedDelay - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ValidatorOpsController.isDefaultUserVaild - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ValidatorOpsController.isUserValid - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ValidatorOpsController.getValidUser - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.ValidatorOpsController.validateUserV2 - normal",
                           TestResult.PASS),
                ReplayTest("VarOpsController.primitivesWrapped - normal", TestResult.PASS),
                ReplayTest("VarOpsController.getAUser - normal", TestResult.PASS),
                ReplayTest("VarOpsController.varListAndMap - normal", TestResult.PASS),
                ReplayTest("VarOpsController.getAsResponseEntity - normal", TestResult.PASS),
                ReplayTest("VarOpsController.getCustomers - normal", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.findAll - normal", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.findById - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.findById - normal integration", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.deleteById - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.deleteById - normal integration", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.insertNew - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.insertNew - normal integration", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.update - normal mocked", TestResult.PASS),
                ReplayTest("PropertyControllerImpl.update - normal integration", TestResult.PASS),
                ReplayTest("org.unlogged.demo.controller.lcc.getSomeInt - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.cron.ScheduledJobs.scheduleFixedDelayTask - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.cron.ScheduledJobs.cron1 - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.cron.ScheduledJobs.cron2Greet - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.CustomObjectInputFilter.checkInput - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.CustomObjectInputFilter.checkInput - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.CustomObjectInputFilter.checkInput - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.CustomObjectInputFilter.checkInput - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.CustomObjectInputFilter.checkInput - normal",
                           TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObjectWithMergedFilters - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObjectNotAllowed - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObjectWithRejectFilter - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObjectUndecided - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObject1 - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObjectWithAllowFilter - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.deserializationfilter.DeserializationController.deserializeObjectRejectUndecided - normal",
                    TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.DeserializationController.serializeObject - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.DeserializationController.serializeObject - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.DeserializationController.serializeObject - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.deserializationfilter.DeserializationController.serializeObject - normal",
                           TestResult.PASS),
                ReplayTest("GlobalFilter.getGlobalFilterAdditive - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.ApiHelper.WeatherApi.getWeatherinfo - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.Alpha.getY - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.Beta.e - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.BigObjController.getListofBigPojos - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.BigObjController.getOrderedList - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.CustomerRes.saveCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.CustomerRes.getCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.CustomerRes.removeCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.CustomerRes.addNewContactNumber - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.CustomerRes.generateNeReferralCode - normal",
                           TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.jspdemo.wfm.Controllers.CustomerRes.isCustomerEligibleForLoyaltyProgram - normal",
                    TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.GcdController.gcdOfTwoNumbers - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.GcdController.getNull - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.ThirdPartyController.getResponse - normal",
                           TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.jspdemo.wfm.Controllers.ThirdPartyController.getWeatherForBangalore - normal",
                    TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.getBool - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.a - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.getTestPojo - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.jodatest - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.getInstant - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.getDeepClass - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.getTestText - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.saveUser - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.saveUser - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Controllers.UserController.testFetchUser - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Dao.CustomerProfileDao.save - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Dao.CustomerProfileDao.save - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Dao.CustomerProfileDao.save - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Dao.CustomerProfileDao.save - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Dao.CustomerProfileDao.fetchCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Dao.CustomerProfileDao.removeCustomer - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.ReferralUtils.generateReferralCode - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.SerializationUtils.getObjectFor - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.SerializationUtils.getObjectFor - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.CustomerService.generateReferralCodes - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.CustomerService.saveNewCustomer - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.CustomerService.fetchCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.CustomerService.removeCustomer - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.CustomerService.addNewContact - normal",
                           TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.jspdemo.wfm.Services.CustomerService.generateReferralForCustomer - normal",
                    TestResult.PASS),
                ReplayTest(
                    "org.unlogged.demo.jspdemo.wfm.Services.CustomerService.isCustomerEligibleForPremium - normal",
                    TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.UserService.many - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.UserService.getDeepClassList - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.Services.UserService.getUser - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.ThirdPartyApiHelper.getWeatherinfo - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.ThirdPartyApiHelper.getWeatherinfo - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.ThirdPartyService.getWeatherFor - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.ThirdPartyService.getWeatherFor - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.UserInstanceDao.save - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.jspdemo.wfm.UserInstanceService.saveUser - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.repository.CustomerProfileRepository.save - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.repository.CustomerProfileRepository.removeCustomer - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.repository.CustomerProfileRepository.fetchCustomerProfile - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.resttemplate.PostController.getPostById - unexpected failure",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.resttemplate.PostsClient.findById - wrong assertions with extra char",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.generateReferralCodes - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.saveNewCustomer - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.removeCustomer - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.generateReferralForCustomer - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.isCustomerEligibleForPremium - normal",
                           TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.getBackProfile - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.CustomerService.getDummyScoreMaps - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.DeepService.getDeepReference - normal", TestResult.PASS),
                ReplayTest("FutureService.doSomething - normal", TestResult.PASS),
                ReplayTest("FutureService.doSomethingOptional - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.WeatherService.getWeatherinfo - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.WeatherService.getWeatherForAddress - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.service.WeatherService.convertToObject - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.getAll - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.getById - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.deleteById - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.insertNew - normal", TestResult.PASS),
                ReplayTest("PropertyServiceCEImpl.updateExisting - normal", TestResult.PASS),
                ReplayTest("org.unlogged.demo.utils.ReferralUtils.generateReferralCode - normal", TestResult.PASS),
                ReplayTest("ClassUserController.setAndGetUser - recursive setter bug freq logging", TestResult.PASS)
            ])
        ),
        # Target(
        # 	"https://github.com/unloggedio/unlogged-spring-webflux-maven-demo",
        # 	"unlogged-spring-webflux-maven-demo",
        # 	"/pom.xml",
        # 	"/src/main/java/org/unlogged/springwebfluxdemo/SpringWebfluxDemoApplication.java",
        # 	buildSystem.MAVEN,
        # 	[
        # 		ReplayTest("VehicleComponent.getVehicles - normal", TestResult.PASS),
        # 		ReplayTest("CronController.cronHello - normal mockMode", TestResult.PASS),
        # 		ReplayTest("CronController.cronHello - normal integMode", TestResult.PASS),
        # 		ReplayTest("EnrichmentController.enrichPersonsInParallel - normal", TestResult.PASS),
        # 		ReplayTest("FluxOpsController.disposableExample - normal", TestResult.PASS),
        # 		ReplayTest("FluxOpsController.parallelFluxExample - normal", TestResult.PASS),
        # 		ReplayTest("FluxOpsController.tuple4Example - normal", TestResult.PASS),
        # 		ReplayTest("PlayerController.createPlayer - normal mockMode", TestResult.PASS),
        # 		ReplayTest("PlayerController.createPlayer - mock mismatch integrationMode", TestResult.PASS),
        # 		ReplayTest("PlayerController.deletePlayerById - normal mockMode", TestResult.PASS),
        # 		ReplayTest("PlayerController.deletePlayerById - normal integrationMode", TestResult.PASS),
        # 		ReplayTest("ProductController.getAllProducts - no class def", TestResult.PASS),
        # 		ReplayTest("ProductController.getProductById - no class def", TestResult.PASS),
        # 		ReplayTest("ProductController.updateProduct - no class def", TestResult.PASS),
        # 		ReplayTest("RecursiveController.nthFibonacci - normal", TestResult.PASS),
        # 		ReplayTest("RecursiveController.factorial - normal", TestResult.PASS),
        #         ReplayTest("RedisOpsController.setValue - return value null", TestResult.PASS),
        #         ReplayTest("RedisOpsController.getValue - logging.record event returns null", TestResult.PASS),
        #         ReplayTest("SealedClassController.getAllVehicles2 - normal", TestResult.PASS),
        #         ReplayTest("SealedClassController.getAllVehicles - normal", TestResult.PASS),
        #         ReplayTest("UserController.deleteUser - normal", TestResult.PASS),
        #         ReplayTest("UserController.updateUser - wrong assertion", TestResult.PASS),
        #         ReplayTest("UserController.getUserById - wrong assertion", TestResult.PASS),
        #         ReplayTest("VarKeywordController.getObjectArrayVar - assertion fail", TestResult.PASS),
        #         ReplayTest("VarKeywordController.getStringVar - normal", TestResult.PASS),
        #         ReplayTest("VarKeywordController.getCharVar - normal", TestResult.PASS),
        #         ReplayTest("VarKeywordController.getIntegerArrayVar - normal", TestResult.PASS),
        #         ReplayTest("ExternalUserController.deduct - normal", TestResult.PASS),
        #         ReplayTest("ExternalUserController.deduct - normal", TestResult.PASS),
        #         ReplayTest("InventoryController.deduct - normal", TestResult.PASS),
        #         ReplayTest("InventoryController.deduct - normal", TestResult.PASS),
        #         ReplayTest("ShippingController.schedule - normal", TestResult.PASS),
        #         ReplayTest("ShippingController.schedule - normal", TestResult.PASS),
        #         ReplayTest("InventoryClient.deduct - mock mismatch", TestResult.PASS),
        #         ReplayTest("InventoryClient.buildErrorResponse - normal", TestResult.PASS),
        #         ReplayTest("ProductClient.getProduct - mock mismatch", TestResult.PASS),
        #         ReplayTest("ProductClient.getProduct - mock mismatch integMode", TestResult.PASS),
        #         ReplayTest("ShippingClient.buildErrorResponse - normal", TestResult.PASS),
        #         ReplayTest("ShippingClient.callShippingService - mock mismatch", TestResult.PASS),
        #         ReplayTest("ShippingClient.callShippingService - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("ShippingClient.schedule - mock mismatch", TestResult.PASS),
        #         ReplayTest("UserClient.buildErrorResponse - normal", TestResult.PASS),
        #         ReplayTest("UserClient.callUserService - mock mismatch", TestResult.PASS),
        #         ReplayTest("UserClient.callUserService - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("UserClient.deduct - mock mismatch", TestResult.PASS),
        #         ReplayTest("OrderController.placeOrder - mono is null", TestResult.PASS),
        #         ReplayTest("OrderController.placeOrder - mono is null in integrationMode", TestResult.PASS),
        #         ReplayTest("InventoryOrchestrator.isSuccess - normal", TestResult.PASS),
        #         ReplayTest("InventoryOrchestrator.create - mock mismatch", TestResult.PASS),
        #         ReplayTest("InventoryOrchestrator.create - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.toOrderResponse - normal", TestResult.PASS),
        #         ReplayTest("OrchestratorService.toOrderResponse - normal integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.doOrderPostProcessing - normal", TestResult.PASS),
        #         ReplayTest("OrchestratorService.doOrderPostProcessing - normal integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.getProduct - mock mismatch", TestResult.PASS),
        #         ReplayTest("OrchestratorService.getProduct - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.placeOrder - mono is null", TestResult.PASS),
        #         ReplayTest("OrderFulfillmentService.updateStatus - normal", TestResult.PASS),
        #         ReplayTest("OrderFulfillmentService.placeOrder - mock mismatch", TestResult.PASS),
        #         ReplayTest("PaymentOrchestrator.isSuccess - normal", TestResult.PASS),
        #         ReplayTest("PaymentOrchestrator.create - mock mismatch", TestResult.PASS),
        #         ReplayTest("PaymentOrchestrator.create - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("ShippingOrchestrator.isSuccess - normal", TestResult.PASS),
        #         ReplayTest("ShippingOrchestrator.create - mock mismatch", TestResult.PASS),
        #         ReplayTest("ShippingOrchestrator.create - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.buildPaymentRequest - normal", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.buildInventoryRequest - normal", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.buildShippingRequest - normal", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.buildRequestContext - normal", TestResult.PASS),
        #         ReplayTest("InventoryClient.buildErrorResponse - normal", TestResult.PASS),
        #         ReplayTest("InventoryClient.callInventoryService - mock mismatch", TestResult.PASS),
        #         ReplayTest("InventoryClient.callInventoryService - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("InventoryClient.deduct - mock mismatch", TestResult.PASS),
        #         ReplayTest("ProductClient.getProduct - mock mismatch", TestResult.PASS),
        #         ReplayTest("ProductClient.getProduct - mock mismatch integrationMode", TestResult.PASS),
        #         ReplayTest("ShippingClient.buildErrorResponse - normal", TestResult.PASS),
        #         ReplayTest("ShippingClient.callShippingService - fail", TestResult.PASS),
        #         ReplayTest("ShippingClient.callShippingService - fail integrationMode", TestResult.PASS),
        #         ReplayTest("ShippingClient.schedule - fail", TestResult.PASS),
        #         ReplayTest("UserClient.buildErrorResponse - normal", TestResult.PASS),
        #         ReplayTest("UserClient.callUserService - fail", TestResult.PASS),
        #         ReplayTest("UserClient.UserService - fail integrationMode", TestResult.PASS),
        #         ReplayTest("UserClient.deduct - fail", TestResult.PASS),
        #         ReplayTest("OrderController.placeOrder - fail", TestResult.PASS),
        #         ReplayTest("OrderController.placeOrder - fail integrationMode", TestResult.PASS),
        #         ReplayTest("InventoryOrchestrator.isSuccess - success", TestResult.PASS),
        #         ReplayTest("InventoryOrchestrator.create - null exception", TestResult.PASS),
        #         ReplayTest("InventoryOrchestrator.create - null exception integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.toOrderResponse - normal", TestResult.PASS),
        #         ReplayTest("OrchestratorService.toOrderResponse - normal integMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.doOrderPostProcessing - fail", TestResult.PASS),
        #         ReplayTest("OrchestratorService.doOrderPostProcessing - fail integrationMode", TestResult.PASS),
        #         ReplayTest("OrchestratorService.placeOrder - fail", TestResult.PASS),
        #         ReplayTest("OrderFulfillmentService.getProduct - fail", TestResult.PASS),
        #         ReplayTest("OrderFulfillmentService.getProduct - fail", TestResult.PASS),
        #         ReplayTest("OrderFulfillmentService.placeOrder - fail", TestResult.PASS),
        #         ReplayTest("PaymentOrchestrator.isSuccess - normal", TestResult.PASS),
        #         ReplayTest("PaymentOrchestrator.create - fail", TestResult.PASS),
        #         ReplayTest("PaymentOrchestrator.create - fail", TestResult.PASS),
        #         ReplayTest("ShippingOrchestrator.isSuccess - normal", TestResult.PASS),
        #         ReplayTest("ShippingOrchestrator.create - fail", TestResult.PASS),
        #         ReplayTest("ShippingOrchestrator.create - fail", TestResult.PASS),
        #         ReplayTest("DebugUtil.print - normal", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.buildShippingRequest - normal", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.InventoryRequest - normal", TestResult.PASS),
        #         ReplayTest("OrchestrationUtil.buildPaymentRequest - normal", TestResult.PASS),
        #         ReplayTest("BlueDartClient.getServiceOptions - fail", TestResult.PASS),
        #         ReplayTest("BlueDartClient.getServiceOptions - fail integrationMode", TestResult.PASS),
        #         ReplayTest("DHLClient.getServiceOptions - fail", TestResult.PASS),
        #         ReplayTest("DHLClient.getServiceOptions - fail integrationMode", TestResult.PASS),
        #         ReplayTest("DHLClient.normalizeResponse - normal", TestResult.PASS),
        #         ReplayTest("CarrierController.getCarriers - fail", TestResult.PASS),
        #         ReplayTest("CarrierController.getCarriers - fail integrationMode", TestResult.PASS),
        #         ReplayTest("MockBlueDartController.createResponse - fail", TestResult.PASS),
        #         ReplayTest("MockBlueDartController.createResponse - fail", TestResult.PASS),
        #         ReplayTest("MockBlueDartController.getServiceOptions - fail to parse", TestResult.PASS),
        #         ReplayTest("MockDHLController.getServiceOptions - fail", TestResult.PASS),
        #         ReplayTest("MockDHLController.responseList - fail", TestResult.PASS),
        #         ReplayTest("CarrierService.getCarriers - fail", TestResult.PASS),
        #         ReplayTest("CarrierService.getCarriers - fail integrationMode", TestResult.PASS),
        #         ReplayTest("TeacherController.getAllTeachers - normal", TestResult.PASS),
        #         ReplayTest("TeacherController.getAllTeachers - normal integ", TestResult.PASS),
        #         ReplayTest("TeacherController.getTeacherById_mockMode_happyPath", TestResult.PASS),
        #         ReplayTest("TeacherController.getTeacherById - normal integ", TestResult.PASS),
        #         ReplayTest("ContentEnrichmentService.getAllTeachers - normal", TestResult.PASS),
        #         ReplayTest("ContentEnrichmentService.enrichTeacherDetails - normal", TestResult.PASS),
        #         ReplayTest("RestaurantClient.getRestaurant - connection refused", TestResult.PASS),
        #         ReplayTest("ReviewClient.getReviews - connection refused", TestResult.PASS),
        #         ReplayTest("BulkHeadController.nonCPUIntensiveTask - normal", TestResult.PASS),
        #         ReplayTest("BulkHeadController.fib - normal", TestResult.PASS),
        #         ReplayTest("CalculatorController.doubleInput - normal", TestResult.PASS),
        #         ReplayTest("RestaurantAggregatorController.getProductAggregate - connection refused", TestResult.PASS),
        #         ReplayTest("MockRestaurantController.getRestaurant - normal", TestResult.PASS),
        #         ReplayTest("MockReviewController.getReviews - normal", TestResult.PASS),
        #         ReplayTest("RestaurantAggregatorService.aggregate - connection refused", TestResult.PASS),
        #         ReplayTest("CronService.getCount- normal", TestResult.PASS),
        #         ReplayTest("CronService.executeTask - normal", TestResult.PASS),
        #         ReplayTest("PlayerService.createPlayer - fail", TestResult.PASS),
        #         ReplayTest("PlayerService.createPlayer - fail integration", TestResult.PASS),
        #         ReplayTest("PlayerService.deletePlayerById - normal", TestResult.PASS),
        #         ReplayTest("PlayerService.deletePlayerById - normal integrationMode", TestResult.PASS),
        #         ReplayTest("ProductService.getAllProducts - no class def", TestResult.PASS),
        #         ReplayTest("ProductService.getProductById - no class def", TestResult.PASS),
        #         ReplayTest("ProductService.updateProduct - no class def", TestResult.PASS),
        #         ReplayTest("RecursiveService.nthFibonacci - normal", TestResult.PASS),
        #         ReplayTest("RecursiveService.fibonacci - normal", TestResult.PASS),
        #         ReplayTest("RecursiveService.factorial - normal", TestResult.PASS),
        #         ReplayTest("UserService.deleteUser - fail with null character", TestResult.PASS),
        #         ReplayTest("UserService.updateUser - fail with null character", TestResult.PASS),
        #         ReplayTest("UserService.getUserById - fail with null character", TestResult.PASS),
        #         ReplayTest("UserUtil.userEntityToUserDto_mockMode_unexpectedFailure", TestResult.PASS),
        #         ReplayTest("UserUtil.userEntityToUserDto_integrationMode_happyPath", TestResult.PASS),
        #         ReplayTest("UserUtil.userDtoToUserEntity_mockMode_unexpectedFailure", TestResult.PASS),
        #         ReplayTest("UserUtil.userDtoToUserEntity_integrationMode_happyPath", TestResult.PASS),
        #         ReplayTest("VarKeywordController.getObjectVar - normal", TestResult.PASS),
        #         ReplayTest("ExternalProductController.getProduct - normal", TestResult.PASS),
        #         ReplayTest("Orchestrator.statusHandler - normal", TestResult.PASS)
        # 	]
        # ),
        Target(
        	"https://github.com/unloggedio/unlogged-spring-mvc-maven-demo",
        	"unlogged-spring-mvc-maven-demo",
        	"/pom.xml",
        	"/src/main/java/org/unlogged/mvc/demo/Application.java",
        	buildSystem.MAVEN,
            target_run_properties = TargetRunProperties("main","17", StartMode.CMD),
            replay_test_options= ReplayTestOptions(
        	[
        		ReplayTest("loadAllBooks - normal", TestResult.PASS),
        		ReplayTest("getBookById - normal", TestResult.PASS),
        		ReplayTest("getAllBooks - normal", TestResult.PASS),
        		ReplayTest("insertBook - normal", TestResult.PASS),
        		ReplayTest("updateBook - normal", TestResult.PASS),
        		ReplayTest("deleteBook - normal", TestResult.PASS)
        	])
        )
    ]

    passing = True
    result_maps = []
    report_generator = Report_Generator(ReportType.REPLAY)
    for local_target in target_list:
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
