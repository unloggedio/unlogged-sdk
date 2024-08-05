from configEnum import ReportType, buildSystem, TestResult
from Target import Target


class Report_Generator:

    table_format = True
    def __init__(self, mode):
        self.mode = mode
        self.result_map = dict()

    def generate_and_write_report(self):
        repoting_content = ""
        if self.mode == ReportType.COMPILE:
            report_content = "## Compile Pipeline Results\n"
        else:
            report_content = "## Replay Pipeline Results\n"

        for key in self.result_map:
            report_content += "### Project : "+key+"\n\n"
            results = self.result_map[key]

            report_content += "| Java Version | Status  | Information |\n"
            report_content += "|--------------|---------|-------------|\n"
            for summary in results:
                if self.table_format == True:
                    report_content += "| "+summary['java_version']+"|"+self.get_status_string(summary['status'])+"|"+summary['information']+"|\n"
                else:
                    report_content += "- *Java version* : "+summary['java_version']+"\n"
                    report_content += "- *Status* : "+self.get_status_string(summary['status'])+"\n"
                    report_content += "- *Information* : "+summary['information']+"\n\n"
            report_content += "\n\n"

        report_file = open('compile-pipeline-result.md', 'w')
        report_file.write(report_content)
        report_file.close()

    def add_compile_result_status_entry(self, target, test_result, info):
        if target.test_repo_name not in self.result_map :
            self.result_map[target.test_repo_name] = []

        new_entry = dict()
        new_entry['java_version'] = target.java_version
        new_entry['status'] = test_result
        new_entry['information'] = info
        self.result_map[target.test_repo_name].append(new_entry)

    def get_status_string(self, test_result):
        if test_result == TestResult.PASS:
            return ":white_check_mark: Passing"
        return ":red_circle: Failing"


