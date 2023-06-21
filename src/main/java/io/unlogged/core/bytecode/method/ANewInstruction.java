package io.unlogged.core.bytecode.method;

/**
 * A class to represent a NEW instruction.
 */
public class ANewInstruction {

	private int dataId;
	private String typeName;

	/**
	 * Create an instance representing a NEW instruction
	 * @param dataId specifies the event location recording the NEW_OBJECT event
	 * @param typeName specifies the type name recorded by the instruction
	 */
	public ANewInstruction(int dataId, String typeName) {
		this.dataId = dataId;
		this.typeName = typeName;
	}
	
	/**
	 * @return the data ID.  This is used to link the NEW instruction and its constructor call
	 */
	public int getDataId() {
		return dataId;
	}
	
	/**
	 * @return the type name.  This is used just for checking the consistency of the bytecode.
	 */
	public String getTypeName() {
		return typeName;
	}

}
