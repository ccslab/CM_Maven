package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that are used for file-transfer tasks.
 * 
 * @author mlim
 * @see CMEvent
 */
public class CMFileEvent extends CMEvent{
	
	// events for the file transfer with the default channel
	public static final int REQUEST_FILE_TRANSFER = 1;		// receiver -> sender
	public static final int REPLY_FILE_TRANSFER = 2;		// sender -> receiver
	public static final int START_FILE_TRANSFER = 3;		// sender -> receiver
	public static final int START_FILE_TRANSFER_ACK = 4;	// receiver -> sender
	public static final int CONTINUE_FILE_TRANSFER = 5;		// sender -> receiver
	public static final int CONTINUE_FILE_TRANSFER_ACK = 6;	// receiver -> sender (obsolete)
	public static final int END_FILE_TRANSFER = 7;			// sender -> receiver
	public static final int END_FILE_TRANSFER_ACK = 8;		// receiver -> sender
	public static final int REQUEST_DIST_FILE_PROC = 9;		// c -> s (for distributed file processing)
	public static final int CANCEL_FILE_SEND = 10;			// sender -> receiver
	public static final int CANCEL_FILE_SEND_ACK = 11;		// receiver -> sender
	
	// events for the file transfer with the separate channel and thread
	public static final int REQUEST_FILE_TRANSFER_CHAN = 12;		// receiver -> sender
	public static final int REPLY_FILE_TRANSFER_CHAN = 13;		// sender -> receiver
	public static final int START_FILE_TRANSFER_CHAN = 14;		// sender -> receiver
	public static final int START_FILE_TRANSFER_CHAN_ACK = 15;	// receiver -> sender
	public static final int END_FILE_TRANSFER_CHAN = 16;		// sender -> receiver
	public static final int END_FILE_TRANSFER_CHAN_ACK = 17;	// receiver -> sender
	public static final int CANCEL_FILE_SEND_CHAN = 18;			// sender -> receiver
	public static final int CANCEL_FILE_SEND_CHAN_ACK = 19;		// receiver -> sender
	public static final int CANCEL_FILE_RECV_CHAN = 20;			// receiver -> sender
	public static final int CANCEL_FILE_RECV_CHAN_ACK = 21;		// sender -> receiver

	
	private String m_strReceiverName;	// receiver name
	private String m_strSenderName;	// sender name
	private String m_strFileName;
	private long m_lFileSize;
	private long m_lReceivedFileSize;
	private int m_nReturnCode;
	private byte[] m_cFileBlock;
	private int m_nBlockSize;
	private int m_nContentID;	// associated content ID (a file as an attachment of SNS content)
	private byte m_byteFileAppendFlag;	// flag of the file append mode (-1, 0 or 1)
	
	public CMFileEvent()
	{
		m_nType = CMInfo.CM_FILE_EVENT;
		m_nID = -1;
		m_strReceiverName = "?";
		m_strSenderName = "?";
		m_strFileName = "?";
		m_lFileSize = 0;
		m_lReceivedFileSize = 0;
		m_nReturnCode = -1;
		m_nBlockSize = -1;
		m_cFileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		m_nContentID = -1;
		m_byteFileAppendFlag = -1;
	}
	
	public CMFileEvent(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
	
	// set/get methods
	public void setReceiverName(String uName)
	{
		m_strReceiverName = uName;
	}
	public String getReceiverName()
	{
		return m_strReceiverName;
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
		if(fBlock == null)
			m_cFileBlock = null;
		else
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
	
	public void setFileAppendFlag(byte flag)
	{
		m_byteFileAppendFlag = flag;
	}
	
	public byte getFileAppendFlag()
	{
		return m_byteFileAppendFlag;
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
			nByteNum += 3*Integer.BYTES + m_strReceiverName.getBytes().length + m_strFileName.getBytes().length
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
			nByteNum += 3*Integer.BYTES + m_strReceiverName.getBytes().length + m_strFileName.getBytes().length 
					+ Long.BYTES;
			break;
		case CONTINUE_FILE_TRANSFER:
			nByteNum += 4*Integer.BYTES + m_strSenderName.getBytes().length + m_strFileName.getBytes().length 
					+ CMInfo.FILE_BLOCK_LEN;
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			nByteNum += 3*Integer.BYTES + m_strReceiverName.getBytes().length + m_strFileName.getBytes().length 
					+ Long.BYTES;
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			nByteNum += 3*Integer.BYTES + m_strSenderName.getBytes().length + m_strFileName.getBytes().length 
					+ Long.BYTES;
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			nByteNum += 4*Integer.BYTES + Long.BYTES + m_strReceiverName.getBytes().length + m_strFileName.getBytes().length;
			break;
		case REQUEST_DIST_FILE_PROC:
			nByteNum += 2*Integer.BYTES + m_strReceiverName.getBytes().length;
			break;
		case CANCEL_FILE_SEND:
		case CANCEL_FILE_SEND_CHAN:
		case CANCEL_FILE_RECV_CHAN:
			nByteNum += 2*Integer.BYTES + m_strSenderName.getBytes().length + m_strReceiverName.getBytes().length;
			break;
		case CANCEL_FILE_SEND_ACK:
		case CANCEL_FILE_SEND_CHAN_ACK:
		case CANCEL_FILE_RECV_CHAN_ACK:
			nByteNum += 3*Integer.BYTES + m_strSenderName.getBytes().length + m_strReceiverName.getBytes().length;
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
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.put(m_byteFileAppendFlag);
			break;
		case REPLY_FILE_TRANSFER:
		case REPLY_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.put(m_byteFileAppendFlag);
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.putLong(m_lReceivedFileSize);
			break;
		case CONTINUE_FILE_TRANSFER:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.putInt(m_nBlockSize);
			m_bytes.put(m_cFileBlock);
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lReceivedFileSize);
			m_bytes.putInt(m_nContentID);
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			m_bytes.putInt(m_strFileName.getBytes().length);
			m_bytes.put(m_strFileName.getBytes());
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			break;
		case REQUEST_DIST_FILE_PROC:
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			m_bytes.putInt(m_nContentID);
			break;
		case CANCEL_FILE_SEND:
		case CANCEL_FILE_SEND_CHAN:
		case CANCEL_FILE_RECV_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			break;
		case CANCEL_FILE_SEND_ACK:
		case CANCEL_FILE_SEND_CHAN_ACK:
		case CANCEL_FILE_RECV_CHAN_ACK:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strReceiverName.getBytes().length);
			m_bytes.put(m_strReceiverName.getBytes());
			m_bytes.putInt(m_nReturnCode);
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
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_byteFileAppendFlag = msg.get();
			break;
		case REPLY_FILE_TRANSFER:
		case REPLY_FILE_TRANSFER_CHAN:
			m_strFileName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			m_byteFileAppendFlag = msg.get();
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_lReceivedFileSize = msg.getLong();
			break;
		case CONTINUE_FILE_TRANSFER:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_nBlockSize = msg.getInt();
			msg.get(m_cFileBlock);
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lReceivedFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			break;
		case REQUEST_DIST_FILE_PROC:
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			break;
		case CANCEL_FILE_SEND:
		case CANCEL_FILE_SEND_CHAN:
		case CANCEL_FILE_RECV_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strReceiverName = getStringFromByteBuffer(msg);
			break;
		case CANCEL_FILE_SEND_ACK:
		case CANCEL_FILE_SEND_CHAN_ACK:
		case CANCEL_FILE_RECV_CHAN_ACK:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			break;			
		default:
			System.out.println("CMFileEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}		
		
	}
}
