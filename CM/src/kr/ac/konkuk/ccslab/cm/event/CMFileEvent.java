package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that are used for the file-transfer task.
 * 
 * @author CCSLab, Konkuk University
 */
public class CMFileEvent extends CMEvent{
	
	/**
	 * The event ID for requesting a file.
	 * <p>event direction: receiver (requester) -&gt; sender (file owner)
	 * <p>This event is sent when the receiver calls 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#requestFile(String, String)} or 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#requestFile(String, String, byte)}, 
	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 0.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}
	 * <br>0: overwrite mode
	 * <br>1: append mode</li>
	 * </ul>
	 */
	public static final int REQUEST_PERMIT_PULL_FILE = 1;
	
	/**
	 * The event ID for the response to the file request.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event as the response to the 
	 * {@link CMFileEvent#REQUEST_FILE_TRANSFER} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>0: the request is denied.
	 * <br>1: the request is accepted.</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li> 
	 * </ul>
	 */
	public static final int REPLY_PERMIT_PULL_FILE = 2;
	
	public static final int REQUEST_PERMIT_PUSH_FILE = 3;
	public static final int REPLY_PERMIT_PUSH_FILE = 4;
	
	/**
	 * The event ID for notifying the receiver of the start of file-transfer.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event right after it sends the 
	 * {@link CMFileEvent#REPLY_FILE_TRANSFER} event. The owner also sends this event 
	 * when it calls the {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#pushFile(String, String)}, 
 	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 0.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}
	 * <br>0: overwrite mode, 1: append mode</li>
	 * </ul>
	 */
	public static final int START_FILE_TRANSFER = 5;
	
	/**
	 * The event ID for the response to the notification of the start of file-transfer.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The file receiver sends this event as the response to the 
	 * {@link CMFileEvent#START_FILE_TRANSFER} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>received file size: {@link CMFileEvent#getReceivedFileSize()}
	 * <br>&gt; 0: the receiver has already received some bytes of the file.
	 * <br>0: the receiver has no byte of the file.</li> 
	 * </ul>
	 */
	public static final int START_FILE_TRANSFER_ACK = 6;
	
	/**
	 * The event ID for transferring each file block.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner starts to send this event when it receives the 
	 * {@link CMFileEvent#START_FILE_TRANSFER_ACK} event. This event is repeatedly sent 
	 * for each file block until all file blocks are sent to the receiver.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>file block size: {@link CMFileEvent#getBlockSize()}</li>
	 * <li>file block: {@link CMFileEvent#getFileBlock()}</li>
	 * </ul>
	 */
	public static final int CONTINUE_FILE_TRANSFER = 7;
	
	public static final int CONTINUE_FILE_TRANSFER_ACK = 8;	// receiver -> sender (obsolete)
	
	/**
	 * The event ID for notifying the receiver of the end of file-transfer.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event after it sends the last 
	 * {@link CMFileEvent#CONTINUE_FILE_TRANSFER} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * </ul>
	 */
	public static final int END_FILE_TRANSFER = 9;
	
	/**
	 * The event ID for the response to the notification of the end of file-transfer.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The file receiver sends this event as the response to the 
	 * {@link CMFileEvent#END_FILE_TRANSFER} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: success
	 * <br>0: failure</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * </ul> 
	 */
	public static final int END_FILE_TRANSFER_ACK = 10;
	
	public static final int REQUEST_DIST_FILE_PROC = 11;		// c -> s (for distributed file processing)
	
	/**
	 * The event ID for the cancellation of pushing (or sending) a file.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file sender sends this event when it calls 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#cancelPushFile(String)}, 
	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 0.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * </ul>
	 */
	public static final int CANCEL_FILE_SEND = 12;
	
	/**
	 * The event ID for the response to the cancellation of sending a file.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The receiver sends this event as the response to the 
	 * {@link CMFileEvent#CANCEL_FILE_SEND} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: cancellation success
	 * <br>0: cancellation failure</li>
	 * </ul>
	 */
	public static final int CANCEL_FILE_SEND_ACK = 13;		// receiver -> sender
	
	// events for the file transfer with the separate channel and thread
	
	/**
	 * The event ID for requesting a file.
	 * <p>event direction: receiver (requester) -&gt; sender (file owner)
	 * <p>This event is sent when the receiver calls 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#requestFile(String, String)} or 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#requestFile(String, String, byte)}, 
	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 1.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}
	 * <br>0: overwrite mode
	 * <br>1: append mode</li>
	 * </ul>
	 */
	public static final int REQUEST_PERMIT_PULL_FILE_CHAN = 14;
	
	/**
	 * The event ID for the response to the file request.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event as the response to the 
	 * {@link CMFileEvent#REQUEST_FILE_TRANSFER_CHAN} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>0: the request is denied.
	 * <br>1: the request is accepted.</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li> 
	 * </ul>
	 */
	public static final int REPLY_PERMIT_PULL_FILE_CHAN = 15;
	
	
	/**
	 * The event ID for notifying the receiver of the start of file-transfer.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event right after it sends the 
	 * {@link CMFileEvent#REPLY_FILE_TRANSFER_CHAN} event. The owner also sends this event 
	 * when it calls the {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#pushFile(String, String)}, 
 	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 1.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}
	 * <br>0: overwrite mode, 1: append mode</li>
	 * </ul>
	 */
	public static final int START_FILE_TRANSFER_CHAN = 16;
	
	/**
	 * The event ID for the response to the notification of the start of file-transfer.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The file receiver sends this event as the response to the 
	 * {@link CMFileEvent#START_FILE_TRANSFER_CHAN} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>received file size: {@link CMFileEvent#getReceivedFileSize()}
	 * <br>&gt; 0: the receiver has already received some bytes of the file.
	 * <br>0: the receiver has no byte of the file.</li> 
	 * </ul>
	 */	
	public static final int START_FILE_TRANSFER_CHAN_ACK = 17;
	
	/**
	 * The event ID for notifying the receiver of the end of file-transfer.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event after it sends the last 
	 * file block.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * </ul>
	 */
	public static final int END_FILE_TRANSFER_CHAN = 18;
	
	/**
	 * The event ID for the response to the notification of the end of file-transfer.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The file receiver sends this event as the response to the 
	 * {@link CMFileEvent#END_FILE_TRANSFER_CHAN} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: success
	 * <br>0: failure</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * </ul> 
	 */
	public static final int END_FILE_TRANSFER_CHAN_ACK = 19;
	
	/**
	 * The event ID for the cancellation of pushing (or sending) a file.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file sender sends this event when it calls 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#cancelPushFile(String)}, 
	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 1.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * </ul>
	 */
	public static final int CANCEL_FILE_SEND_CHAN = 20;
	
	/**
	 * The event ID for the response to the cancellation of sending a file.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The receiver sends this event as the response to the 
	 * {@link CMFileEvent#CANCEL_FILE_SEND_CHAN} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: cancellation success
	 * <br>0: cancellation failure</li>
	 * </ul>
	 */
	public static final int CANCEL_FILE_SEND_CHAN_ACK = 21;
	
	/**
	 * The event ID for the cancellation of receiving a file.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The file receiver sends this event when it calls 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#cancelRequestFile(String)}, 
	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 1.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * </ul>
	 */
	public static final int CANCEL_FILE_RECV_CHAN = 22;
	
	/**
	 * The event ID for the response to the cancellation of receiving a file.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file sender sends this event as the response to the 
	 * {@link CMFileEvent#CANCEL_FILE_RECV_CHAN} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>sender name: {@link CMFileEvent#getSenderName()}</li>
	 * <li>receiver name: {@link CMFileEvent#getReceiverName()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: cancellation success
	 * <br>0: cancellation failure</li>
	 * </ul> 
	 */
	public static final int CANCEL_FILE_RECV_CHAN_ACK = 23;

	public static final int ERR_SEND_FILE_CHAN = 24;
	public static final int ERR_RECV_FILE_CHAN = 25;
	
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
		if(uName != null)
			m_strReceiverName = uName;
	}
	
	/**
	 * Returns the receiver name of a file.
	 * @return receiver name
	 */
	public String getReceiverName()
	{
		return m_strReceiverName;
	}
	
	public void setSenderName(String sName)
	{
		if(sName != null)
			m_strSenderName = sName;
	}
	
	/**
	 * Returns the sender name of a file.
	 * @return sender name
	 */
	public String getSenderName()
	{
		return m_strSenderName;
	}
	
	public void setFileName(String fName)
	{
		if(fName != null)
			m_strFileName = fName;
	}
	
	/**
	 * Returns the file name.
	 * @return file name
	 */
	public String getFileName()
	{
		return m_strFileName;
	}
	
	public void setFileSize(long fSize)
	{
		m_lFileSize = fSize;
	}
	
	/**
	 * Returns the file size.
	 * @return file size (number of bytes)
	 */
	public long getFileSize()
	{
		return m_lFileSize;
	}
	
	public void setReceivedFileSize(long fSize)
	{
		m_lReceivedFileSize = fSize;
	}
	
	/**
	 * Returns the size of a file that already has been received.
	 * @return received size of a file (number of bytes)
	 */
	public long getReceivedFileSize()
	{
		return m_lReceivedFileSize;
	}
	
	public void setReturnCode(int code)
	{
		m_nReturnCode = code;
	}
	
	/**
	 * Returns the return code.
	 * @return 1 if a corresponding request is successfully processed; 0 otherwise.
	 */
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
	
	/**
	 * Returns the file block.
	 * @return file block (bytes)
	 */
	public byte[] getFileBlock()
	{
		return m_cFileBlock;
	}
	
	public void setBlockSize(int bSize)
	{
		m_nBlockSize = bSize;
	}
	
	/**
	 * Returns the size of file block.
	 * @return size of file block (number of bytes)
	 */
	public int getBlockSize()
	{
		return m_nBlockSize;
	}
	
	public void setContentID(int id)
	{
		m_nContentID = id;
	}
	
	/**
	 * Returns the identifier of SNS content that attaches a file.
	 * @return SNS content ID
	 */
	public int getContentID()
	{
		return m_nContentID;
	}
	
	public void setFileAppendFlag(byte flag)
	{
		m_byteFileAppendFlag = flag;
	}
	
	/**
	 * Returns the file transfer mode.
	 * @return 1 if the append mode is on; 0 if overwrite mode is on.
	 */
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
		case REQUEST_PERMIT_PULL_FILE:
		case REQUEST_PERMIT_PULL_FILE_CHAN:
			// sender name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length;
			// receiver name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length;
			// file name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
			// content ID
			nByteNum += Integer.BYTES;
			// file-append flag
			nByteNum += Byte.BYTES;
			break;
		case REPLY_PERMIT_PULL_FILE:
		case REPLY_PERMIT_PULL_FILE_CHAN:
			// sender name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length;
			// receiver name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length;
			// file name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
			// content ID, return code
			nByteNum += 2*Integer.BYTES;
			break;
		case REQUEST_PERMIT_PUSH_FILE:
			// sender name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length;
			// receiver name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length;
			// file name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
			nByteNum += Long.BYTES;	// file size 
			nByteNum += Integer.BYTES;	// content ID
			break;
		case REPLY_PERMIT_PUSH_FILE:
			// sender name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length;
			// receiver name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length;
			// file name
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
			nByteNum += Long.BYTES;	// file size 
			nByteNum += Integer.BYTES;	// content ID
			nByteNum += Integer.BYTES;	// return code
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Long.BYTES + Integer.BYTES + Byte.BYTES;
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Integer.BYTES + Long.BYTES;
			break;
		case CONTINUE_FILE_TRANSFER:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += 2*Integer.BYTES + CMInfo.FILE_BLOCK_LEN;
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Long.BYTES + Integer.BYTES;
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Long.BYTES + Integer.BYTES;
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Long.BYTES + 2*Integer.BYTES;
			break;
		case REQUEST_DIST_FILE_PROC:
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length;
			nByteNum += Integer.BYTES;
			break;
		case CANCEL_FILE_SEND:
		case CANCEL_FILE_SEND_CHAN:
		case CANCEL_FILE_RECV_CHAN:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length
				+ m_strReceiverName.getBytes().length;
			break;
		case CANCEL_FILE_SEND_ACK:
		case CANCEL_FILE_SEND_CHAN_ACK:
		case CANCEL_FILE_RECV_CHAN_ACK:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length
				+ m_strReceiverName.getBytes().length;
			nByteNum += Integer.BYTES;
			break;
		case ERR_SEND_FILE_CHAN:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strReceiverName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Integer.BYTES;
			break;
		case ERR_RECV_FILE_CHAN:
			nByteNum += 2*CMInfo.STRING_LEN_BYTES_LEN + m_strSenderName.getBytes().length
				+ m_strFileName.getBytes().length;
			nByteNum += Integer.BYTES;
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
		case REQUEST_PERMIT_PULL_FILE:
		case REQUEST_PERMIT_PULL_FILE_CHAN:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putInt(m_nContentID);
			m_bytes.put(m_byteFileAppendFlag);
			break;
		case REPLY_PERMIT_PULL_FILE:
		case REPLY_PERMIT_PULL_FILE_CHAN:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			break;
		case REQUEST_PERMIT_PUSH_FILE:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			break;
		case REPLY_PERMIT_PUSH_FILE:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.putInt(m_nReturnCode);
			break;
		case START_FILE_TRANSFER:
		case START_FILE_TRANSFER_CHAN:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.put(m_byteFileAppendFlag);
			break;
		case START_FILE_TRANSFER_ACK:
		case START_FILE_TRANSFER_CHAN_ACK:
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putInt(m_nContentID);
			m_bytes.putLong(m_lReceivedFileSize);
			break;
		case CONTINUE_FILE_TRANSFER:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putInt(m_nContentID);
			m_bytes.putInt(m_nBlockSize);
			m_bytes.put(m_cFileBlock);
			break;
		case CONTINUE_FILE_TRANSFER_ACK:
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putLong(m_lReceivedFileSize);
			m_bytes.putInt(m_nContentID);
			break;
		case END_FILE_TRANSFER:
		case END_FILE_TRANSFER_CHAN:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nContentID);
			break;
		case END_FILE_TRANSFER_ACK:
		case END_FILE_TRANSFER_CHAN_ACK:
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putLong(m_lFileSize);
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			break;
		case REQUEST_DIST_FILE_PROC:
			putStringToByteBuffer(m_strReceiverName);
			m_bytes.putInt(m_nContentID);
			break;
		case CANCEL_FILE_SEND:
		case CANCEL_FILE_SEND_CHAN:
		case CANCEL_FILE_RECV_CHAN:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strReceiverName);
			break;
		case CANCEL_FILE_SEND_ACK:
		case CANCEL_FILE_SEND_CHAN_ACK:
		case CANCEL_FILE_RECV_CHAN_ACK:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strReceiverName);
			m_bytes.putInt(m_nReturnCode);
			break;
		case ERR_SEND_FILE_CHAN:
			putStringToByteBuffer(m_strReceiverName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putInt(m_nContentID);
			break;
		case ERR_RECV_FILE_CHAN:
			putStringToByteBuffer(m_strSenderName);
			putStringToByteBuffer(m_strFileName);
			m_bytes.putInt(m_nContentID);
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
		case REQUEST_PERMIT_PULL_FILE:
		case REQUEST_PERMIT_PULL_FILE_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_byteFileAppendFlag = msg.get();
			break;
		case REPLY_PERMIT_PULL_FILE:
		case REPLY_PERMIT_PULL_FILE_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			break;
		case REQUEST_PERMIT_PUSH_FILE:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			break;
		case REPLY_PERMIT_PUSH_FILE:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_lFileSize = msg.getLong();
			m_nContentID = msg.getInt();
			m_nReturnCode = msg.getInt();
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
		case ERR_SEND_FILE_CHAN:
			m_strReceiverName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			break;
		case ERR_RECV_FILE_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strFileName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			break;
		default:
			System.out.println("CMFileEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}		
		
	}
}
