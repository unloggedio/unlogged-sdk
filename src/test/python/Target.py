import os
import xml.etree.ElementTree as ET
from configEnum import TestResult
import subprocess

# define constants
main_method_identifier = "public static void main"
unlogged_annotation = "@Unlogged"
unlogged_import = "import io.unlogged.Unlogged;"

class ReplayTest:
	def __init__(self, test_name, result_ideal):
		self.test_name = test_name
		self.result_ideal = result_ideal

class Target:
	def __init__(self, test_repo_url, test_repo_name, rel_dependency_path, rel_main_path, buildSystem, test_response = [], projectType = "Normal", branch_name = "main", java_version = "17"):
		self.test_repo_url = test_repo_url
		self.test_repo_name = test_repo_name
		self.rel_dependency_path = rel_dependency_path
		self.rel_main_path = rel_main_path
		self.buildSystem = buildSystem
		self.test_response = test_response
		self.projectType = projectType
		self.branch_name = branch_name
		self.java_version = java_version

	def modify_pom(self, sdk_version):

		pom_path = self.test_repo_name + self.rel_dependency_path
		# parse file
		ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")
		ET.register_namespace('xsi', "http://www.w3.org/2001/XMLSchema-instance")
		ET.register_namespace('schemaLocation', "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd")
		tree = ET.parse(pom_path)
		root = tree.getroot()

		# search dependency
		dependency_present = False
		for child in root.iter("{http://maven.apache.org/POM/4.0.0}artifactId"):
			if (child.text == "unlogged-sdk"):
				dependency_present = True

		if dependency_present:
			# update dependency
			os.system("mvn versions:use-latest-versions -DallowSnapshots=true -Dincludes=video.bug:unlogged-sdk -f " + pom_path)

		
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
				if (unlogged_annotation in line):
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
		for local_test in self.test_response:
			expected_response_dict[local_test.test_name] = local_test.result_ideal

		# parse report
		report_path = "replay_report.xml"
		docker_container_name = "target-repo"

		copy_cmd = "docker cp " + docker_container_name + ":/target/surefire-reports/TEST-UnloggedRunnerTest.xml " + report_path
		print ("copy_cmd = " + copy_cmd)
		copy_cmd = subprocess.Popen([copy_cmd], stdout=subprocess.PIPE, shell=True)
		(copy_cmd_std, copy_cmd_err) = copy_cmd.communicate()

		ET.register_namespace ('xsi', 'http://www.w3.org/2001/XMLSchema-instance')
		ET.register_namespace ('noNamespaceSchemaLocation', 'https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd')
		
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
		for local_test in expected_response_dict:
			print ("Test name = " + local_test)
			print ("	Expected value = " + expected_response_dict[local_test].name)
			print ("	Actual value = " + actual_response_dict[local_test].name)
			print ("----")
			
			if (expected_response_dict[local_test] != actual_response_dict[local_test]):
				replay_fail.append(local_test)
	
		if (len(replay_fail) == 0):
			print ("All tests passed succesfully")
		else:
			print ("Some tests failed. There are:")
			for local_test in replay_fail:
				print ("Test Case: " + local_test)
			raise Exception("Replay tests have failed")
