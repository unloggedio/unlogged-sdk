package io.unlogged.logging.util;

import io.unlogged.logging.IErrorLogger;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is a stream specialized to write a sequence of events into files.
 * A triple of data ID, thread ID, and a value observed in the event is recorded.
 *
 * While a regular stream like FileOutputStream generates a single file,
 * this stream creates a number of files whose size is limited by the number of events
 * (MAX_EVENTS_PER_FILE field).
 */
public class EventDataStream {
	
	/**
	 * The number of events stored in a single file.
	 */
	public static final int MAX_EVENTS_PER_FILE = 10000000;
	
	/**
	 * The data size of an event.
	 */
	public static final int BYTES_PER_EVENT = 16;
	
	private FileNameGenerator files;
	private DataOutputStream out;
	private IErrorLogger err;
	private int count;

	/**
	 * This object records the number of threads observed by SELogger.
	 */
	private static final AtomicInteger nextThreadId = new AtomicInteger(0);
	
	/**
	 * Assign an integer to this thread. 
	 */
	private static ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return nextThreadId.getAndIncrement();
		}
	};
	
	/**
	 * Create an instance of stream.
	 * @param target is an object generating file names.
	 * @param logger is to report errors that occur in this class.
	 */
	public EventDataStream(FileNameGenerator target, IErrorLogger logger) {
		try {
			files = target;
			err = logger;
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(target.getNextFile()), 32 * 1024));
			count = 0;
		} catch (IOException e) {
			err.log(e);
		}
	}
	
	/**
	 * Write an event data into a file.  The thread ID is also recorded. 
	 * @param dataId specifies an event and its bytecode location.
	 * @param value specifies a data value observed in the event.
	 */
	public synchronized void write(int dataId, long value) {
//		System.out.printf("Record write [%s] - [%s]\n", dataId, value);
		if (out != null) {
			try {
				if (count >= MAX_EVENTS_PER_FILE) {
					out.close();
					out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(files.getNextFile()), 32 * 1024));
					count = 0;
				}
				out.writeInt(dataId);
				out.writeInt(threadId.get());
				out.writeLong(value);
				count++;
			} catch (IOException e) {
				out = null;
				err.log(e);
			}
		}
	}
	
	/**
	 * Close the stream.
	 */
	public synchronized void close() {
		try {
			out.close();
			out = null;
		} catch (IOException e) {
			out = null;
			err.log(e);
		}
	}

}
