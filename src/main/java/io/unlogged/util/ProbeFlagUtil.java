package io.unlogged.util;

import org.objectweb.asm.Opcodes;

public class ProbeFlagUtil {
	public static boolean getAlwaysProbeClassFlag (int access) {
		if ((access & Opcodes.ACC_INTERFACE) != 0) {
			// class is an interface
			return true;
		}
		if ((access & Opcodes.ACC_ENUM) != 0) {
			// class is enum
			return true;
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			// class is static
			return true;
		}
		return false;
	}

	public static boolean getalwaysProbeMethodFlag (String methodName, int access, String desc) {
		if ((access & Opcodes.ACC_INTERFACE) != 0) {
			// method is interface (remove it)
			return true;
		}
		if ((access & Opcodes.ACC_ENUM) != 0) {
			// method is enum (remove it)
			return true;
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			// method is static
			return true;
		}
		if (methodName.equals("<init>")) {
			// constructor method
			return true;
		}
		if (methodName.equals("main") && (access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) && desc.equals("([Ljava/lang/String;)V")) {
			// initial method [public static void main ()]
			return true;
		}
		return false;
	}

	public static Boolean getNeverProbeMethodFlag (String methodName) {
		if (methodName.equals("equals")) {
			return true;
		}
		if (methodName.equals("hashCode")) {
			return true;
		}
		return false;
	}
}
