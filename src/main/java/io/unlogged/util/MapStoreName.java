package io.unlogged.util;

import io.unlogged.Constants;

public class MapStoreName {

	public static String getClassMapStore(String fullClassName) {
		String tempMapName = fullClassName.replace("/", "$") + "$" + Constants.mapStoreCompileValue;
		return tempMapName;
	}
}
