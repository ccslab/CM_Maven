package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;
import java.util.Arrays;
import java.util.UUID;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.util.CMUUIDConverter;

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
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#requestFile(String, String, byte)}.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}
	 * <br>0: overwrite mode
	 * <br>1: append mode</li>
	 * <li>port number of file receiver (client): {@link CMFileEvent#getSSCPort()}</li>
	 * </ul>
	 */
	public static final int REQUEST_PERMIT_PULL_FILE = 1;
	
	/**
	 * The event ID for the response to the file request.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event as the response to the 
	 * {@link CMFileEvent#REQUEST_PERMIT_PULL_FILE} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
	 * <li>file name: {@link CMFileEvent#getFileName()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>-1: the requested file does not exist.
	 * <br>0: the request is denied.
	 * <br>1: the request is accepted.</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * </ul>
	 */
	public static final int REPLY_PERMIT_PULL_FILE = 2;
	
	/**
	 * The event ID for requesting a permit to push a file.
	 * <p>event direction: sender (requester) -&gt; receiver (file owner)
	 * <p>This event is sent when the sender calls 
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#pushFile(String, String)}.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
	 * <li>file path: {@link CMFileEvent#getFilePath()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * </ul>
	 */
	public static final int REQUEST_PERMIT_PUSH_FILE = 3;
	
	/**
	 * The event ID for the response to the permit-request of pushing a file.
	 * <p>event direction: receiver -&gt; sender
	 * <p>The file receiver sends this event as the response to the 
	 * {@link CMFileEvent#REQUEST_PERMIT_PUSH_FILE} event.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
	 * <li>file path: {@link CMFileEvent#getFilePath()}</li>
	 * <li>file size: {@link CMFileEvent#getFileSize()}</li>
	 * <li>append mode: {@link CMFileEvent#getFileAppendFlag()}</li>
	 * <li>content ID: {@link CMFileEvent#getContentID()}
	 * <br>&gt;= 0: the requested file is an attachment of SNS content ID
	 * <br>-1: the file is no attachment of SNS content</li>
	 * <li>port number of file receiver (client): {@link CMFileEvent#getSSCPort()}</li> 
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>0: the request is denied.
	 * <br>1: the request is accepted.</li>
	 * </ul>
	 */
	public static final int REPLY_PERMIT_PUSH_FILE = 4;
	
	/**
	 * The event ID for notifying the receiver of the start of file-transfer.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event right after it sends the 
	 * {@link CMFileEvent#REPLY_PERMIT_PULL_FILE} event, or receives 
	 * the {@link CMFileEvent#REPLY_PERMIT_PUSH_FILE} event with the granted permit, 
 	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 0.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: cancellation success
	 * <br>0: cancellation failure</li>
	 * </ul>
	 */
	public static final int CANCEL_FILE_SEND_ACK = 13;		// receiver -> sender
	
	// events for the file transfer with the separate channel and thread
	
	/**
	 * The event ID for notifying the receiver of the start of file-transfer.
	 * <p>event direction: sender -&gt; receiver
	 * <p>The file owner sends this event right after it sends the 
	 * {@link CMFileEvent#REPLY_PERMIT_PULL_FILE} event, or receives 
	 * the {@link CMFileEvent#REPLY_PERMIT_PUSH_FILE} event with the granted permit 
 	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 1.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * file block, and if the FILE_TRANSFER_SCHEME field of the configuration file 
	 * of the CM server (cm-server.conf) is set to 1. 
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * {@link kr.ac.konkuk.ccslab.cm.stub.CMStub#cancelPullFile(String)}, 
	 * and if the FILE_TRANSFER_SCHEME field of the configuration file of the CM server 
	 * (cm-server.conf) is set to 1.
	 * <br>The following fields are used for this event:
	 * <ul>
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
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
	 * <li>file sender: {@link CMFileEvent#getFileSender()}</li>
	 * <li>file sender uuid: {@link CMFileEvent#getFileSenderUuid()}</li>
	 * <li>file receiver: {@link CMFileEvent#getFileReceiver()}</li>
	 * <li>file receiver uuid: {@link CMFileEvent#getFileReceiverUuid()}</li>
	 * <li>return code: {@link CMFileEvent#getReturnCode()}
	 * <br>1: cancellation success
	 * <br>0: cancellation failure</li>
	 * </ul> 
	 */
	public static final int CANCEL_FILE_RECV_CHAN_ACK = 23;

	public static final int ERR_SEND_FILE_CHAN = 24;
	public static final int ERR_RECV_FILE_CHAN = 25;
	
	private String m_strFileSender;	// file sender
	private UUID m_fileSenderUuid;	// file sender uuid
	private String m_strFileReceiver;	// file receiver
	private UUID m_fileReceiverUuid;	// file receiver uuid
	private String m_strFileName;	// file name
	private String m_strFilePath;	// file path
	private long m_lFileSize;
	private long m_lReceivedFileSize;
	private int m_nReturnCode;
	private byte[] m_cFileBlock;
	private int m_nBlockSize;
	private int m_nContentID;	// associated content ID (a file as an attachment of SNS content)
	private byte m_byteFileAppendFlag;	// flag of the file append mode (-1, 0 or 1)
	private int m_nSSCPort;	// port number of (client) file receiver
	
	public CMFileEvent()
	{
		m_nType = CMInfo.CM_FILE_EVENT;
		m_nID = -1;
		m_strFileReceiver = "?";
		m_fileReceiverUuid = null;
		m_strFileSender = "?";
		m_fileSenderUuid = null;
		m_strFileName = "?";
		m_strFilePath = "?";
		m_lFileSize = 0;
		m_lReceivedFileSize = 0;
		m_nReturnCode = -1;
		m_nBlockSize = -1;
		m_cFileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		m_nContentID = -1;
		m_byteFileAppendFlag = -1;
		m_nSSCPort = -1;
	}
	
	public CMFileEvent(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
	
	// set/get methods
	
	public void setFileReceiver(String uName)
	{
		if(uName != null)
			m_strFileReceiver = uName;
	}
	
	/**
	 * Returns the receiver name of a file.
	 * @return receiver name
	 */
	public String getFileReceiver()
	{
		return m_strFileReceiver;
	}

	public void setFileReceiverUuid(UUID uuid) {
		m_fileReceiverUuid = uuid;
	}

	/**
	 * Returns the receiver UUID of a file.
	 * @return receiver UUID
	 */
	public UUID getFileReceiverUuid() { return m_fileReceiverUuid; }
	
	public void setFileSender(String sName)
	{
		if(sName != null)
			m_strFileSender = sName;
	}
	
	/**
	 * Returns the sender name of a file.
	 * @return sender name
	 */
	public String getFileSender()
	{
		return m_strFileSender;
	}

	public void setFileSenderUuid(UUID uuid) {
		m_fileSenderUuid = uuid;
	}

	/**
	 * Returns the sender UUID of a file.
	 * @return sender UUID
	 */
	public UUID getFileSenderUuid() { return m_fileSenderUuid; }
	
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
	
	public void setFilePath(String fPath)
	{
		if(fPath != null)
			m_strFilePath = fPath;
	}
	
	/**
	 * Returns the file path.
	 * @return file path at the sender node
	 */
	public String getFilePath()
	{
		return m_strFilePath;
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
	
	public void setSSCPort(int nPort)
	{
		m_nSSCPort = nPort;
	}
	
	/**
	 * Returns the port number of (client) file receiver.
	 * <p> If the FILE_TRANSFER_SCHEME of the server CM configuration file (cm-server.conf) 
	 * is 1 and both the file sender and receiver are the client type (P2P file-transfer), 
	 * the file sender connects to the receiver by opening a blocking socket channel with 
	 * the IP address and this port number of the receiver. 
	 * @return port number of file receiver.
	 */
	public int getSSCPort()
	{
		return m_nSSCPort;
	}
	
	//////////////////////////////////////////////////////////
	@Override
	protected int getByteNum()
	{		
		int nByteNum = 0;
		nByteNum = super.getByteNum();
		
		switch(m_nID)
		{
			case REQUEST_PERMIT_PULL_FILE:
			case REPLY_PERMIT_PULL_FILE:
			case REQUEST_PERMIT_PUSH_FILE:
			case REPLY_PERMIT_PUSH_FILE:
			case START_FILE_TRANSFER:
			case START_FILE_TRANSFER_CHAN:
			case START_FILE_TRANSFER_ACK:
			case START_FILE_TRANSFER_CHAN_ACK:
			case CONTINUE_FILE_TRANSFER:
			case CONTINUE_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER:
			case END_FILE_TRANSFER_CHAN:
			case END_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER_CHAN_ACK:
			case REQUEST_DIST_FILE_PROC:
			case CANCEL_FILE_SEND:
			case CANCEL_FILE_SEND_CHAN:
			case CANCEL_FILE_RECV_CHAN:
			case CANCEL_FILE_SEND_ACK:
			case CANCEL_FILE_SEND_CHAN_ACK:
			case CANCEL_FILE_RECV_CHAN_ACK:
			case ERR_SEND_FILE_CHAN:
			case ERR_RECV_FILE_CHAN:
				// file sender
				nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileSender.getBytes().length;
				// fine sender uuid
				nByteNum += CMInfo.STRING_LEN_BYTES_LEN + CMUUIDConverter.uuidToString(m_fileSenderUuid)
						.getBytes().length;
				// file receiver
				nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileReceiver.getBytes().length;
				// file receiver uuid
				nByteNum += CMInfo.STRING_LEN_BYTES_LEN + CMUUIDConverter.uuidToString(m_fileReceiverUuid)
						.getBytes().length;

				if( m_nID == REQUEST_PERMIT_PULL_FILE ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// content ID
					nByteNum += Integer.BYTES;
					// append mode flag
					nByteNum += Byte.BYTES;
					// port number of server socket channel of file receiver in P2P file-transfer
					nByteNum += Integer.BYTES;
				}
				else if( m_nID == REPLY_PERMIT_PULL_FILE) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// content ID, return code
					nByteNum += 2*Integer.BYTES;
				}
				else if( m_nID == REQUEST_PERMIT_PUSH_FILE ) {
					// file path
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFilePath.getBytes().length;
					nByteNum += Long.BYTES;	// file size
					nByteNum += Byte.BYTES;	// append mode flag
					nByteNum += Integer.BYTES;	// content ID
				}
				else if( m_nID == REPLY_PERMIT_PUSH_FILE ) {
					// file path
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFilePath.getBytes().length;
					nByteNum += Long.BYTES;	// file size
					nByteNum += Byte.BYTES;	// append mode flag
					nByteNum += Integer.BYTES;	// content ID
					// port number of server socket channel of file receiver in P2P file-transfer
					nByteNum += Integer.BYTES;
					nByteNum += Integer.BYTES;	// return code
				}
				else if( m_nID == START_FILE_TRANSFER || m_nID == START_FILE_TRANSFER_CHAN ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// file size
					nByteNum += Long.BYTES;
					// content ID
					nByteNum += Integer.BYTES;
					// append mode
					nByteNum += Byte.BYTES;
				}
				else if( m_nID == START_FILE_TRANSFER_ACK || m_nID == START_FILE_TRANSFER_CHAN_ACK ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// content ID
					nByteNum += Integer.BYTES;
					// received file size
					nByteNum += Long.BYTES;
				}
				else if( m_nID == CONTINUE_FILE_TRANSFER ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// content ID
					nByteNum += Integer.BYTES;
					// file block size
					nByteNum += Integer.BYTES;
					// file block
					nByteNum += CMInfo.FILE_BLOCK_LEN;
				}
				else if( m_nID == CONTINUE_FILE_TRANSFER_ACK ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// received file size
					nByteNum += Long.BYTES;
					// content ID
					nByteNum += Integer.BYTES;
				}
				else if( m_nID == END_FILE_TRANSFER || m_nID == END_FILE_TRANSFER_CHAN ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// file size
					nByteNum += Long.BYTES;
					// content ID
					nByteNum += Integer.BYTES;
				}
				else if( m_nID == END_FILE_TRANSFER_ACK || m_nID == END_FILE_TRANSFER_CHAN_ACK ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// file size
					nByteNum += Long.BYTES;
					// return code
					nByteNum += Integer.BYTES;
					// content ID
					nByteNum += Integer.BYTES;
				}
				else if( m_nID == REQUEST_DIST_FILE_PROC ) {
					// content ID
					nByteNum += Integer.BYTES;
				}
				else if( m_nID == CANCEL_FILE_SEND || m_nID == CANCEL_FILE_SEND_CHAN
						|| m_nID == CANCEL_FILE_RECV_CHAN ) {
					// nothing more to add
				}
				else if( m_nID == CANCEL_FILE_SEND_ACK || m_nID == CANCEL_FILE_SEND_CHAN_ACK
						|| m_nID == CANCEL_FILE_RECV_CHAN_ACK ) {
					// return code
					nByteNum += Integer.BYTES;
				}
				else if( m_nID == ERR_SEND_FILE_CHAN || m_nID == ERR_RECV_FILE_CHAN ) {
					// file name
					nByteNum += CMInfo.STRING_LEN_BYTES_LEN + m_strFileName.getBytes().length;
					// content ID
					nByteNum += Integer.BYTES;
				}
				break;

			default:
				System.err.println("CMFileEvent.getByteNum(), unknown event id("+m_nID+")");
				nByteNum = -1;
				break;
		}
		
		return nByteNum;
	}

	@Override
	protected void marshallBody()
	{
		switch(m_nID)
		{
			case REQUEST_PERMIT_PULL_FILE:
			case REPLY_PERMIT_PULL_FILE:
			case REQUEST_PERMIT_PUSH_FILE:
			case REPLY_PERMIT_PUSH_FILE:
			case START_FILE_TRANSFER:
			case START_FILE_TRANSFER_CHAN:
			case START_FILE_TRANSFER_ACK:
			case START_FILE_TRANSFER_CHAN_ACK:
			case CONTINUE_FILE_TRANSFER:
			case CONTINUE_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER:
			case END_FILE_TRANSFER_CHAN:
			case END_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER_CHAN_ACK:
			case REQUEST_DIST_FILE_PROC:
			case CANCEL_FILE_SEND:
			case CANCEL_FILE_SEND_CHAN:
			case CANCEL_FILE_RECV_CHAN:
			case CANCEL_FILE_SEND_ACK:
			case CANCEL_FILE_SEND_CHAN_ACK:
			case CANCEL_FILE_RECV_CHAN_ACK:
			case ERR_SEND_FILE_CHAN:
			case ERR_RECV_FILE_CHAN:
				putStringToByteBuffer(m_strFileSender);
				putStringToByteBuffer(CMUUIDConverter.uuidToString(m_fileSenderUuid));
				putStringToByteBuffer(m_strFileReceiver);
				putStringToByteBuffer(CMUUIDConverter.uuidToString(m_fileReceiverUuid));

				if( m_nID == REQUEST_PERMIT_PULL_FILE ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putInt(m_nContentID);
					m_bytes.put(m_byteFileAppendFlag);
					m_bytes.putInt(m_nSSCPort);
				}
				else if( m_nID == REPLY_PERMIT_PULL_FILE ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putInt(m_nReturnCode);
					m_bytes.putInt(m_nContentID);
				}
				else if( m_nID == REQUEST_PERMIT_PUSH_FILE ) {
					putStringToByteBuffer(m_strFilePath);
					m_bytes.putLong(m_lFileSize);
					m_bytes.put(m_byteFileAppendFlag);
					m_bytes.putInt(m_nContentID);
				}
				else if( m_nID == REPLY_PERMIT_PUSH_FILE ) {
					putStringToByteBuffer(m_strFilePath);
					m_bytes.putLong(m_lFileSize);
					m_bytes.put(m_byteFileAppendFlag);
					m_bytes.putInt(m_nContentID);
					m_bytes.putInt(m_nSSCPort);
					m_bytes.putInt(m_nReturnCode);
				}
				else if( m_nID == START_FILE_TRANSFER || m_nID == START_FILE_TRANSFER_CHAN ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putLong(m_lFileSize);
					m_bytes.putInt(m_nContentID);
					m_bytes.put(m_byteFileAppendFlag);
				}
				else if( m_nID == START_FILE_TRANSFER_ACK || m_nID == START_FILE_TRANSFER_CHAN_ACK ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putInt(m_nContentID);
					m_bytes.putLong(m_lReceivedFileSize);
				}
				else if( m_nID == CONTINUE_FILE_TRANSFER ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putInt(m_nContentID);
					m_bytes.putInt(m_nBlockSize);
					m_bytes.put(m_cFileBlock);
				}
				else if( m_nID == CONTINUE_FILE_TRANSFER_ACK ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putLong(m_lReceivedFileSize);
					m_bytes.putInt(m_nContentID);
				}
				else if( m_nID == END_FILE_TRANSFER || m_nID == END_FILE_TRANSFER_CHAN ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putLong(m_lFileSize);
					m_bytes.putInt(m_nContentID);
				}
				else if( m_nID == END_FILE_TRANSFER_ACK || m_nID == END_FILE_TRANSFER_CHAN_ACK ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putLong(m_lFileSize);
					m_bytes.putInt(m_nReturnCode);
					m_bytes.putInt(m_nContentID);
				}
				else if( m_nID == REQUEST_DIST_FILE_PROC ) {
					m_bytes.putInt(m_nContentID);
				}
				else if( m_nID == CANCEL_FILE_SEND || m_nID == CANCEL_FILE_SEND_CHAN
						|| m_nID == CANCEL_FILE_RECV_CHAN ) {
					// nothing more to add
				}
				else if( m_nID == CANCEL_FILE_SEND_ACK || m_nID == CANCEL_FILE_SEND_CHAN_ACK
						|| m_nID == CANCEL_FILE_RECV_CHAN_ACK ) {
					m_bytes.putInt(m_nReturnCode);
				}
				else if( m_nID == ERR_SEND_FILE_CHAN || m_nID == ERR_RECV_FILE_CHAN ) {
					putStringToByteBuffer(m_strFileName);
					m_bytes.putInt(m_nContentID);
				}
				break;

			default:
				System.err.println("CMFileEvent.marshallBody(), unknown event id("+m_nID+").");
				m_bytes = null;
				break;
		}		
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		switch(m_nID)
		{
			case REQUEST_PERMIT_PULL_FILE:
			case REPLY_PERMIT_PULL_FILE:
			case REQUEST_PERMIT_PUSH_FILE:
			case REPLY_PERMIT_PUSH_FILE:
			case START_FILE_TRANSFER:
			case START_FILE_TRANSFER_CHAN:
			case START_FILE_TRANSFER_ACK:
			case START_FILE_TRANSFER_CHAN_ACK:
			case CONTINUE_FILE_TRANSFER:
			case CONTINUE_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER:
			case END_FILE_TRANSFER_CHAN:
			case END_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER_CHAN_ACK:
			case REQUEST_DIST_FILE_PROC:
			case CANCEL_FILE_SEND:
			case CANCEL_FILE_SEND_CHAN:
			case CANCEL_FILE_RECV_CHAN:
			case CANCEL_FILE_SEND_ACK:
			case CANCEL_FILE_SEND_CHAN_ACK:
			case CANCEL_FILE_RECV_CHAN_ACK:
			case ERR_SEND_FILE_CHAN:
			case ERR_RECV_FILE_CHAN:
				m_strFileSender = getStringFromByteBuffer(msg);
				m_fileSenderUuid = CMUUIDConverter.stringToUuid(getStringFromByteBuffer(msg));
				m_strFileReceiver = getStringFromByteBuffer(msg);
				m_fileReceiverUuid = CMUUIDConverter.stringToUuid(getStringFromByteBuffer(msg));

				if( m_nID == REQUEST_PERMIT_PULL_FILE ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_nContentID = msg.getInt();
					m_byteFileAppendFlag = msg.get();
					m_nSSCPort = msg.getInt();
				}
				else if( m_nID == REPLY_PERMIT_PULL_FILE ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_nReturnCode = msg.getInt();
					m_nContentID = msg.getInt();
				}
				else if( m_nID == REQUEST_PERMIT_PUSH_FILE ) {
					m_strFilePath = getStringFromByteBuffer(msg);
					m_lFileSize = msg.getLong();
					m_byteFileAppendFlag = msg.get();
					m_nContentID = msg.getInt();
				}
				else if( m_nID == REPLY_PERMIT_PUSH_FILE ) {
					m_strFilePath = getStringFromByteBuffer(msg);
					m_lFileSize = msg.getLong();
					m_byteFileAppendFlag = msg.get();
					m_nContentID = msg.getInt();
					m_nSSCPort = msg.getInt();
					m_nReturnCode = msg.getInt();
				}
				else if( m_nID == START_FILE_TRANSFER || m_nID == START_FILE_TRANSFER_CHAN ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_lFileSize = msg.getLong();
					m_nContentID = msg.getInt();
					m_byteFileAppendFlag = msg.get();
				}
				else if( m_nID == START_FILE_TRANSFER_ACK || m_nID == START_FILE_TRANSFER_CHAN_ACK ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_nContentID = msg.getInt();
					m_lReceivedFileSize = msg.getLong();
				}
				else if( m_nID == CONTINUE_FILE_TRANSFER ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_nContentID = msg.getInt();
					m_nBlockSize = msg.getInt();
					msg.get(m_cFileBlock);
				}
				else if( m_nID == CONTINUE_FILE_TRANSFER_ACK ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_lReceivedFileSize = msg.getLong();
					m_nContentID = msg.getInt();
				}
				else if( m_nID == END_FILE_TRANSFER || m_nID == END_FILE_TRANSFER_CHAN ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_lFileSize = msg.getLong();
					m_nContentID = msg.getInt();
				}
				else if( m_nID == END_FILE_TRANSFER_ACK || m_nID == END_FILE_TRANSFER_CHAN_ACK ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_lFileSize = msg.getLong();
					m_nReturnCode = msg.getInt();
					m_nContentID = msg.getInt();
				}
				else if( m_nID == REQUEST_DIST_FILE_PROC ) {
					m_nContentID = msg.getInt();
				}
				else if( m_nID == CANCEL_FILE_SEND || m_nID == CANCEL_FILE_SEND_CHAN
						|| m_nID == CANCEL_FILE_RECV_CHAN ) {
					// nothing more to add
				}
				else if( m_nID == CANCEL_FILE_SEND_ACK || m_nID == CANCEL_FILE_SEND_CHAN_ACK
						|| m_nID == CANCEL_FILE_RECV_CHAN_ACK ) {
					m_nReturnCode = msg.getInt();
				}
				else if( m_nID == ERR_SEND_FILE_CHAN || m_nID == ERR_RECV_FILE_CHAN ) {
					m_strFileName = getStringFromByteBuffer(msg);
					m_nContentID = msg.getInt();
				}
				break;

		default:
			System.err.println("CMFileEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}		
		
	}

	@Override
	public String toString() {
		switch (m_nID) {
			case REQUEST_PERMIT_PULL_FILE:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_nContentID=" + m_nContentID +
						", m_byteFileAppendFlag=" + m_byteFileAppendFlag +
						", m_nSSCPort=" + m_nSSCPort +
						'}';
			case REPLY_PERMIT_PULL_FILE:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_nReturnCode=" + m_nReturnCode +
						", m_nContentID=" + m_nContentID +
						'}';
			case REQUEST_PERMIT_PUSH_FILE:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFilePath='" + m_strFilePath + '\'' +
						", m_lFileSize=" + m_lFileSize +
						", m_nContentID=" + m_nContentID +
						", m_byteFileAppendFlag=" + m_byteFileAppendFlag +
						'}';
			case REPLY_PERMIT_PUSH_FILE:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFilePath='" + m_strFilePath + '\'' +
						", m_lFileSize=" + m_lFileSize +
						", m_nReturnCode=" + m_nReturnCode +
						", m_nContentID=" + m_nContentID +
						", m_byteFileAppendFlag=" + m_byteFileAppendFlag +
						", m_nSSCPort=" + m_nSSCPort +
						'}';
			case START_FILE_TRANSFER:
			case START_FILE_TRANSFER_CHAN:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_lReceivedFileSize=" + m_lReceivedFileSize +
						", m_nContentID=" + m_nContentID +
						'}';
			case CONTINUE_FILE_TRANSFER:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_cFileBlock=" + Arrays.toString(m_cFileBlock) +
						", m_nBlockSize=" + m_nBlockSize +
						", m_nContentID=" + m_nContentID +
						'}';
			case CONTINUE_FILE_TRANSFER_ACK:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_lReceivedFileSize=" + m_lReceivedFileSize +
						", m_nContentID=" + m_nContentID +
						'}';
			case END_FILE_TRANSFER:
			case END_FILE_TRANSFER_CHAN:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_lFileSize=" + m_lFileSize +
						", m_nContentID=" + m_nContentID +
						'}';
			case END_FILE_TRANSFER_ACK:
			case END_FILE_TRANSFER_CHAN_ACK:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_lFileSize=" + m_lFileSize +
						", m_nReturnCode=" + m_nReturnCode +
						", m_nContentID=" + m_nContentID +
						'}';
			case REQUEST_DIST_FILE_PROC:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_nContentID=" + m_nContentID +
						'}';
			case CANCEL_FILE_SEND:
			case CANCEL_FILE_SEND_CHAN:
			case CANCEL_FILE_RECV_CHAN:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						'}';
			case ERR_SEND_FILE_CHAN:
			case ERR_RECV_FILE_CHAN:
				return "CMFileEvent{" +
						"m_strSender='" + m_strSender + '\'' +
						", m_strReceiver='" + m_strReceiver + '\'' +
						", m_nID=" + m_nID +
						", m_nByteNum=" + m_nByteNum +
						", m_strFileReceiver='" + m_strFileReceiver + '\'' +
						", m_fileReceiverUuid= " + m_fileReceiverUuid +
						", m_strFileSender='" + m_strFileSender + '\'' +
						", m_fileSenderUuid= " + m_fileSenderUuid +
						", m_strFileName='" + m_strFileName + '\'' +
						", m_nContentID=" + m_nContentID +
						'}';
		}

		return super.toString();
	}

}
