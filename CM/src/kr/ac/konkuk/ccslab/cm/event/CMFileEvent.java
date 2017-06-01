package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMFileEvent extends CMEvent{
	
	// events for the file transfer with the default channel
	public static final int REQUEST_FILE_TRANSFER = 1;		// c -> s
	public static final int REPLY_FILE_TRANSFER = 2;		// s -> c
	public static final int START_FILE_TRANSFER = 3;		// s -> c
	public static final int START_FILE_TRANSFER_ACK = 4;	// c -> s
	public static final int CONTINUE_FILE_TRANSFER = 5;		// s -> c
	public static final int CONTINUE_FILE_TRANSFER_ACK = 6;	// c -> s
	public static final int END_FILE_TRANSFER = 7;			// s -> c
	public static final int END_FILE_TRANSFER_ACK = 8;		// c -> s
	public static final int REQUEST_DIST_FILE_PROC = 9;		// c -> s (for distributed file processing)
	
	// events for the file transfer with the separate channel and thread
	public static final int REQUEST_FILE_TRANSFER_CHAN = 10;		// c -> s
	public static final int REPLY_FILE_TRANSFER_CHAN = 11;		// s -> c
	public static final int START_FILE_TRANSFER_CHAN = 12;		// s -> c
	public static final int START_FILE_TRANSFER_CHAN_ACK = 13;	// c -> s
	public static final int CONTINUE_FILE_TRANSFER_CHAN = 14;		// s -> c
	public static final int CONTINUE_FILE_TRANSFER_CHAN_ACK = 15;	// c -> s
	public static final int END_FILE_TRANSFER_CHAN = 16;			// s -> c
	public static final int END_FILE_TRANSFER_CHAN_ACK = 17;		// c -> s

	
	private String m_strUserName;	// target name
	private String m_strSenderName;	// sender name
	private String m_strFileName;
	private long m_lFileSize;
	private long m_lReceivedFileSize;
	private int m_nReturnCode;
	private byte[] m_cFileBlock;
	private int m_nBlockSize;
	private int m_nContentID;	// associated content ID (a file as an attachment of SNS content)
	private byte m_byteThroughputTestFlag;	// flag whether this file transfer is to measure throughput or not (0 or 1)
	
	public CMFileEvent()
	{
		m_nType = CMInfo.CM_FILE_EVENT;
		m_nID = -1;
		m_strUserName = "?";
		m_strSenderName = "?";
		m_strFileName = "?";
		m_lFileSize = 0;
		m_lReceivedFileSize = 0;
		m_nReturnCode = -1;
		m_nBlockSize = -1;
		m_cFileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		m_nContentID = -1;
		m_byteThroughputTestFlag = 0;
	}
	
	public CMFileEvent(ByteBuffer msg)
	{
		m_nType = CMInfo.CM_FILE_EVENT;
		m_nID = -1;
		m_strUserName = "?";
		m_strSenderName = "?";
		m_strFileName = "?";
		m_lFileSize = 0;
		m_lReceivedFileSize = 0;
		m_nReturnCode = -1;
		m_nBlockSize = -1;
		m_cFileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		m_nContentID = -1;
		m_byteThroughputTestFlag = 0;
		
		unmarshallHeader(msg);
		unmarshallBody(msg);
	}
	
	public CMFileEvent unmarshall(ByteBuffer msg)
	{
		unmarshallHeader(msg);
		unmarshallBody(msg);
		
		return this;
	}
	
	// set/get methods
	public void setUserName(String uName)
	{
		m_strUserName = uName;
	}
	public String getUserName()
	{
		return m_strUserName;
	}
	
	public void setSenderName(String sName)
	{
		m_strSenderName = sName;
	}
	
	public String getSenderName()
	{
		return m_strSenderName;
	}
	
	public void setFileName(String fName)
	{
		m_strFileName = fName;
	}
	
	public String getFileName()
	{
		return m_strFileName;
	}
	
	public void setFileSize(long fSize)
	{
		m_lFileSize = fSize;
	}
	
	public long getFileSize()
	{
		return m_lFileSize;
	}
	
	public void setReceivedFileSize(long fSize)
	{
		m_lReceivedFileSize = fSize;
	}
	
	public long getReceivedFileSize()
	{
		return m_lReceivedFileSize;
	}
	
	public void setReturnCode(int code)
	{
		m_nReturnCode = code;
	}
	
	public int getReturnCode()
	{
		return m_nReturnCode;
	}
	
	public void setFileBlock(byte[] fBlock)
	{
		System.arraycopy(fBlock, 0, m_cFileBlock, 0, CMInfo.FILE_BLOCK_LEN);
	}
	
	public byte[] getFileBlock()
	{
		return m_cFileBlock;
	}
	
	public void setBlockSize(int bSize)
	{
		m_nBlockSize = bSize;
	}
	
	public int getBlockSize()
	{
		return m_nBlockSize;
	}
	
	public void setContentID(int id)
	{
		m_nContentID = id;
	}
	
	public int getContentID()
	{
		return m_nContentID;
	}
	
	public void setThroughputTestFlag(byte flag)
	{
		m_byteThroughputTestFlag = flag;
	}
	
	public byte getThroughputTestFlag()
	{
		return m_byteThroughputTestFlag;
	}
	
	//////////////////////////////////////////////////////////
	
	protected int getByteNum()
	{		
		int nByteNum = 0;
		nByteNum = super.getByteNum();
		
		switch(m_nID)
		{
		case REQUEST_FILE_TRANSFER:
		case REQUEST_FILE_TRANSFER_CHAN:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strFileName.getBytes().length
					+ Byte.BYTES;
			break;
		case REPLY_FILE_TRANSFER:
		case REPLY_FILE_TRANSFER_CHAN:
			nByteNum += 3*Integer.BYTES + m_strFileName.getBytes().length;
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			nByteNum += 3*Integer.BYTES + m_strSenderName.getBytes().length + m_strFileName.getBytes().length
					+ Long.BYTES + Byte.BYTES;
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strFileName.getBytes().length 
					+ Long.BYTES;
			break;
		case CONTINUE_FILE_TRANSFER:
			nByteNum += 4*Integer.BYTES + m_strSenderName.getBytes().length + m_strFileName.getBytes().length 
					+ CMInfo.FILE_BLOCK_LEN;
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strFileName.getBytes().length 
					+ Long.BYTES;
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			nByteNum += 3*Integer.BYTES + m_strSenderName.getBytes().length + m_strFileName.getBytes().length 
					+ Long.BYTES;
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			nByteNum += 4*Integer.BYTES + m_strUserName.getBytes().length + m_strFileName.getBytes().length;
			break;
		case REQUEST_DIST_FILE_PROC:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length;
			break;
		default:
			nByteNum = -1;
			break;
		}
		
		return nByteNum;
	}
	
	protected void marshallBody()
	{
		switch(m_nID)
		{
		case REQUEST_FILE_TRANSFER:
		case REQUEST_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.put(m_byteThroughputTestFlag);
			m_bytes.clear();
			break;
		case REPLY_FILE_TRANSFER:
		case REPLY_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.put(m_byteThroughputTestFlag);
			m_bytes.clear();
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.putLong(m_lReceivedFileSize);
			m_bytes.clear();			
			break;
		case CONTINUE_FILE_TRANSFER:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.putInt(m_nBlockSize);
			m_bytes.put(m_cFileBlock);
			m_bytes.clear();
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lReceivedFileSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case REQUEST_DIST_FILE_PROC:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		default:
			System.out.println("CMFileEvent.marshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}		
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		switch(m_nID)
		{
		case REQUEST_FILE_TRANSFER:
		case REQUEST_FILE_TRANSFER_CHAN:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_byteThroughputTestFlag = msg.get();
			msg.clear();
			break;
		case REPLY_FILE_TRANSFER:
		case REPLY_FILE_TRANSFER_CHAN:
			m_strFileName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			m_byteThroughputTestFlag = msg.get();
			msg.clear();
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_lReceivedFileSize = msg.getLong();
			msg.clear();
			break;
		case CONTINUE_FILE_TRANSFER:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_nBlockSize = msg.getInt();
			msg.get(m_cFileBlock);
			msg.clear();
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lReceivedFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case REQUEST_DIST_FILE_PROC:
			m_strUserName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		default:
			System.out.println("CMFileEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}		
		
	}
}
