package io.unlogged.util;

import org.objectweb.asm.Opcodes;

import io.unlogged.UnloggedMode;
import io.unlogged.core.processor.UnloggedProcessorConfig;

public class ProbeFlagUtil {

	public static boolean getAddHashMap(UnloggedProcessorConfig unloggedProcessorConfig, int access) {
		// TODO: remove this filter
		// decide if a Hashmap is to be added in static call of the class
		// always probe a default method in interface, because we cannot add a hashmap to the interface

		if (unloggedProcessorConfig.getUnloggedMode() == UnloggedMode.LogNothing) {
			return false;
		}
		else if (((access & Opcodes.ACC_INTERFACE) != 0)
			|| ((access & Opcodes.ACC_ENUM) != 0)){
			// do not add a hash map
			return false;
		}

		return true;
	}

	public static boolean getAlwaysProbeMethodFlag(String methodName, int access, String desc) {

		if (methodName.equals("<init>")) {
			// constructor method
			return true;
		}
		else if (methodName.equals("main") && (access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) && desc.equals("([Ljava/lang/String;)V")) {
			// initial method [public static void main ()]
			return true;
		}
		return false;
	}

	public static Boolean getNeverProbeMethodFlag (UnloggedProcessorConfig unloggedProcessorConfig, String methodName, int access) {

		if (unloggedProcessorConfig.getUnloggedMode() == UnloggedMode.LogNothing) {
			return true;
		}
		else if (methodName.equals("equals")
			|| methodName.equals("hashCode")
			|| methodName.equals("onNext")
			|| methodName.equals("onSubscribe")
			|| methodName.equals("onError")
			|| methodName.equals("currentContext")
			|| methodName.equals("onComplete")) {
			return true;
		}
		else if (((access & Opcodes.ACC_INTERFACE) != 0)
				|| ((access & Opcodes.ACC_ENUM) != 0)
				|| ((access & Opcodes.ACC_ABSTRACT) != 0)
				|| ((access & Opcodes.ACC_SYNTHETIC) != 0)) {
			return true;
		}

		return false;
	}
}
