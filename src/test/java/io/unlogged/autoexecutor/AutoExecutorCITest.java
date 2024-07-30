package io.unlogged.autoexecutor;

import io.unlogged.autoexecutor.testutils.autoCIUtils.AgentClientLite;
import io.unlogged.autoexecutor.testutils.autoCIUtils.AssertionUtils;
import io.unlogged.autoexecutor.testutils.autoCIUtils.ParseUtils;
import io.unlogged.autoexecutor.testutils.autoCIUtils.XlsxUtils;
import io.unlogged.autoexecutor.testutils.entity.AutoAssertionResult;
import io.unlogged.autoexecutor.testutils.entity.TestResultSummary;
import io.unlogged.autoexecutor.testutils.entity.TestUnit;
import io.unlogged.command.AgentCommand;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandRequestType;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.mocking.DeclaredMock;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AutoExecutorCITest {

    private final Logger logger = Logger.getLogger("AutoExecutorLog");
    private FileHandler fh;
    final private String testResourcesPath = "auto-test-resources/";
    final private boolean printOnlyFailing = false;

    private void setupLogger(String project) {
        String autoExecutorResourcesPath = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath).getPath();
        String reportsPath = autoExecutorResourcesPath + "AutoExecutorReports/";
        new File(reportsPath).mkdirs();
        try {
            fh = new FileHandler(reportsPath + project + "_report.txt", true);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.info("Starting AutoExecutor For : " + project);
        } catch (Exception e) {

        }
    }

    //mvn test -Dtest=AutoExecutorCITest#startUnloggedMavenDemoTest
    @Test
    public void startUnloggedMavenDemoTest() {
        TreeMap<String, URL> testConfig = new TreeMap<>();
        URL pathToIntegrationResources = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "maven-demo-integration-resources.xlsx");
        URL pathToMockResources = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "maven-demo-mocked-resources.xlsx");
        testConfig.put("Integration", pathToIntegrationResources);
        testConfig.put("Unit", pathToMockResources);
        setupLogger("unlogged-spring-maven-demo");
        runTests(testConfig);
    }

    //mvn test -Dtest=AutoExecutorCITest#startWebfluxDemoTest
    @Test
    public void startWebfluxDemoTest() {
        TreeMap<String, URL> testConfig = new TreeMap<>();
        URL pathToIntegrationResources = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "webflux-demo-integration-resources.xlsx");
        URL pathToMockResources = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "webflux-demo-unit-resources.xlsx");
        testConfig.put("Integration", pathToIntegrationResources);
        testConfig.put("Unit", pathToMockResources);
        setupLogger("unlogged-spring-maven-demo");
        runTests(testConfig);
    }


    public void runTests(TreeMap<String, URL> testConfigs) {
        AgentClientLite agentClientLite = new AgentClientLite();
        boolean isConnected = agentClientLite.isConnected();
        if (!isConnected) {
            System.out.println("Skipping : Agent not connected");
            logger.info("Skipping autoExecutor Run, Agent not connected");
            return;
        }

        List<TestResultSummary> resultSummaries = new ArrayList<>();

        for (String testMode : testConfigs.keySet()) {
            logger.info("\n-----" + testMode + " mode testing-----\n");
            TestResultSummary summary = executeMethods(testConfigs.get(testMode), agentClientLite);
            summary.setMode(testMode);
            resultSummaries.add(summary);
        }

        logger.info("\n-----Test Summary-----\n");
        boolean overallStatus = true;
        for (TestResultSummary resultSummary : resultSummaries) {
            logger.info("\n    " + resultSummary.getMode() + " Mode ->" + "\n"
                    + "    Total number of cases run : " + resultSummary.getNumberOfCases() + "\n"
                    + "    Number of Failing cases : " + resultSummary.getFailingCasesCount() + "\n"
                    + "    Failing tests (Row numbers) : " + resultSummary.getFailingCases().toString());
            if (resultSummary.getFailingCasesCount() > 0) {
                overallStatus = false;
            }
        }
        Assertions.assertTrue(overallStatus);
    }

    public TestResultSummary executeMethods(URL pathToUrl, AgentClientLite agentClientLite) {
        XSSFWorkbook workbook = XlsxUtils.getWorkbook(pathToUrl);
        assert workbook != null;

        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        int count = 0;
        int passing = 0;
        int failing = 0;
        List<Integer> failingCaseIds = new ArrayList<>();

        while (rowIterator.hasNext()) {
            if (count == 0) {
                count++;
                rowIterator.next();
            } else {
                Row row = rowIterator.next();
                count++;

                //stop if a row elem is null, iterator will keep going
                if (row.getCell(0) == null) {
                    break;
                }

                String targetClassname = row.getCell(0).getStringCellValue();
                String targetMethodInfo = row.getCell(1).getStringCellValue();
                String selectedImplementation = row.getCell(2).getStringCellValue();
                if (selectedImplementation == null || selectedImplementation.isEmpty()) {
                    selectedImplementation = targetClassname;
                }
                String responseType = row.getCell(3).getStringCellValue();
                String methodInput = row.getCell(4).getStringCellValue();
                String methodAssertionType = row.getCell(5).getStringCellValue();
                Cell outputCell = row.getCell(6);
                String methodOutput = "";
                if (outputCell != null) {
                    //can be null when an empty value is present in cell
                    try {
                        methodOutput = row.getCell(6).getStringCellValue();
                    } catch (IllegalStateException illegalStateException) {
                        //get numeric value
                        methodOutput = String.valueOf(row.getCell(6).getNumericCellValue());
                    }
                }
                String declaredMocks = row.getCell(9).getStringCellValue();
                Cell commentCell = row.getCell(11);
                String caseComment = null;
                if (commentCell != null) {
                    caseComment = commentCell.getStringCellValue();
                }

                String[] methodParts = targetMethodInfo.split("\\n");
                assert methodParts.length == 2;

                String methodName = methodParts[0];
                String methodSignature = methodParts[1];

                List<String> types = new ArrayList<>();
                List<String> parameters = new ArrayList<>();
                if (!methodInput.equals("[]")) {
                    Map<String, List<String>> typesAndParams = ParseUtils.getTypeAndParameter(methodInput);
                    types = typesAndParams.get("types");
                    parameters = typesAndParams.get("parameters");
                }

                //Create an Agent Command request and execute with it.
                AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
                agentCommandRequest.setCommand(AgentCommand.EXECUTE);
                agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
                agentCommandRequest.setMethodSignature(methodSignature);
                agentCommandRequest.setClassName(selectedImplementation);
                agentCommandRequest.setMethodName(methodName);
                agentCommandRequest.setParameterTypes(types);
                agentCommandRequest.setMethodParameters(parameters);

                List<DeclaredMock> declaredMocksList = ParseUtils.getDeclaredMocksFrom(declaredMocks);
                agentCommandRequest.setDeclaredMocks(declaredMocksList);

                boolean shouldPrint = true;
                try {
                    AgentCommandResponse agentCommandResponse = agentClientLite.executeCommand(agentCommandRequest);
                    TestUnit testUnit = new TestUnit(targetClassname, methodName, methodSignature,
                            methodInput, methodAssertionType, methodOutput, responseType, agentCommandRequest,
                            agentCommandResponse);
                    AutoAssertionResult result = AssertionUtils.assertCase(testUnit);
                    if (!result.isPassing()) {
                        failing++;
                        failingCaseIds.add(count);
                    } else {
                        passing++;
                    }
                    if (printOnlyFailing && result.isPassing()) {
                        shouldPrint = false;
                    }
                    if (shouldPrint) {
                        logger.info(">\n    [Case " + count + " (Row)] is [" + ((result.isPassing()) ? "Passing]]" : "Failing]]") + "\n"
                                + "    Classname : " + targetClassname + "\n"
                                + "    MethodName : " + methodName + "\n"
                                + "    Implementation : " + selectedImplementation + "\n"
                                + "    Assertion type : " + result.getAssertionType() + "\n"
                                + "    Message : " + result.getMessage() + "\n"
                                + "    Case Comment : " + caseComment
                                + (!result.isPassing() ? "\n    Raw Response : " + limitResponseSize(String.valueOf(agentCommandResponse.getMethodReturnValue())) : ""));
                    }
                } catch (IOException e) {
                    logger.warning("Execution failed " + e);
                }
            }
        }
        return new TestResultSummary(count - 2, passing, failing, failingCaseIds);
    }

    private String limitResponseSize(String input) {
        if (input != null && input.length() > 5000) {
            return input.substring(0, 5000) + " ... ";
        } else return input;
    }
}
