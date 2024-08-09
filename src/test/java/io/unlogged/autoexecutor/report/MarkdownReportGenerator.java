package io.unlogged.autoexecutor.report;

import io.unlogged.autoexecutor.testutils.entity.AssertionDetails;
import io.unlogged.autoexecutor.testutils.entity.TestResultSummary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MarkdownReportGenerator {

    private String filename;

    public static void generateAndWriteMarkdownReport(String project, String path, List<TestResultSummary> testResultSummaryList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("## Project : " + project);

        testResultSummaryList.forEach(testResultSummary ->
        {
            stringBuilder
                    .append("\n\n")
                    .append("### Mode : " + testResultSummary.getMode()).append("\n\n")
                    .append("| Status   | Passing | Failing | Total |").append("\n")
                    .append("|----------|---------|---------|-------|").append("\n")
                    .append("| " + (testResultSummary.getFailingCasesCount() == 0 ? "Pass" : "Fail") + "     | " +
                            testResultSummary.getPassingCasesCount() + "       | " +
                            testResultSummary.getFailingCasesCount() + "       | " +
                            (testResultSummary.getPassingCasesCount() + testResultSummary.getFailingCasesCount()) + "    |")
                    .append("\n\n")
                    .append("<details>\n" +
                            "<summary>Failing cases</summary>\n" +
                            "\n<ul>");
            if (testResultSummary.getFailingCasesCount() == 0) {
                stringBuilder.append("<li>There are no failing cases.</li>");
            } else {
                testResultSummary.getFailingCases().forEach(failingCase -> {
                    stringBuilder.append(generateFailingCaseSummary(failingCase));
                });
            }
            stringBuilder.append("\n\n").append("</ul></details>").append("\n")
                    .append(generatePieChart(testResultSummary))
                    .append("\n\n");

        });
        writeFile(project + "-summary", stringBuilder.toString(), path);
    }

    private static String generateFailingCaseSummary(AssertionDetails assertionDetails) {
        return new StringBuilder().append("\n")
                .append("<li><details>\n")
                .append("<summary> Case ID : ").append(assertionDetails.getCaseId()).append("</summary>\n\n")
                .append("| Operation Type | ").append(assertionDetails.getAssertionType()).append(" |\n")
                .append("|----------------|------|\n")
                .append("| Expected | ").append(capSize(assertionDetails.getExpected())).append("| \n")
                .append("| Actual | ").append(capSize(assertionDetails.getActual())).append("| \n\n")
                .append("</details></li>\n\n").toString();
    }

    public static void generateReportForSkippedTest(String project, String path) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("## Project : `" + project + "`")
                .append("\n\n")
                .append("Not running tests as Agent is not connected.")
                .append("\n\n");
        writeFile(project + "-summary", stringBuilder.toString(), path);
    }

    private static void writeFile(String filename, String contents, String path) {
        try {
            File file = new File(path + filename + ".md");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            System.out.println("Failed to write markdown report.");
        }
    }

    private static String generatePieChart(TestResultSummary testResultSummary) {
        StringBuilder pieChartBuilder = new StringBuilder();
        pieChartBuilder.append("<details>").append("\n")
                .append("<summary>Status Chart</summary>").append("\n\n")
                .append("```mermaid\n" +
                        "%%{init: {'theme': 'base', 'themeVariables': {'pie1': '#238636', 'pie2': '#da3633','primaryTextColor': '#fff', 'primaryBorderColor': '#7C0000'}}}%%\n" +
                        "pie title Status Chart\n" +
                        "    \"Passing\" : " + testResultSummary.getPassingCasesCount() + "\n" +
                        "    \"Failing\" : " + testResultSummary.getFailingCasesCount() + "\n" +
                        "```").append("\n")
                .append("</details>").append("\n\n");
        return pieChartBuilder.toString();
    }

    private static String capSize(String payload) {
        if (payload.length() > 10000) {
            return "Too large to show here, please refer to Logs";
        }
        return payload;
    }
}
