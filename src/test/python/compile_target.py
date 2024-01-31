import os
import sys
import xml.etree.ElementTree as ET

def modify_pom(pom_path, sdk_version):

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

	if not dependency_present:
		# create dependency
		dependency = ET.Element("dependency")
		ET.SubElement(dependency, "groupId").text = "video.bug"
		ET.SubElement(dependency, "artifactId").text = "unlogged-sdk"
		ET.SubElement(dependency, "version").text = sdk_version

		# add dependency
		for child in root:
			if (child.tag == "{http://maven.apache.org/POM/4.0.0}dependencies"):
				child.append(dependency)

	# write file
	ET.indent(tree, space="\t", level=0)
	tree.write(pom_path, encoding="UTF-8", xml_declaration=True)

def modify_main(main_path):

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

			file.insert(line_count, unlogged_annotation)

	# write file
	with open(main_path, "w") as file_new:
		
		for line in file:
			file_new.write(line)
			file_new.write("\n")



if __name__=="__main__":

	# get constants
	main_method_identifier = "public static void main"
	unlogged_annotation = "@Unlogged"
	artifact_file = "src/test/python/test_report.txt"
	sdk_version = sys.argv[1]

	with open(artifact_file, "w") as file:
		file.writelines(["compile_test artifact file"])
		file.writelines(["sdk_version = " + sdk_version])

		test_repo_url = "https://github.com/kartikeytewari-ul/unlogged-spring-maven-demo-without-sdk"
		test_repo_name = "unlogged-spring-maven-demo-without-sdk"
		rel_pom_path = "/pom.xml"
		rel_main_path = "/src/main/java/org/unlogged/demo/UnloggedDemoApplication.java"
		

		os.system("git clone " + test_repo_url)
		modify_pom(test_repo_name + rel_pom_path, sdk_version)
		modify_main(test_repo_name + rel_main_path)
		os.system("cd " + test_repo_name + " && mvn clean compile")
		os.system("rm -rf " + test_repo_name)

		file.close()
