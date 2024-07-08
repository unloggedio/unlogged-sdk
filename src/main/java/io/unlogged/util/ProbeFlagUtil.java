package io.unlogged.util;

import org.objectweb.asm.Opcodes;

public class ProbeFlagUtil {
	public static boolean getAlwaysProbeClassFlag (int access) {
		if ((access & Opcodes.ACC_INTERFACE) != 0) {
			// class is an interface
			return true;
		}
		else if ((access & Opcodes.ACC_ENUM) != 0) {
			// class is enum
			return true;
		}
		return false;
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

	public static Boolean getNeverProbeMethodFlag (String methodName, int access) {

		if (methodName.equals("equals")
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
				|| ((access & Opcodes.ACC_ABSTRACT) != 0)) {
			return true;
		}

		return false;
	}
}
