// package io.unlogged;

// import java.util.logging.MemoryHandler;

// import org.junit.Test;
// import org.objectweb.asm.ClassReader;

// import io.unlogged.core.bytecode.*;
// import io.unlogged.logging.Logging;
// import io.unlogged.weaver.DataInfoProvider;
// import io.unlogged.weaver.TypeHierarchy;
// import io.unlogged.weaver.WeaveLog;

// public class FrequencyLoggingTest {
// 	@Test
//     void freqLogging() throws Exception {

// 		// define weavelog
// 		DataInfoProvider dataInfoProvider = new DataInfoProvider(0, 0, 0);
// 		WeaveLog weaveLog = new WeaveLog(0, dataInfoProvider);

// 		// define config
// 		RuntimeWeaverParameters runtimeWeaverParameters = new RuntimeWeaverParameters("a");
// 		WeaveConfig weaveConfig = new WeaveConfig(runtimeWeaverParameters);

// 		// define classReader
// 		String testClass = "selogger/testdata/SimpleTarget";
// 		ClassReader classReader = new ClassReader(testClass);

// 		// define type hierarcy
// 		TypeHierarchy typeHierarchy = new TypeHierarchy(null);

		
// 		ClassTransformer classTransformer = new ClassTransformer(weaveLog, weaveConfig, classReader, typeHierarchy);

// 		// Load the woven class 
// 		WeaveClassLoader loader = new WeaveClassLoader();
// 		Class<?> wovenClass = loader.createClass("selogger.testdata.SimpleTarget", classTransformer.getWeaveResult());
// 		MemoryHandler memoryLogger = Logging.initializeForTest();
		
// 		ClassReader r2 = new ClassReader("selogger/testdata/SimpleTarget$StringComparator");
// 		Class<?> innerClass = loader.createClass("selogger.testdata.SimpleTarget$StringComparator", r2.b) ;

// 		EventIterator it = new EventIterator(memoryLogger, weaveLog);
// 	}
// }
