from configEnum import ReportType, buildSystem, TestResult
from Target import Target

class ReportGenerator:

    table_format = True
    def __init__(self, mode):
        self.mode = mode

    def write_to_file(self, contents, filename):
        report_file = open(filename, 'a')
        report_file.write(contents)
        report_file.close()

    def write_compile_report(self, results):
        filename = "compile-pipeline-result.md"
        report_content = ""

        for project in results:
            result_summaries = results[project]
            project_name = project
            report_content += "### Project : "+project_name+"\n\n"
            report_content += "| Java Version | Status  | Information |\n"
            report_content += "|--------------|---------|-------------|\n"
            for summary in result_summaries:
                target = summary['target']
                java_version = target.target_run_properties.java_version
                status = summary['status']
                information = summary['information']

                report_content += "| "+java_version+"|"+self.get_status_string(status,True)+"|"+information+"|\n"
            report_content += "\n\n"
        self.write_to_file(report_content,filename)

    def write_replay_report(self, target, result_map):
        java_version = target.target_run_properties.java_version
        run_state = result_map['run_state']
        status = result_map['status']
        tot = result_map['tot']
        passing = result_map['passing']
        message = result_map['message']
        case_result = result_map['case_result']
        project_name = target.test_repo_name

        report_content = ""
        filename = 'replay-pipeline-result.md'

        report_content += "### Project : "+project_name+"\n\n"
        report_content += "*Java version* : "+java_version+"\n\n"
        report_content += "*Passing Count* : "+passing+"/"+tot+"\n\n"
        report_content += "*Status* : "+self.get_status_string(status)+"\n\n"

        if not run_state:
            report_content += "*Failure message : "+message+"*\n\n"

        else :
            report_content += "| Test ID | Status |\n"
            report_content += "|---------|--------|\n"

            for case in case_result:
                test_name = case['name']
                status = case['result']
                report_content += "| "+test_name+" | "+self.get_status_string(status,True)+" |\n"
            report_content += "\n\n"

        self.write_to_file(report_content, filename)

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