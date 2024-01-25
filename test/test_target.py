import sys
import pyfiglet 

def print_banner(val):
	print(pyfiglet.figlet_format(val) )

with open("./test/test_report.txt", "w") as file:
	file.writelines(["Line-1 ", "Line-2"])
	for arg in sys.argv:
		file.writelines(["arg = " + arg])
	file.close()
