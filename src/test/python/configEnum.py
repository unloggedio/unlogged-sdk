from enum import Enum

class TestResult(Enum):
	PASS = 1
	FAIL = 2

class buildSystem(Enum):
	MAVEN = 1
	GRADLE = 2

class ReportType(Enum):
	COMPILE = 1
	REPLAY = 2