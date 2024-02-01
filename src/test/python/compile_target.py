import os
import sys
import xml.etree.ElementTree as ET
from enum import Enum

class build_system(Enum):
	MAVEN = 1
	GRADLE = 2

class Target:
	def __init__(self, test_repo_url, test_repo_name, rel_dependency_path, rel_main_path, build_system):
		self.test_repo_url = test_repo_url
		self.test_repo_name = test_repo_name
		self.rel_dependency_path = rel_dependency_path
		self.rel_main_path = rel_main_path
		self.build_system = build_system

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

	# def modify_gradle(self, sdk_version):
	# 	continue

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

def compile_target (target):
	
	# clone target
	os.system("git clone " + target.test_repo_url)
	
	# modify build system file
	target.modify_main()
	if (target.build_system == build_system.MAVEN):
		target.modify_pom(sdk_version)
		compile_command = "cd " + target.test_repo_name + " && mvn clean compile"
	elif (target.build_system == build_system.GRADLE):
		target.modify_gradle(sdk_version)
		compile_command = "cd " + target.test_repo_name + " && gradle clean compileJava"
	
	# target compile
	response_code = os.system(compile_command)
	if (response_code == 0):
		print ("Target compiled succesfully: " +  target.test_repo_name)
	else:
		raise Exception("Target did not compiled: " + target.test_repo_name)

	# delete target
	os.system("rm -rf " + target.test_repo_name)



if __name__=="__main__":

	# get constants
	main_method_identifier = "public static void main"
	unlogged_annotation = "@Unlogged"
	unlogged_import = "import io.unlogged.Unlogged;"
	sdk_version = sys.argv[1]

	target_list = [
		# unlogged-spring-maven-demo in github
		Target(
			"https://github.com/unloggedio/unlogged-spring-maven-demo",
			"unlogged-spring-maven-demo",
			"/pom.xml",
			"/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java",
			build_system.MAVEN
		),
		# unlogged-spring-maven-demo without sdk
		Target(
			"https://github.com/kartikeytewari-ul/unlogged-spring-maven-demo-without-sdk",
			"unlogged-spring-maven-demo-without-sdk",
			"/pom.xml",
			"/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java",
			build_system.MAVEN
		)
	]
		
	for local_target in target_list:
		compile_target(local_target)
