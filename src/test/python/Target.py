import os
import xml.etree.ElementTree as ET
from TestResult import TestResult
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
	def __init__(self, test_repo_url, test_repo_name, rel_dependency_path, rel_main_path, build_system, test_response):
		self.test_repo_url = test_repo_url
		self.test_repo_name = test_repo_name
		self.rel_dependency_path = rel_dependency_path
		self.rel_main_path = rel_main_path
		self.build_system = build_system
		self.test_response = test_response

	def modify_pom(self, sdk_version, in_docker):

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
			if (in_docker):
				proc = subprocess.Popen(["docker container ls --all --quiet --filter 'name=conf-demo-app'"], stdout=subprocess.PIPE, shell=True)
				(out_stream, err_stream) = proc.communicate()
				docker_container_id = str(out_stream)
				docker_container_id = docker_container_id[2:][:-3]
				os.system("docker exec -it " + docker_container_id + " apt-get update")
				os.system("docker exec -it " + docker_container_id + " apt-get install -y maven")
				print ("maven installed!!!!")
				os.system("docker exec -it " + docker_container_id + " mvn versions:use-latest-versions -DallowSnapshots=true -Dincludes=video.bug:unlogged-sdk -f " + pom_path)
				print ("maven updated dependency locally!!!!")
			else:
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
		for local_test in self.test_response:
			print ("test_name = " + local_test.test_name)
			print ("result_ideal = " + local_test.result_ideal)
