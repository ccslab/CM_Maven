import java.util.ArrayList;
import java.util.Iterator;

import java.io.*;
import java.awt.*;

import kr.ac.konkuk.ccslab.cm.entity.CMServerInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSessionInfo;
import kr.ac.konkuk.ccslab.cm.event.CMDataEvent;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEventHandler;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEventField;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMSNSInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContent;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContentList;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;

public class CMWinClientEventHandler implements CMEventHandler{
	//private JTextArea m_outTextArea;
	private CMWinClient m_client;
	private CMClientStub m_clientStub;
	private long m_lDelaySum;	// for forwarding simulation
	private long m_lStartTime;	// for delay of SNS content downloading, distributed file processing, server response
	private int m_nEstDelaySum;	// for SNS downloading simulation
	private int m_nSimNum;		// for simulation of multiple sns content downloading
	private FileOutputStream m_fos;	// for storing downloading delay of multiple SNS content
	private PrintWriter m_pw;		//
	private int m_nCurrentServerNum;	// for distributed file processing
	private int m_nRecvPieceNum;		// for distributed file processing
	private boolean m_bDistFileProc;	// for distributed file processing
	private String m_strExt;			// for distributed file processing
	private String[] m_filePieces;		// for distributed file processing
	private boolean m_bReqAttachedFile;	// for storing the fact that the client requests an attachment
	private int m_nMinNumWaitedEvents;  // for checking the completion of asynchronous castrecv service
	private int m_nRecvReplyEvents;		// for checking the completion of asynchronous castrecv service
	
	public CMWinClientEventHandler(CMClientStub clientStub, CMWinClient client)
	{
		m_client = client;
		//m_outTextArea = textArea;
		m_clientStub = clientStub;
		m_lDelaySum = 0;
		m_lStartTime = 0;
		m_nEstDelaySum = 0;
		m_nSimNum = 0;
		m_fos = null;
		m_pw = null;
		m_nCurrentServerNum = 0;
		m_nRecvPieceNum = 0;
		m_bDistFileProc = false;
		m_strExt = null;
		m_filePieces = null;
		m_bReqAttachedFile = false;
		m_nMinNumWaitedEvents = 0;
		m_nRecvReplyEvents = 0;
	}
	
	//////////////////////////////////////////////////////////////////////////////
	// get/set methods
	
	public void setStartTime(long time)
	{
		m_lStartTime = time;
	}
	
	public long getStartTime()
	{
		return m_lStartTime;
	}
	
	public void setFileOutputStream(FileOutputStream fos)
	{
		m_fos = fos;
	}
	
	public FileOutputStream getFileOutputStream()
	{
		return m_fos;
	}
	
	public void setPrintWriter(PrintWriter pw)
	{
		m_pw = pw;
	}
	
	public PrintWriter getPrintWriter()
	{
		return m_pw;
	}
	
	public void setSimNum(int num)
	{
		m_nSimNum = num;
	}
	
	public int getSimNum()
	{
		return m_nSimNum;
	}
	
	public void setCurrentServerNum(int num)
	{
		m_nCurrentServerNum = num;
	}
	
	public int getCurrentServerNum()
	{
		return m_nCurrentServerNum;
	}
	
	public void setRecvPieceNum(int num)
	{
		m_nRecvPieceNum = num;
	}
	
	public int getRecvPieceNum()
	{
		return m_nRecvPieceNum;
	}
	
	public void setDistFileProc(boolean b)
	{
		m_bDistFileProc = b;
	}
	
	public boolean isDistFileProc()
	{
		return m_bDistFileProc;
	}
	
	public void setFileExtension(String ext)
	{
		m_strExt = ext;
	}
	
	public String getFileExtension()
	{
		return m_strExt;
	}
	
	public void setFilePieces(String[] pieces)
	{
		m_filePieces = pieces;
	}
	
	public String[] getFilePieces()
	{
		return m_filePieces;
	}
	
	public void setReqAttachedFile(boolean bReq)
	{
		m_bReqAttachedFile = bReq;
	}
	
	public boolean isReqAttachedFile()
	{
		return m_bReqAttachedFile;
	}

	public void setMinNumWaitedEvents(int num)
	{
		m_nMinNumWaitedEvents = num;
	}
	
	public int getMinNumWaitedEvents()
	{
		return m_nMinNumWaitedEvents;
	}
	
	public void setRecvReplyEvents(int num)
	{
		m_nRecvReplyEvents = num;
	}
	
	public int getRecvReplyEvents()
	{
		return m_nRecvReplyEvents;
	}
	//////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void processEvent(CMEvent cme) {
		switch(cme.getType())
		{
		case CMInfo.CM_SESSION_EVENT:
			processSessionEvent(cme);
			break;
		case CMInfo.CM_INTEREST_EVENT:
			processInterestEvent(cme);
			break;
		case CMInfo.CM_DATA_EVENT:
			processDataEvent(cme);
			break;
		case CMInfo.CM_DUMMY_EVENT:
			processDummyEvent(cme);
			break;
		case CMInfo.CM_USER_EVENT:
			processUserEvent(cme);
			break;
		case CMInfo.CM_FILE_EVENT:
			processFileEvent(cme);
			break;
		case CMInfo.CM_SNS_EVENT:
			processSNSEvent(cme);
			break;
		case CMInfo.CM_MULTI_SERVER_EVENT:
			processMultiServerEvent(cme);
			break;
		default:
			return;
		}	
	}
	
	private void processSessionEvent(CMEvent cme)
	{
		long lDelay = 0;
		CMSessionEvent se = (CMSessionEvent)cme;
		switch(se.getID())
		{
		case CMSessionEvent.LOGIN_ACK:
			lDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("LOGIN_ACK delay: "+lDelay+" ms.\n");
			if(se.isValidUser() == 0)
			{
				printMessage("This client fails authentication by the default server!\n");
			}
			else if(se.isValidUser() == -1)
			{
				printMessage("This client is already in the login-user list!\n");
			}
			else
			{
				printMessage("This client successfully logs in to the default server.\n");
				CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
				
				// Change the title of the client window
				m_client.setTitle("CM Client ["+interInfo.getMyself().getName()+"]");

				// Set the appearance of buttons in the client frame window
				m_client.setButtonsAccordingToClientState();
			}
			break;
		case CMSessionEvent.RESPONSE_SESSION_INFO:
			lDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("RESPONSE_SESSION_INFO delay: "+lDelay+" ms.\n");
			processRESPONSE_SESSION_INFO(se);
			break;
		case CMSessionEvent.SESSION_TALK:
			//System.out.println("("+se.getHandlerSession()+")");
			printMessage("("+se.getHandlerSession()+")\n");
			//System.out.println("<"+se.getUserName()+">: "+se.getTalk());
			printMessage("<"+se.getUserName()+">: "+se.getTalk()+"\n");
			break;
		case CMSessionEvent.JOIN_SESSION_ACK:
			lDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("JOIN_SESSION_ACK delay: "+lDelay+" ms.\n");
			m_client.setButtonsAccordingToClientState();
			break;
		case CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK:
			if(se.getReturnCode() == 0)
			{
				printMessage("Adding a nonblocking SocketChannel("+se.getChannelName()+","+se.getChannelNum()
						+") failed at the server!\n");
			}
			else
			{
				printMessage("Adding a nonblocking SocketChannel("+se.getChannelName()+","+se.getChannelNum()
						+") succeeded at the server!\n");
			}
			break;
		case CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK:
			lDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("ADD_BLOCK_SOCKET_CHANNEL_ACK delay: "+lDelay+" ms.\n");
			if(se.getReturnCode() == 0)
			{
				printMessage("Adding a blocking socket channel ("+se.getChannelName()+","+se.getChannelNum()
					+") failed at the server!\n");
			}
			else
			{
				printMessage("Adding a blocking socket channel("+se.getChannelName()+","+se.getChannelNum()
					+") succeeded at the server!\n");
			}
			break;
		case CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL_ACK:
			lDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("REMOVE_BLOCK_SOCKET_CHANNEL_ACK delay: "+lDelay+" ms.\n");
			if(se.getReturnCode() == 0)
			{
				printMessage("Removing a blocking socket channel ("+se.getChannelName()+","+se.getChannelNum()
					+") failed at the server!\n");
			}
			else
			{
				printMessage("Removing a blocking socket channel("+se.getChannelName()+","+se.getChannelNum()
					+") succeeded at the server!\n");
			}
			break;
		case CMSessionEvent.REGISTER_USER_ACK:
			if( se.getReturnCode() == 1 )
			{
				// user registration succeeded
				//System.out.println("User["+se.getUserName()+"] successfully registered at time["
				//			+se.getCreationTime()+"].");
				printMessage("User["+se.getUserName()+"] successfully registered at time["
							+se.getCreationTime()+"].\n");
			}
			else
			{
				// user registration failed
				//System.out.println("User["+se.getUserName()+"] failed to register!");
				printMessage("User["+se.getUserName()+"] failed to register!\n");
			}
			break;
		case CMSessionEvent.DEREGISTER_USER_ACK:
			if( se.getReturnCode() == 1 )
			{
				// user deregistration succeeded
				//System.out.println("User["+se.getUserName()+"] successfully deregistered.");
				printMessage("User["+se.getUserName()+"] successfully deregistered.\n");
			}
			else
			{
				// user registration failed
				//System.out.println("User["+se.getUserName()+"] failed to deregister!");
				printMessage("User["+se.getUserName()+"] failed to deregister!\n");
			}
			break;
		case CMSessionEvent.FIND_REGISTERED_USER_ACK:
			if( se.getReturnCode() == 1 )
			{
				//System.out.println("User profile search succeeded: user["+se.getUserName()
				//		+"], registration time["+se.getCreationTime()+"].");
				printMessage("User profile search succeeded: user["+se.getUserName()
						+"], registration time["+se.getCreationTime()+"].\n");
			}
			else
			{
				//System.out.println("User profile search failed: user["+se.getUserName()+"]!");
				printMessage("User profile search failed: user["+se.getUserName()+"]!\n");
			}
			break;
		case CMSessionEvent.UNEXPECTED_SERVER_DISCONNECTION:
			m_client.printStyledMessage("Unexpected disconnection from the default server!\n", "bold");
			m_client.setButtonsAccordingToClientState();
			m_client.setTitle("CM Client");
			break;
		default:
			return;
		}
	}
	
	private void processRESPONSE_SESSION_INFO(CMSessionEvent se)
	{
		Iterator<CMSessionInfo> iter = se.getSessionInfoList().iterator();

		printMessage(String.format("%-60s%n", "------------------------------------------------------------"));
		printMessage(String.format("%-20s%-20s%-10s%-10s%n", "name", "address", "port", "user num"));
		printMessage(String.format("%-60s%n", "------------------------------------------------------------"));

		while(iter.hasNext())
		{
			CMSessionInfo tInfo = iter.next();
			printMessage(String.format("%-20s%-20s%-10d%-10d%n", tInfo.getSessionName(), tInfo.getAddress(), 
					tInfo.getPort(), tInfo.getUserNum()));
		}
	}
	
	private void processInterestEvent(CMEvent cme)
	{
		CMInterestEvent ie = (CMInterestEvent) cme;
		switch(ie.getID())
		{
		case CMInterestEvent.USER_TALK:
			//System.out.println("("+ie.getHandlerSession()+", "+ie.getHandlerGroup()+")");
			printMessage("("+ie.getHandlerSession()+", "+ie.getHandlerGroup()+")\n");
			//System.out.println("<"+ie.getUserName()+">: "+ie.getTalk());
			printMessage("<"+ie.getUserName()+">: "+ie.getTalk()+"\n");
			break;
		default:
			return;
		}
	}
	
	private void processDataEvent(CMEvent cme)
	{
		CMDataEvent de = (CMDataEvent) cme;
		switch(de.getID())
		{
		case CMDataEvent.NEW_USER:
			//System.out.println("["+de.getUserName()+"] enters group("+de.getHandlerGroup()+") in session("
			//		+de.getHandlerSession()+").");
			printMessage("["+de.getUserName()+"] enters group("+de.getHandlerGroup()+") in session("
					+de.getHandlerSession()+").\n");
			break;
		case CMDataEvent.REMOVE_USER:
			//System.out.println("["+de.getUserName()+"] leaves group("+de.getHandlerGroup()+") in session("
			//		+de.getHandlerSession()+").");
			printMessage("["+de.getUserName()+"] leaves group("+de.getHandlerGroup()+") in session("
					+de.getHandlerSession()+").\n");
			break;
		default:
			return;
		}
	}
	
	private void processDummyEvent(CMEvent cme)
	{
		CMDummyEvent due = (CMDummyEvent) cme;
		//System.out.println("session("+due.getHandlerSession()+"), group("+due.getHandlerGroup()+")");
		printMessage("session("+due.getHandlerSession()+"), group("+due.getHandlerGroup()+")\n");
		//System.out.println("dummy msg: "+due.getDummyInfo());
		printMessage("dummy msg: "+due.getDummyInfo()+"\n");
		return;
	}
	
	private void processUserEvent(CMEvent cme)
	{
		int id = -1;
		long lSendTime = 0;
		int nSendNum = 0;
		
		CMUserEvent ue = (CMUserEvent) cme;

		if(ue.getStringID().equals("testForward"))
		{
			id = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "id"));
			//System.out.println("Received user event \'testForward\', id: "+id);
			printMessage("Received user event \'testForward\', id: "+id+"\n");
		}
		else if(ue.getStringID().equals("testNotForward"))
		{
			id = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "id"));
			//System.out.println("Received user event 'testNotForward', id("+id+")");
			printMessage("Received user event 'testNotForward', id("+id+")\n");
		}
		else if(ue.getStringID().equals("testForwardDelay"))
		{
			id = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "id"));
			lSendTime = Long.parseLong(ue.getEventField(CMInfo.CM_LONG, "stime"));
			long lDelay = System.currentTimeMillis() - lSendTime;
			m_lDelaySum += lDelay;
			//System.out.println("Received user event 'testNotForward', id("+id+"), delay("+lDelay+"), delay_sum("+m_lDelaySum+")");
			printMessage("Received user event 'testNotForward', id("+id+"), delay("+lDelay+"), delay_sum("+m_lDelaySum+")\n");
		}
		else if(ue.getStringID().equals("EndForwardDelay"))
		{
			nSendNum = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "sendnum"));
			//System.out.println("Received user envet 'EndForwardDelay', avg delay("+m_lDelaySum/nSendNum+" ms)");
			printMessage("Received user envet 'EndForwardDelay', avg delay("+m_lDelaySum/nSendNum+" ms)\n");
			m_lDelaySum = 0;
		}
		else if(ue.getStringID().equals("repRecv"))
		{
			String strReceiver = ue.getEventField(CMInfo.CM_STR, "receiver");
			int nBlockingChannelType = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "chType"));
			int nBlockingChannelKey = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "chKey"));
			int nRecvPort = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "recvPort"));
			int opt = -1;
			if(nBlockingChannelType == CMInfo.CM_SOCKET_CHANNEL)
				opt = CMInfo.CM_STREAM;
			else if(nBlockingChannelType == CMInfo.CM_DATAGRAM_CHANNEL)
				opt = CMInfo.CM_DATAGRAM;

			CMDummyEvent due = new CMDummyEvent();
			due.setDummyInfo("This is a test message to test a blocking channel");
			System.out.println("Sending a dummy event to ("+strReceiver+")..");
			
			if(opt == CMInfo.CM_STREAM)
				m_clientStub.send(due, strReceiver, opt, nBlockingChannelKey, true);
			else if(opt == CMInfo.CM_DATAGRAM)
				m_clientStub.send(due, strReceiver, opt, nBlockingChannelKey, nRecvPort, true);
			else
				System.err.println("invalid sending option!: "+opt);
		}
		else if(ue.getStringID().equals("testSendRecv"))
		{
			printMessage("Received user event from ["+ue.getSender()+"] to ["+ue.getReceiver()+
					"], (id, "+ue.getID()+"), (string id, "+ue.getStringID()+")\n");
			
			if(!m_clientStub.getMyself().getName().equals(ue.getReceiver()))
				return;
			
			CMUserEvent rue = new CMUserEvent();
			rue.setID(222);
			rue.setStringID("testReplySendRecv");
			boolean ret = m_clientStub.send(rue, ue.getSender());
			if(ret)
				printMessage("Sent reply event: (id, "+rue.getID()+"), (string id, "+rue.getStringID()+")\n");
			else
				printMessage("Failed to send the reply event!\n");			
		}
		else if(ue.getStringID().equals("testCastRecv"))
		{
			printMessage("Received user event from ["+ue.getSender()+"], to session["+
					ue.getEventField(CMInfo.CM_STR, "Target Session")+"] and group["+
					ue.getEventField(CMInfo.CM_STR,  "Target Group")+"], (id, "+ue.getID()+
					"), (string id, "+ue.getStringID()+")\n");
			CMUserEvent rue = new CMUserEvent();
			rue.setID(223);
			rue.setStringID("testReplyCastRecv");
			boolean ret = m_clientStub.send(rue, ue.getSender());
			if(ret)
				printMessage("Sent reply event: (id, "+rue.getID()+"), (sting id, "+rue.getStringID()+")\n");
			else
				printMessage("Failed to send the reply event!\n");
		}
		else if(ue.getStringID().equals("testReplySendRecv")) // for testing asynchronous sendrecv service
		{
			long lServerResponseDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("Asynchronously received reply event from ["+ue.getSender()+"]: (type, "+ue.getType()+
					"), (id, "+ue.getID()+"), (string id, "+ue.getStringID()+")\n");
			printMessage("Server response delay: "+lServerResponseDelay+"ms.\n");

		}
		else if(ue.getStringID().equals("testReplyCastRecv")) // for testing asynchronous castrecv service
		{
			//printMessage("Asynchronously received reply event from ["+ue.getSender()+"]: (type, "+ue.getType()+
			//		"), (id, "+ue.getID()+"), (string id, "+ue.getStringID()+")\n");
			m_nRecvReplyEvents++;
			
			if(m_nRecvReplyEvents == m_nMinNumWaitedEvents)
			{
				long lServerResponseDelay = System.currentTimeMillis() - m_lStartTime;
				printMessage("Complete to receive requested number of reply events.\n");
				printMessage("Number of received reply events: "+m_nRecvReplyEvents+"\n");
				printMessage("Server response delay: "+lServerResponseDelay+"ms.\n");
				m_nRecvReplyEvents = 0;
			}
			
		}
		else
		{
			printMessage("CMUserEvent received from ["+ue.getSender()+"], strID("+ue.getStringID()+")\n");
			printMessage(String.format("%-5s%-20s%-10s%-20s%n", "Type", "Field", "Length", "Value"));
			printMessage("-----------------------------------------------------\n");
			Iterator<CMUserEventField> iter = ue.getAllEventFields().iterator();
			while(iter.hasNext())
			{
				CMUserEventField uef = iter.next();
				if(uef.nDataType == CMInfo.CM_BYTES)
				{
					printMessage(String.format("%-5s%-20s%-10d", uef.nDataType, uef.strFieldName, 
										uef.nValueByteNum));
					for(int i = 0; i < uef.nValueByteNum; i++)
					{
						//not yet
					}
					printMessage("\n");
				}
				else
				{
					printMessage(String.format("%-5d%-20s%-10d%-20s%n", uef.nDataType, uef.strFieldName, 
							uef.strFieldValue.length(), uef.strFieldValue));
				}
			}
		}
		return;
	}
	
	private void processFileEvent(CMEvent cme)
	{
		CMFileEvent fe = (CMFileEvent) cme;
		long lDelay = 0;
		
		switch(fe.getID())
		{
		case CMFileEvent.REQUEST_FILE_TRANSFER:
		case CMFileEvent.REQUEST_FILE_TRANSFER_CHAN:
			//System.out.println("["+fe.getUserName()+"] requests file("+fe.getFileName()+").");
			printMessage("["+fe.getReceiverName()+"] requests file("+fe.getFileName()+").\n");
			break;
		case CMFileEvent.REPLY_FILE_TRANSFER:
		case CMFileEvent.REPLY_FILE_TRANSFER_CHAN:
			if(fe.getReturnCode() == 0)
			{
				//System.out.println("["+fe.getFileName()+"] does not exist in the owner!");
				printMessage("["+fe.getFileName()+"] does not exist in the owner!\n");
			}
			break;
		case CMFileEvent.START_FILE_TRANSFER:
		case CMFileEvent.START_FILE_TRANSFER_CHAN:
			//System.out.println("["+fe.getSenderName()+"] is about to send file("+fe.getFileName()+").");
			printMessage("["+fe.getSenderName()+"] is about to send file("+fe.getFileName()+").\n");
			break;
		case CMFileEvent.END_FILE_TRANSFER:
		case CMFileEvent.END_FILE_TRANSFER_CHAN:
			//System.out.println("["+fe.getSenderName()+"] completes to send file("+fe.getFileName()+", "
			//		+fe.getFileSize()+" Bytes).");
			printMessage("["+fe.getSenderName()+"] completes to send file("+fe.getFileName()+", "
					+fe.getFileSize()+" Bytes).\n");
			lDelay = System.currentTimeMillis() - m_lStartTime;
			printMessage("file-transfer delay: "+lDelay+" ms.\n");

			if(m_bDistFileProc)
				processFile(fe.getFileName());
			if(m_bReqAttachedFile)
			{
				CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
				String strPath = confInfo.getTransferedFileHome().toString() + File.separator + fe.getFileName();
				File file = new File(strPath);
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				m_bReqAttachedFile = false;
			}
			break;
		case CMFileEvent.CANCEL_FILE_SEND:
		case CMFileEvent.CANCEL_FILE_SEND_CHAN:
			printMessage("["+fe.getSenderName()+"] cancelled the file transfer.\n");
			break;
		case CMFileEvent.CANCEL_FILE_RECV_CHAN:
			printMessage("["+fe.getReceiverName()+"] cancelled the file request.\n");
			break;
		}
		return;
	}
	
	private void processFile(String strFile)
	{
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		String strMergeName = null;

		// add file name to list and increase index
		if(m_nCurrentServerNum == 1)
		{
			m_filePieces[m_nRecvPieceNum++] = confInfo.getTransferedFileHome().toString()+File.separator+strFile; 
		}
		else
		{
			// Be careful to put a file into an appropriate array member (file piece order)
			// extract piece number from file name ('filename'-'number'.split )
			int nStartIndex = strFile.lastIndexOf("-")+1;
			int nEndIndex = strFile.lastIndexOf(".");
			int nPieceIndex = Integer.parseInt(strFile.substring(nStartIndex, nEndIndex))-1;
			
			m_filePieces[nPieceIndex] = confInfo.getTransferedFileHome().toString()+File.separator+strFile;
			m_nRecvPieceNum++;
		}
		
		
		// if the index is the same as the number of servers, merge the split file
		if( m_nRecvPieceNum == m_nCurrentServerNum )
		{
			if(m_nRecvPieceNum > 1)
			{
				// set the merged file name m-'file name'.'ext'
				int index = strFile.lastIndexOf("-");
				strMergeName = confInfo.getTransferedFileHome().toString()+File.separator+
						strFile.substring(0, index)+"."+m_strExt;

				// merge split pieces
				CMFileTransferManager.mergeFiles(m_filePieces, m_nCurrentServerNum, strMergeName);
			}

			// calculate the total delay
			long lRecvTime = System.currentTimeMillis();
			//System.out.println("total delay for ("+m_nRecvPieceNum+") files: "
			//					+(lRecvTime-m_lStartTime)+" ms");
			printMessage("total delay for ("+m_nRecvPieceNum+") files: "
								+(lRecvTime-m_lStartTime)+" ms\n");

			// reset m_bDistSendRecv, m_nRecvFilePieceNum
			m_bDistFileProc = false;
			m_nRecvPieceNum = 0;
		}

		return;
	}
	
	private void processSNSEvent(CMEvent cme)
	{
		CMSNSInfo snsInfo = m_clientStub.getCMInfo().getSNSInfo();
		CMSNSContentList contentList = snsInfo.getSNSContentList();
		CMSNSEvent se = (CMSNSEvent) cme;
		int i = 0;
		
		switch(se.getID())
		{
		case CMSNSEvent.CONTENT_UPLOAD_RESPONSE:
			if( se.getReturnCode() == 1 )
			{
				//System.out.println("Content upload succeeded.");
				printMessage("Content upload succeeded.\n");
			}
			else
			{
				//System.out.println("Content upload failed.");
				printMessage("Content upload failed.\n");
			}
			//System.out.println("user("+se.getUserName()+"), seqNum("+se.getContentID()+"), time("
			//		+se.getDate()+").");
			printMessage("user("+se.getUserName()+"), seqNum("+se.getContentID()+"), time("
					+se.getDate()+").\n");
			break;
		case CMSNSEvent.CONTENT_DOWNLOAD_RESPONSE:
			contentList.removeAllSNSContents();	// clear the content list to which downloaded contents will be stored
			m_nEstDelaySum = 0;
			break;
		case CMSNSEvent.CONTENT_DOWNLOAD:
			ArrayList<String> fNameList = null;
			if(se.getNumAttachedFiles() > 0)
			{
				ArrayList<String> rcvList = se.getFileNameList();
				fNameList = new ArrayList<String>();
				for(i = 0; i < rcvList.size(); i++)
				{
					fNameList.add(rcvList.get(i));
				}
			}

			contentList.addSNSContent(se.getContentID(), se.getDate(), se.getWriterName(), se.getMessage(),
					se.getNumAttachedFiles(), se.getReplyOf(), se.getLevelOfDisclosure(), fNameList);
			//printMessage("transmitted delay: "+se.getEstDelay()+"\n");
			m_nEstDelaySum += se.getEstDelay();
			break;
		case CMSNSEvent.CONTENT_DOWNLOAD_END:
			processCONTENT_DOWNLOAD_END(se);
			break;
		case CMSNSEvent.RESPONSE_ATTACHED_FILE:
			if(se.getReturnCode() == 1)
			{
				printMessage("The request for an attached file ["+se.getFileName()
						+"] of content ID ["+se.getContentID()+"] written by ["+se.getWriterName()
						+"] is succeeded.\n");
			}
			else
			{
				printMessage("The request for an attached file ["+se.getFileName()
						+"] of content ID ["+se.getContentID()+"] written by ["+se.getWriterName()
						+"] is failed!\n");
				if(m_bReqAttachedFile)
					m_bReqAttachedFile = false;	// reset the flag
			}
			break;
		case CMSNSEvent.ADD_NEW_FRIEND_ACK:
			if(se.getReturnCode() == 1)
			{
				//System.out.println("["+se.getUserName()+"] succeeds to add a friend["
				//		+se.getFriendName()+"].");
				printMessage("["+se.getUserName()+"] succeeds to add a friend["
						+se.getFriendName()+"].\n");
			}
			else
			{
				//System.out.println("["+se.getUserName()+"] fails to add a friend["
				//		+se.getFriendName()+"].");
				printMessage("["+se.getUserName()+"] fails to add a friend["
						+se.getFriendName()+"].\n");
			}
			break;
		case CMSNSEvent.REMOVE_FRIEND_ACK:
			if(se.getReturnCode() == 1)
			{
				//System.out.println("["+se.getUserName()+"] succeeds to remove a friend["
				//		+se.getFriendName()+"].");
				printMessage("["+se.getUserName()+"] succeeds to remove a friend["
						+se.getFriendName()+"].\n");
			}
			else
			{
				//System.out.println("["+se.getUserName()+"] fails to remove a friend["
				//		+se.getFriendName()+"].");
				printMessage("["+se.getUserName()+"] fails to remove a friend["
						+se.getFriendName()+"].\n");
			}
			break;
		case CMSNSEvent.RESPONSE_FRIEND_LIST:
			//System.out.println("["+se.getUserName()+"] receives "+se.getNumFriends()+" friends "
			//		+"of total "+se.getTotalNumFriends()+" friends.");
			printMessage("["+se.getUserName()+"] receives "+se.getNumFriends()+" friends "
					+"of total "+se.getTotalNumFriends()+" friends.\n");
			//System.out.print("Friends: ");
			printMessage("Friends: ");
			for(i = 0; i < se.getFriendList().size(); i++)
			{
				//System.out.print(se.getFriendList().get(i)+" ");
				printMessage(se.getFriendList().get(i)+" ");
			}
			//System.out.println();
			printMessage("\n");
			break;
		case CMSNSEvent.RESPONSE_FRIEND_REQUESTER_LIST:
			//System.out.println("["+se.getUserName()+"] receives "+se.getNumFriends()+" requesters "
			//		+"of total "+se.getTotalNumFriends()+" requesters.");
			printMessage("["+se.getUserName()+"] receives "+se.getNumFriends()+" requesters "
					+"of total "+se.getTotalNumFriends()+" requesters.\n");
			//System.out.print("Requesters: ");
			printMessage("Requesters: ");
			for(i = 0; i < se.getFriendList().size(); i++)
			{
				//System.out.print(se.getFriendList().get(i)+" ");
				printMessage(se.getFriendList().get(i)+" ");
			}
			//System.out.println();
			printMessage("\n");
			break;
		case CMSNSEvent.RESPONSE_BI_FRIEND_LIST:
			//System.out.println("["+se.getUserName()+"] receives "+se.getNumFriends()+" bi-friends "
			//		+"of total "+se.getTotalNumFriends()+" bi-friends.");
			printMessage("["+se.getUserName()+"] receives "+se.getNumFriends()+" bi-friends "
					+"of total "+se.getTotalNumFriends()+" bi-friends.\n");
			//System.out.print("Bi-friends: ");
			printMessage("Bi-friends: ");
			for(i = 0; i < se.getFriendList().size(); i++)
			{
				//System.out.print(se.getFriendList().get(i)+" ");
				printMessage(se.getFriendList().get(i)+" ");
			}
			//System.out.println();
			printMessage("\n");
			break;
		case CMSNSEvent.CHANGE_ATTACH_DOWNLOAD_SCHEME:
			String[] attachScheme = {"Full", "Thumbnail", "Prefetching", "None"};
			printMessage("Server changes the scheme for attachment download of SNS content to ["
					+attachScheme[se.getAttachDownloadScheme()]+"].\n");
			break;
		case CMSNSEvent.PREFETCH_COMPLETED:
			processPREFETCH_COMPLETED(se);
			break;
		}
		return;
	}
	
	private void processCONTENT_DOWNLOAD_END(CMSNSEvent se)
	{
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		int nAttachScheme = confInfo.getAttachDownloadScheme();
		CMSNSInfo snsInfo = m_clientStub.getCMInfo().getSNSInfo();
		CMSNSContentList contentList = snsInfo.getSNSContentList();
		Iterator<CMSNSContent> iter = null;
		boolean bShowLink = true;
		
		//System.out.println("# downloaded contents: "+se.getNumContents());
		printMessage("# downloaded contents: "+se.getNumContents()+"\n");
		//System.out.println("# contents to be printed: "+contentList.getSNSContentNum());
		printMessage("# contents to be printed: "+contentList.getSNSContentNum()+"\n");
		
		// writes info to the file
		int nRealDelay = (int)(System.currentTimeMillis()-m_lStartTime);
		int nAccessDelay = nRealDelay + m_nEstDelaySum;
		printMessage("Real download delay: "+nRealDelay+" ms\n");
		if(m_pw != null)	// if multiple downloading is requested,
		{
			m_pw.format("%10d %10d%n", nAccessDelay, se.getNumContents());
			m_pw.flush();
		}

		// print out SNS content that is downloaded
		iter = contentList.getContentList().iterator();
		while(iter.hasNext())
		{
			CMSNSContent cont = iter.next();
			//System.out.println("--------------------------------------");
			printMessage("-----------------------------------------------------------\n");
			//System.out.println("ID("+cont.getContentID()+"), Date("+cont.getDate()+"), Writer("
			//		+cont.getWriterName()+"), File("+cont.getAttachedFileName()+")");
			printMessage("ID("+cont.getContentID()+"), Date("+cont.getDate()+"), Writer("
					+cont.getWriterName()+"), #attachment("+cont.getNumAttachedFiles()
					+"), replyID("+cont.getReplyOf()+"), lod("+cont.getLevelOfDisclosure()+")\n");
			//System.out.println("Message: "+cont.getMessage());
			printMessage("Message: "+cont.getMessage()+"\n");
			if(cont.getNumAttachedFiles() > 0)
			{
				ArrayList<String> fNameList = cont.getFileNameList();
				for(int i = 0; i < fNameList.size(); i++)
				{
					String fPath = confInfo.getTransferedFileHome().toString() + File.separator + fNameList.get(i);
					File file = new File(fPath);
					
					// display images (possibly thumbnails)
					if(nAttachScheme == CMInfo.SNS_ATTACH_FULL || nAttachScheme == CMInfo.SNS_ATTACH_PARTIAL)
					{
						if(CMUtil.isImageFile(fPath))
						{
							printImage(fPath);
						}
					}
					else if(nAttachScheme == CMInfo.SNS_ATTACH_PREFETCH)
					{
						// skip visualization of the prefetched original image
						if(CMUtil.isImageFile(fPath) && fNameList.get(i).contains("-thumbnail."))
						{
							printImage(fPath);
						}
					}
					
					// determine whether a link will be showed or not
					bShowLink = true;
					
					if(nAttachScheme == CMInfo.SNS_ATTACH_PARTIAL)
					{
						// if the file is a thumbnail file
						String fName = fNameList.get(i);
						if(fName.contains("-thumbnail."))
						{
							int index = fName.lastIndexOf("-thumbnail.");
							String ext = fName.substring(index+"-thumbnail".length(), fName.length());
							fName = fName.substring(0, index)+ext;
							fPath = confInfo.getTransferedFileHome().toString()+File.separator+fName;
							file = new File(fPath);
						}
					}
					else if(nAttachScheme == CMInfo.SNS_ATTACH_PREFETCH)
					{
						if(CMUtil.isImageFile(fPath))
						{
							String fName = fNameList.get(i);
							if(fName.contains("-thumbnail."))
							{
								int index = fName.lastIndexOf("-thumbnail.");
								String ext = fName.substring(index+"-thumbnail".length(), fName.length());
								fName = fName.substring(0, index)+ext;
								fPath = confInfo.getTransferedFileHome().toString()+File.separator+fName;
								file = new File(fPath);
							}
							else
								bShowLink = false;
						}
					}
					
					// print the link to a original attachment file
					if(bShowLink)
					{
						if(file.exists())
						{
							printMessage("attachment: ");
						}
						else
						{
							printMessage("attachment not downloaded: ");
						}
						printFilePath(fPath);						
					}
				}	// for
			}	// if
		}	// while
		//printMessage("sum of estimated download delay: "+m_nEstDelaySum +" ms\n");

		// continue simulation until m_nSimNum = 0
		if( --m_nSimNum > 0 )
		{
			// repeat the request of SNS content downloading
			m_lStartTime = System.currentTimeMillis();
			int nContentOffset = 0;
			String strUserName = m_clientStub.getMyself().getName();
			String strWriterName = "";
			
			m_clientStub.requestSNSContent(strWriterName, nContentOffset);
			if(CMInfo._CM_DEBUG)
			{
				//System.out.println("["+strUserName+"] requests content with offset("+nContentOffset+").");
				printMessage("["+strUserName+"] requests content of writer["+strWriterName+"] with offset("
				+nContentOffset+").\n");
			}
		}
		else
		{
			if(m_fos != null){
				try {
					m_fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(m_pw != null){
				m_pw.close();
			}
		}
		
		return;
	}
	
	private void processPREFETCH_COMPLETED(CMSNSEvent se)
	{
		String strUserName = se.getUserName();
		int nDelay = (int)(System.currentTimeMillis()-m_lStartTime);
		printMessage("["+strUserName+"] prefetching attachments completed, total delay: "+nDelay+" ms\n");
		
		return;
	}
	
	private void processMultiServerEvent(CMEvent cme)
	{
		CMMultiServerEvent mse = (CMMultiServerEvent) cme;
		switch(mse.getID())
		{
		case CMMultiServerEvent.NOTIFY_SERVER_INFO:
			//System.out.println("New server info received: num servers: "+mse.getServerNum() );
			printMessage("New server info received: num servers: "+mse.getServerNum()+"\n");
			Iterator<CMServerInfo> iter = mse.getServerInfoList().iterator();
			//System.out.format("%-20s %-20s %-10s %-10s%n", "name", "addr", "port", "udp port");
			printMessage(String.format("%-20s %-20s %-10s %-10s%n", "name", "addr", "port", "udp port"));
			//System.out.println("--------------------------------------------------------------");
			printMessage("--------------------------------------------------------------\n");
			while(iter.hasNext())
			{
				CMServerInfo si = iter.next();
				//System.out.format("%-20s %-20s %-10d %-10d%n", si.getServerName(), 
				//		si.getServerAddress(), si.getServerPort(), si.getServerUDPPort());
				printMessage(String.format("%-20s %-20s %-10d %-10d%n", si.getServerName(), 
						si.getServerAddress(), si.getServerPort(), si.getServerUDPPort()));
			}
			break;
		case CMMultiServerEvent.NOTIFY_SERVER_LEAVE:
			//System.out.println("An additional server["+mse.getServerName()+"] left the "
			//		+ "default server.");
			printMessage("An additional server["+mse.getServerName()+"] left the "
					+ "default server.\n");
			break;
		case CMMultiServerEvent.ADD_RESPONSE_SESSION_INFO:
			//System.out.println("Session information of server["+mse.getServerName()+"]");
			printMessage("Session information of server["+mse.getServerName()+"]\n");
			//System.out.format("%-60s%n", "------------------------------------------------------------");
			printMessage(String.format("%-60s%n", "------------------------------------------------------------"));
			//System.out.format("%-20s%-20s%-10s%-10s%n", "name", "address", "port", "user num");
			printMessage(String.format("%-20s%-20s%-10s%-10s%n", "name", "address", "port", "user num"));
			//System.out.format("%-60s%n", "------------------------------------------------------------");
			printMessage(String.format("%-60s%n", "------------------------------------------------------------"));
			Iterator<CMSessionInfo> iterSI = mse.getSessionInfoList().iterator();

			while(iterSI.hasNext())
			{
				CMSessionInfo tInfo = iterSI.next();
				//System.out.format("%-20s%-20s%-10d%-10d%n", tInfo.getSessionName(), tInfo.getAddress(), 
				//		tInfo.getPort(), tInfo.getUserNum());
				printMessage(String.format("%-20s%-20s%-10d%-10d%n", tInfo.getSessionName(), tInfo.getAddress(), 
						tInfo.getPort(), tInfo.getUserNum()));
			}
			break;
		case CMMultiServerEvent.ADD_LOGIN_ACK:
			//System.out.println("This client successfully logs in to server["+mse.getServerName()+"].");
			printMessage("This client successfully logs in to server["+mse.getServerName()+"].\n");
			break;
		}
		
		return;
	}

	private void printMessage(String strText)
	{
		/*
		m_outTextArea.append(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
		*/
		/*
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, null);
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		m_client.printMessage(strText);
		
		return;
	}
	
	/*
	private void printStyledMessage(String strText, String strStyleName)
	{
		m_client.printStyledMessage(strText, strStyleName);
	}
	*/
	
	private void printImage(String strPath)
	{
		m_client.printImage(strPath);
	}
	
	private void printFilePath(String strPath)
	{
		m_client.printFilePath(strPath);
	}
	
	/*
	private void setMessage(String strText)
	{
		m_outTextArea.setText(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
	}
	*/

}
