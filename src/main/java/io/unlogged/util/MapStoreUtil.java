package io.unlogged.util;

import io.unlogged.Constants;

public class MapStoreUtil {

	public static String getClassMapStore(String fullClassName) {
		String tempMapName = fullClassName.replace("/", "$") + "$" + Constants.mapStoreCompileValue;
		return tempMapName;
	}

	public static String getMethodCompoundName(String name, String desc) {
		String methodCompoundName = name + "$" + desc;
		return methodCompoundName;
	}
}
