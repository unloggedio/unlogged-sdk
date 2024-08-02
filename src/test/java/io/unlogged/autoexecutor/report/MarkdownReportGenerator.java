package io.unlogged.autoexecutor.report;

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
        stringBuilder.append("## Project : `" + project + "`");

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
                    .append("### Failing cases").append("\n");

            if (testResultSummary.getFailingCasesCount() == 0) {
                stringBuilder.append("There are no failing cases.");
            } else {
                testResultSummary.getFailingCases().forEach(failingCaseId -> {
                    stringBuilder.append("- " + failingCaseId).append("\n");
                });
            }
        });
        writeFile(project + "-summary", stringBuilder.toString(), path);
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
}
