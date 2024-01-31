import os
import sys

def modify_pom(pom_path, pom_xml_sdk_line, sdk_version):
	
	with open(pom_path, "r") as pom_file:
		data = pom_file.readlines()

	data[pom_xml_sdk_line-1] = "<version>" + sdk_version + "</version>"

	with open(pom_path, "w") as pom_file:
		pom_file.writelines(data)

if __name__=="__main__":

	# get constants
	artifact_file = "./test/test_report.txt"
	sdk_version = sys.argv[1]

	with open(artifact_file, "w") as file:
		file.writelines(["compile_test artifact file"])
		file.writelines(["sdk_version = " + sdk_version])

		test_repo_url = "https://github.com/unloggedio/unlogged-spring-maven-demo"
		test_repo_name = "unlogged-spring-maven-demo"
		pom_xml_sdk_line = 35
		pom_path = "/pom.xml"

		os.system ("git clone " + test_repo_url)
		modify_pom (test_repo_name + pom_path, pom_xml_sdk_line, sdk_version)
		os.system("cd " + test_repo_name + " && mvn clean compile")

		file.close()
