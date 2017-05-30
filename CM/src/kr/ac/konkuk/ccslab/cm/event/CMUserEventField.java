package kr.ac.konkuk.ccslab.cm.event;

public class CMUserEventField {
	public int nDataType;
	public String strFieldName;
	public String strFieldValue;	// for CM_INT, CM_LONG, CM_FLOAT, CM_DOUBLE, CM_STR
	public int nValueByteNum;		// for CM_BYTES
	public byte[] valueBytes;		// for CM_BYTES
}
