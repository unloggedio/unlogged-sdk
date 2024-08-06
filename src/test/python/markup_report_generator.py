from configEnum import ReportType, buildSystem, TestResult
from Target import Target


class Report_Generator:

    table_format = True
    def __init__(self, mode):
        self.mode = mode
        self.result_map = dict()

    def generate_and_write_report(self):
        repoting_content = ""
        filename = ''
        if self.mode == ReportType.COMPILE:
            report_content = "## Compile Pipeline Results\n"
            filename = 'compile-pipeline-result.md'
            for key in self.result_map:
                report_content += "### Project : "+key+"\n\n"
                results = self.result_map[key]
                report_content += "| Java Version | Status  | Information |\n"
                report_content += "|--------------|---------|-------------|\n"
                for summary in results:
                    if self.table_format == True:
                        report_content += "| "+summary['java_version']+"|"+self.get_status_string(summary['status'],True)+"|"+summary['information']+"|\n"
                    else:
                        report_content += "- *Java version* : "+summary['java_version']+"\n"
                        report_content += "- *Status* : "+self.get_status_string(summary['status'])+"\n"
                        report_content += "- *Information* : "+summary['information']+"\n\n"
                report_content += "\n\n"
        else:
            report_content = "## Replay Pipeline Results\n"
            filename = 'replay-pipeline-result.md'
            for key in self.result_map:
                report_content += "### Project : "+key+"\n\n"
                project_summary = self.result_map[key]
                for summary in project_summary:
                    java_version = summary['java_version']
                    status = summary['status']
                    total = summary['tot']
                    passing_count = summary['passing']
                    case_results = summary['case_result']

                    report_content += "*Java version* : "+java_version+"\n\n"
                    report_content += "*Passing Count* : "+passing_count+"/"+total+"\n\n"
                    report_content += "*Status* : "+self.get_status_string(status)+"\n\n"

                    report_content += "| Test ID | Status |\n"
                    report_content += "|---------|--------|\n"

                    for case in case_results:
                        test_name = case['name']
                        status = case['result']
                        report_content += "| "+test_name+" | "+self.get_status_string(status,True)+" |\n"
                    report_content += "\n\n"

        report_file = open(filename, 'w')
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

    def add_replay_result_status_entry(self, target, result_map):
        if target.test_repo_name not in self.result_map :
            self.result_map[target.test_repo_name] = []

        new_entry = dict()
        new_entry['java_version'] = target.java_version
        new_entry['status'] = result_map['status']
        new_entry['tot'] = result_map['tot']
        new_entry['passing'] = result_map['passing']
        new_entry['case_result'] = result_map['case_result']
        self.result_map[target.test_repo_name].append(new_entry)


    def get_status_string(self, test_result, icon=True):
        if test_result == TestResult.PASS:
            if icon:
                return ":white_check_mark: Passing"
            else:
                return "Passing"
        else:
            if icon:
                return ":red_circle: Failing"
            else:
                return "Failing"




