package io.unlogged.util;

import io.unlogged.Constants;

public class DistinctClassLogNameMap {

	public static String getClassMapStore(String fullClassName) {
		String tempMapName = fullClassName.replace("/", "$") + "$" + Constants.mapStoreCompileValue;
		return tempMapName;
	}

	public static String getMethodCompoundName(String className, String methodName, String desc) {
		String methodCompoundName = className.replace("/", ".") + "$" + methodName + "$" + desc;
		return methodCompoundName;
	}

	public static String getProbedMethodPrefix(String fullClassName) {
		String fullClassPart = fullClassName.replace("/", "$");
		String classPrefix = Constants.probedValue + fullClassPart + "$";
		return classPrefix;
	}
}
