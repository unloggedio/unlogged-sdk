import os
import xml.etree.ElementTree as ET
from configEnum import TestResult, StartMode
import subprocess
from pathlib import Path

# define constants
main_method_identifier = "public static void main"
unlogged_annotation = "@Unlogged(port = " + "12100" + ")"
unlogged_annotation_base_string = "Unlogged"
unlogged_import = "import io.unlogged.Unlogged;"


class ReplayTest:
	def __init__(self, test_name, result_ideal):
		self.test_name = test_name
		self.result_ideal = result_ideal

class ReplayTestOptions:
	def __init__(self,test_response):
		self.test_response=test_response

class CompileTestOptions:
	def __init__(self, project_type):
		self.project_type = project_type

class TargetRunProperties:
	def __init__(self, branch_name="main", java_version="17", start_mode=StartMode.DOCKER):
		self.branch_name = branch_name
		self.java_version=java_version
		self.start_mode=start_mode

class AutoExecutorProperties:
	def __init__(self, test_method=""):
		self.test_method=test_method


class Target:
	def __init__(self, test_repo_url, test_repo_name, rel_dependency_path, rel_main_path, buildSystem,
				 target_run_properties, compile_test_options = None, replay_test_options = None, autoexecutor_properties = None):
		self.test_repo_url = test_repo_url
		self.test_repo_name = test_repo_name
		self.rel_dependency_path = rel_dependency_path
		self.rel_main_path = rel_main_path
		self.buildSystem = buildSystem
		self.target_run_properties = target_run_properties
		self.compile_test_options = compile_test_options
		self.replay_test_options = replay_test_options
		self.autoexecutor_properties = autoexecutor_properties

	def set_java_home(self,java_home):
		os.environ["JAVA_HOME"] = java_home
		os.environ["PATH"] = os.path.join(java_home, "bin") + ":" + os.environ["PATH"]

	def check_java_version(self,expected_version):
		result = subprocess.run(["java", "-version"], capture_output=True, text=True)
		if result.returncode != 0:
			raise Exception(f"Failed to check Java version: {result.stderr}")

		version_output = result.stderr.split('\n')[0]
		version = version_output.split('"')[1]

		if version.startswith("1.8"):
			version = "8"
		else:
			version = version.split('.')[0]

		if not version == expected_version:
			raise Exception(f"Java version {version} does not match expected version {expected_version} - Failing")
		print(f"Java version {version} matches expected version {expected_version} - Passing")


	def modify_pom(self, sdk_version):

		pom_path = self.test_repo_name + self.rel_dependency_path
		# parse file
		ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")
		ET.register_namespace('xsi', "http://www.w3.org/2001/XMLSchema-instance")
		ET.register_namespace('schemaLocation',
							  "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd")
		tree = ET.parse(pom_path)
		root = tree.getroot()

		# search dependency
		dependency_present = False
		for child in root.iter("{http://maven.apache.org/POM/4.0.0}artifactId"):
			if (child.text == "unlogged-sdk"):
				dependency_present = True

		if dependency_present:
			# update dependency
			os.system(
				"mvn versions:use-latest-versions -DallowSnapshots=true -Dincludes=video.bug:unlogged-sdk -f " + pom_path)


		else:
			# add dependency
			dependency = ET.Element("dependency")
			ET.SubElement(dependency, "groupId").text = "video.bug"
			ET.SubElement(dependency, "artifactId").text = "unlogged-sdk"
			ET.SubElement(dependency, "version").text = sdk_version

			for child in root:
				if (child.tag == "{http://maven.apache.org/POM/4.0.0}dependencies"):
					child.append(dependency)

			# write file
			ET.indent(tree, space="\t", level=0)
			tree.write(pom_path, encoding="UTF-8", xml_declaration=True)

	def modify_gradle(self, sdk_version):

		# read file
		gradle_path = self.test_repo_name + self.rel_dependency_path
		with open(gradle_path, "r") as file:
			file = [line.rstrip('\n') for line in file]

			unlogged_dependency = []
			dependencies_index = -1
			for index in range(len(file)):
				# dependency block
				if ("dependencies" in file[index]):
					dependencies_index = index

				# unlogged dependency
				elif ("video.bug:unlogged-sdk" in file[index]):
					unlogged_dependency.append(index)

			# remove unlogged dependency if it exists
			for i in reversed(unlogged_dependency):
				file.pop(i)

			# add unlogged dependency with latest version
			unlogged_dependency_implementation = "implementation 'video.bug:unlogged-sdk:" + sdk_version + "'"
			unlogged_dependency_annotation = "annotationProcessor 'video.bug:unlogged-sdk:" + sdk_version + "'"
			file.insert(dependencies_index + 1, unlogged_dependency_implementation)
			file.insert(dependencies_index + 2, unlogged_dependency_annotation)

		# write file
		with open(gradle_path, "w") as file_new:

			for line in file:
				file_new.write(line)
				file_new.write("\n")

	def modify_main(self):

		main_path = self.test_repo_name + self.rel_main_path
		# read file
		with open(main_path, "r") as file:
			file = [line.rstrip('\n') for line in file]

			# check annotation
			annotation_present = False
			for line in file:
				if (unlogged_annotation_base_string in line):
					annotation_present = True

			# add annotation
			if not annotation_present:
				line_count = 0
				for index in range(len(file)):
					if (main_method_identifier in file[index]):
						line_count = index
						break

				file.insert(1, unlogged_import)
				file.insert(line_count + 1, unlogged_annotation)

		# write file
		with open(main_path, "w") as file_new:

			for line in file:
				file_new.write(line)
				file_new.write("\n")

	def check_replay(self):

		# create dictionary from configuration
		expected_response_dict = {}
		for local_test in self.replay_test_options.test_response:
			expected_response_dict[local_test.test_name] = local_test.result_ideal

		# Create path if it doesn't exist
		Path("replayReports/").mkdir(parents=True, exist_ok=True)
		# parse report
		report_name="replay_report_"+self.format_report_name(self.test_repo_name)+"_"+self.target_run_properties.java_version+".xml"
		report_path = "replayReports/"+report_name

		if self.target_run_properties.start_mode == StartMode.DOCKER:
			# Default docker maven filepath
			docker_container_name = "target-repo"
			copy_cmd = "docker cp " + docker_container_name + ":/target/surefire-reports/TEST-UnloggedRunnerTest.xml " + report_path
		else :
			# Default local maven filepath
			local_report_path = self.test_repo_name+"/target/surefire-reports/TEST-UnloggedTests.xml"
			copy_cmd = "cp "+local_report_path+" "+report_path

		print("copy_cmd = " + copy_cmd)
		copy_cmd = subprocess.Popen([copy_cmd], stdout=subprocess.PIPE, shell=True)
		(copy_cmd_std, copy_cmd_err) = copy_cmd.communicate()

		ET.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')
		ET.register_namespace('noNamespaceSchemaLocation',
							  'https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd')

		print("Report Path : ",report_path)
		report_file = Path(report_path)
		if not report_file.is_file():
			print("Exception : Report file not found")
			result_map = dict()
			result_map['java_version'] = self.target_run_properties.java_version
			result_map['status'] = TestResult.FAIL
			result_map['tot'] = "0"
			result_map['passing'] = "0"
			result_map['case_result'] = []
			result_map['run_state'] = False
			result_map['message'] = "Report file not found"
			return result_map

		tree_report = ET.parse(report_path)
		tree_root = tree_report.getroot()

		actual_response_dict = {}
		for local in tree_root:
			if (local.tag == "testcase"):
				test_name = local.attrib["name"]

				actual_result = TestResult.PASS
				for metadata in local:
					if (metadata.tag == "failure"):
						actual_result = TestResult.FAIL

				actual_response_dict[test_name] = actual_result

		replay_fail = []
		test_case_results = []
		total = len(expected_response_dict)
		for local_test in expected_response_dict:
			print("Test name = " + local_test)
			print("	Expected value = " + expected_response_dict[local_test].name)
			print("	Actual value = " + actual_response_dict[local_test].name)
			print("----")
			case_result = dict()
			case_result['name'] = local_test

			if (expected_response_dict[local_test] != actual_response_dict[local_test]):
				replay_fail.append(local_test)
				case_result['result'] = TestResult.FAIL
			else :
				case_result['result'] = TestResult.PASS
			test_case_results.append(case_result)

		result_map = dict()
		result_map['tot'] = str(total)
		if (len(replay_fail) == 0):
			print("All tests passed succesfully")
			result_map['status'] = TestResult.PASS
			result_map['passing'] = str(total)
			result_map['case_result'] = test_case_results

		else:
			print("Replay tests have failed for " + self.test_repo_name + ". Fail count = " + str(len(replay_fail)))
			for local_test in replay_fail:
				print("Test Case: " + local_test)
				result_map['status'] = TestResult.FAIL

			result_map['passing'] = str(total-len(replay_fail))
			result_map['case_result'] = test_case_results

		result_map['run_state'] = True
		result_map['message'] = "Tests ran"
		return result_map

	def format_report_name(self,repo_name):
		return repo_name.replace("-","_")
