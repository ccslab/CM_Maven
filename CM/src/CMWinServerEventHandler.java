import java.util.Iterator;
import java.io.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEventField;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventDISCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBCOMP;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREC;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREL;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventSUBSCRIBE;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventUNSUBSCRIBE;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMDBManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

public class CMWinServerEventHandler implements CMAppEventHandler {
	private CMWinServer m_server;
	private CMServerStub m_serverStub;
	private int m_nCheckCount;	// for internal forwarding simulation
	private boolean m_bDistFileProc;	// for distributed file processing

	public CMWinServerEventHandler(CMServerStub serverStub, CMWinServer server)
	{
		m_server = server;
		m_serverStub = serverStub;
		m_nCheckCount = 0;
		m_bDistFileProc = false;
	}
	
	@Override
	public void processEvent(CMEvent cme) {
		// TODO Auto-generated method stub
		switch(cme.getType())
		{
		case CMInfo.CM_SESSION_EVENT:
			processSessionEvent(cme);
			break;
		case CMInfo.CM_INTEREST_EVENT:
			processInterestEvent(cme);
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
		case CMInfo.CM_MQTT_EVENT:
			processMqttEvent(cme);
			break;
		default:
			return;
		}
	}
	
	private void processSessionEvent(CMEvent cme)
	{
		CMConfigurationInfo confInfo = m_serverStub.getCMInfo().getConfigurationInfo();
		CMSessionEvent se = (CMSessionEvent) cme;
		switch(se.getID())
		{
		case CMSessionEvent.LOGIN:
			//System.out.println("["+se.getUserName()+"] requests login.");
			printMessage("["+se.getUserName()+"] requests login.\n");
			if(confInfo.isLoginScheme())
			{
				// user authentication...
				// CM DB must be used in the following authentication..
				boolean ret = CMDBManager.authenticateUser(se.getUserName(), se.getPassword(), 
						m_serverStub.getCMInfo());
				if(!ret)
				{
					printMessage("["+se.getUserName()+"] authentication fails!\n");
					m_serverStub.replyEvent(cme, 0);
				}
				else
				{
					printMessage("["+se.getUserName()+"] authentication succeeded.\n");
					m_serverStub.replyEvent(cme, 1);
				}
			}
			break;
		case CMSessionEvent.LOGOUT:
			//System.out.println("["+se.getUserName()+"] logs out.");
			printMessage("["+se.getUserName()+"] logs out.\n");
			break;
		case CMSessionEvent.REQUEST_SESSION_INFO:
			//System.out.println("["+se.getUserName()+"] requests session information.");
			printMessage("["+se.getUserName()+"] requests session information.\n");
			break;
		case CMSessionEvent.CHANGE_SESSION:
			//System.out.println("["+se.getUserName()+"] changes to session("+se.getSessionName()+").");
			printMessage("["+se.getUserName()+"] changes to session("+se.getSessionName()+").\n");
			break;
		case CMSessionEvent.JOIN_SESSION:
			//System.out.println("["+se.getUserName()+"] requests to join session("+se.getSessionName()+").");
			printMessage("["+se.getUserName()+"] requests to join session("+se.getSessionName()+").\n");
			break;
		case CMSessionEvent.LEAVE_SESSION:
			//System.out.println("["+se.getUserName()+"] leaves a session("+se.getSessionName()+").");
			printMessage("["+se.getUserName()+"] leaves a session("+se.getSessionName()+").\n");
			break;
		case CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL:
			//System.out.println("["+se.getChannelName()+"] request to add SocketChannel with index("
			//		+se.getChannelNum()+").");
			printMessage("["+se.getChannelName()+"] request to add a nonblocking SocketChannel with key("
			+se.getChannelNum()+").\n");
			break;
		case CMSessionEvent.REGISTER_USER:
			//System.out.println("User registration requested by user["+se.getUserName()+"].");
			printMessage("User registration requested by user["+se.getUserName()+"].\n");
			break;
		case CMSessionEvent.DEREGISTER_USER:
			//System.out.println("User deregistration requested by user["+se.getUserName()+"].");
			printMessage("User deregistration requested by user["+se.getUserName()+"].\n");
			break;
		case CMSessionEvent.FIND_REGISTERED_USER:
			//System.out.println("User profile requested for user["+se.getUserName()+"].");
			printMessage("User profile requested for user["+se.getUserName()+"].\n");
			break;
		case CMSessionEvent.UNEXPECTED_SERVER_DISCONNECTION:
			m_server.printStyledMessage("Unexpected disconnection from ["
					+se.getChannelName()+"] with key["+se.getChannelNum()+"]!\n", "bold");
			break;
		case CMSessionEvent.INTENTIONALLY_DISCONNECT:
			m_server.printStyledMessage("Intentionally disconnected all channels from ["
					+se.getChannelName()+"]!\n", "bold");
			break;
		default:
			return;
		}
	}
	
	private void processInterestEvent(CMEvent cme)
	{
		CMInterestEvent ie = (CMInterestEvent) cme;
		switch(ie.getID())
		{
		case CMInterestEvent.USER_ENTER:
			//System.out.println("["+ie.getUserName()+"] enters group("+ie.getCurrentGroup()+") in session("
			//		+ie.getHandlerSession()+").");
			printMessage("["+ie.getUserName()+"] enters group("+ie.getCurrentGroup()+") in session("
					+ie.getHandlerSession()+").\n");
			break;
		case CMInterestEvent.USER_LEAVE:
			//System.out.println("["+ie.getUserName()+"] leaves group("+ie.getHandlerGroup()+") in session("
			//		+ie.getHandlerSession()+").");
			printMessage("["+ie.getUserName()+"] leaves group("+ie.getHandlerGroup()+") in session("
					+ie.getHandlerSession()+").\n");
			break;
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
		int nForwardType = -1;
		int id = -1;
		String strUser = null;
		
		CMUserEvent ue = (CMUserEvent) cme;
		
		if(ue.getStringID().equals("testNotForward"))
		{
			m_nCheckCount++;
			id = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "id"));
			//System.out.println("Received user event 'testNotForward', id("+id+"), checkCount("+m_nCheckCount+")");
			printMessage("Received user event 'testNotForward', id("+id+"), checkCount("+m_nCheckCount+")\n");
		}
		else if(ue.getStringID().equals("testForward"))
		{
			nForwardType = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "ftype"));
			if(nForwardType == 0)	// typical forwarding
			{
				m_nCheckCount++;
				id = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "id"));
				//System.out.println("Received user evnet 'testForward', id("+id+"), checkCount("+m_nCheckCount+")");
				printMessage("Received user evnet 'testForward', id("+id+"), checkCount("+m_nCheckCount+")\n");
				strUser = ue.getEventField(CMInfo.CM_STR, "user");
				m_serverStub.send(cme, strUser);
			}
		}
		else if(ue.getStringID().equals("EndSim"))
		{
			int nSimNum = 0;
			nSimNum = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "simnum"));
			//System.out.println("Received user event 'EndSim', simulation num("+nSimNum+")");
			printMessage("Received user event 'EndSim', simulation num("+nSimNum+")\n");
			if(nSimNum == 0)
			{
				//System.out.println("divided by 0 error.");
				printMessage("divided by 0 error.\n");
				return;
			}
			int nAvgCount = m_nCheckCount / nSimNum;
			//System.out.println("Total count("+m_nCheckCount+"), average count("+nAvgCount+").");
			printMessage("Total count("+m_nCheckCount+"), average count("+nAvgCount+").\n");
			m_nCheckCount = 0;
		}
		else if(ue.getStringID().equals("testForwardDelay"))
		{
			nForwardType = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "ftype"));
			if(nForwardType == 0)
			{
				id = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "id"));
				//System.out.println("Received user event 'testForwardDelay', id("+id+")");
				printMessage("Received user event 'testForwardDelay', id("+id+")\n");
				strUser = ue.getEventField(CMInfo.CM_STR, "user");
				m_serverStub.send(cme, strUser);
			}
		}
		else if(ue.getStringID().equals("EndForwardDelay"))
		{
			nForwardType = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "ftype"));
			if(nForwardType == 0)
			{
				//System.out.println("Received user event 'EndForwardDelay'");
				printMessage("Received user event 'EndForwardDelay'\n");
				strUser = ue.getEventField(CMInfo.CM_STR, "user");
				m_serverStub.send(cme, strUser);
			}
			
		}
		else if(ue.getStringID().equals("reqRecv"))
		{
			strUser = ue.getEventField(CMInfo.CM_STR, "user");
			int nChType = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "chType"));
			int nChKey = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "chKey"));
			int nRecvPort = Integer.parseInt(ue.getEventField(CMInfo.CM_INT, "recvPort"));
			CMUserEvent userEvent = new CMUserEvent();
			userEvent.setStringID("repRecv");
			userEvent.setEventField(CMInfo.CM_STR, "receiver", m_serverStub.getMyself().getName());
			userEvent.setEventField(CMInfo.CM_INT, "chType", Integer.toString(nChType));
			userEvent.setEventField(CMInfo.CM_INT, "chKey", Integer.toString(nChKey));
			userEvent.setEventField(CMInfo.CM_INT, "recvPort", Integer.toString(nRecvPort));
			m_serverStub.send(userEvent, strUser);
			
			printMessage("["+strUser+"] requested to receive a dummy event ");
			
			SocketChannel sc = null;
			DatagramChannel dc = null;
			CMDummyEvent due = null;
			if(nChType == CMInfo.CM_SOCKET_CHANNEL)
			{
				printMessage("with the blocking socket channel ("+nChKey+").\n");
				sc = m_serverStub.getBlockSocketChannel(nChKey, strUser);
				if(sc == null)
				{
					System.err.println("CMWinServerEventHandler.processUserEvent(): reqRecv, socket channel not found, key("
							+nChKey+"), user("+strUser+")!");
					return;
				}
				
				due = (CMDummyEvent) m_serverStub.receive(sc);
				if(due == null)
				{
					System.err.println("CMWinServerEventHandler.processUserEvent(): reqRecv, failed to receive a dummy event!");
					return;
				}
				printMessage("received dummy info: "+due.getDummyInfo()+"\n");

			}
			else if(nChType == CMInfo.CM_DATAGRAM_CHANNEL)
			{
				printMessage("with the blocking datagram channel port("+nRecvPort+").\n");
				dc = m_serverStub.getBlockDatagramChannel(nRecvPort);
				if(dc == null)
				{
					System.err.println("CMWinServerEventHandler.processUserEvent(): reqRecv, datagram channel not found, recvPort("
							+nRecvPort+")!");
					return;
				}
				
				due = (CMDummyEvent) m_serverStub.receive(dc);
				if(due == null)
				{
					System.err.println("CMWinServerEventHandler.processUserEvent(): reqRecv, failed to receive a dummy event!");
					return;
				}
				printMessage("received dummy info: "+due.getDummyInfo()+"\n");				
			}
			
		}
		else if(ue.getStringID().equals("testSendRecv"))
		{
			printMessage("Received user event from ["+ue.getSender()+"] to ["+ue.getReceiver()+
					"], (id, "+ue.getID()+"), (string id, "+ue.getStringID()+")\n");

			if(!m_serverStub.getMyself().getName().equals(ue.getReceiver()))
				return;

			CMUserEvent rue = new CMUserEvent();
			rue.setID(222);
			rue.setStringID("testReplySendRecv");
			boolean ret = m_serverStub.send(rue, ue.getSender());
			if(ret)
				printMessage("Sent reply event: (id, "+rue.getID()+"), (string id, "+rue.getStringID()+")\n");
			else
				printMessage("Failed to send the reply event!\n");			
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
						// not yet
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
		switch(fe.getID())
		{
		case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
			printMessage("["+fe.getFileReceiver()+"] requests file("+fe.getFileName()+").\n");
			printMessage("The pull-file request is not automatically permitted!\n");
			printMessage("To change to automatically permit the pull-file request, \n");
			printMessage("set the PERMIT_FILE_TRANSFER field to 1 in the cm-server.conf file\n");
			break;
		case CMFileEvent.REPLY_PERMIT_PULL_FILE:
			if(fe.getReturnCode() == -1)
			{
				printMessage("["+fe.getFileName()+"] does not exist in the owner!\n");
			}
			else if(fe.getReturnCode() == 0)
			{
				printMessage("["+fe.getFileSender()+"] rejects to send file("
						+fe.getFileName()+").\n");
			}
			break;
		case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
			printMessage("["+fe.getFileSender()+"] wants to send a file("+fe.getFilePath()+
					").\n");
			printMessage("The push-file request is not automatically permitted!\n");
			printMessage("To change to automatically permit the push-file request, \n");
			printMessage("set the PERMIT_FILE_TRANSFER field to 1 in the cm-server.conf file\n");
			break;
		case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
			if(fe.getReturnCode() == 0)
			{
				printMessage("["+fe.getFileReceiver()+"] rejected the push-file request!\n");
				printMessage("file path("+fe.getFilePath()+"), size("+fe.getFileSize()+").\n");
			}
			break;
		case CMFileEvent.START_FILE_TRANSFER:
		case CMFileEvent.START_FILE_TRANSFER_CHAN:
			//System.out.println("["+fe.getSenderName()+"] is about to send file("+fe.getFileName()+").");
			printMessage("["+fe.getFileSender()+"] is about to send file("+fe.getFileName()+").\n");
			break;
		case CMFileEvent.END_FILE_TRANSFER:
		case CMFileEvent.END_FILE_TRANSFER_CHAN:
			//System.out.println("["+fe.getSenderName()+"] completes to send file("+fe.getFileName()+", "
			//		+fe.getFileSize()+" Bytes).");
			printMessage("["+fe.getFileSender()+"] completes to send file("+fe.getFileName()+", "
					+fe.getFileSize()+" Bytes).\n");
			String strFile = fe.getFileName();
			if(m_bDistFileProc)
			{
				processFile(fe.getFileSender(), strFile);
				m_bDistFileProc = false;
			}
			break;
		case CMFileEvent.REQUEST_DIST_FILE_PROC:
			//System.out.println("["+fe.getUserName()+"] requests the distributed file processing.");
			printMessage("["+fe.getFileReceiver()+"] requests the distributed file processing.\n");
			m_bDistFileProc = true;
			break;
		case CMFileEvent.CANCEL_FILE_SEND:
		case CMFileEvent.CANCEL_FILE_SEND_CHAN:
			printMessage("["+fe.getFileSender()+"] cancelled the file transfer.\n");
			break;
		case CMFileEvent.CANCEL_FILE_RECV_CHAN:
			printMessage("["+fe.getFileReceiver()+"] cancelled the file request.\n");
			break;
		}
		return;
	}
	
	private void processFile(String strSender, String strFile)
	{
		CMConfigurationInfo confInfo = m_serverStub.getCMInfo().getConfigurationInfo();
		String strFullSrcFilePath = null;
		String strModifiedFile = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];

		long lStartTime = System.currentTimeMillis();

		// change the modified file name
		strModifiedFile = "m-"+strFile;
		strModifiedFile = confInfo.getTransferedFileHome().toString()+File.separator+strSender+
				File.separator+strModifiedFile;

		// stylize the file
		strFullSrcFilePath = confInfo.getTransferedFileHome().toString()+File.separator+strSender+File.separator+strFile;
		File srcFile = new File(strFullSrcFilePath);
		long lFileSize = srcFile.length();
		long lRemainBytes = lFileSize;
		int readBytes = 0;

		try {
			fis = new FileInputStream(strFullSrcFilePath);
			fos = new FileOutputStream(strModifiedFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		try {
			
			while( lRemainBytes > 0 )
			{
				if( lRemainBytes >= CMInfo.FILE_BLOCK_LEN )
				{
					readBytes = fis.read(fileBlock);
				}
				else
				{
					readBytes = fis.read(fileBlock, 0, (int)lRemainBytes);
				}
			
				fos.write(fileBlock, 0, readBytes);
				lRemainBytes -= readBytes;
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// add some process delay here
		for(long i = 0; i < lFileSize/50; i++)
		{
			for(long j = 0; j < lFileSize/50; j++)
			{
				// 
			}
		}

		long lEndTime = System.currentTimeMillis();
		//System.out.println("processing delay: "+(lEndTime-lStartTime)+" ms");
		printMessage("processing delay: "+(lEndTime-lStartTime)+" ms\n");

		// send the modified file to the sender
		CMFileTransferManager.pushFile(strModifiedFile, strSender, m_serverStub.getCMInfo());

		return;
	}
	
	private void processSNSEvent(CMEvent cme)
	{
		CMSNSEvent se = (CMSNSEvent) cme;
		switch(se.getID())
		{
		case CMSNSEvent.CONTENT_DOWNLOAD_REQUEST:
			//System.out.println("["+se.getUserName()+"] requests SNS contents starting at: offset("
			//		+se.getContentOffset()+").");
			printMessage("["+se.getUserName()+"] requests SNS contents starting at: offset("
					+se.getContentOffset()+").\n");
			break;
		case CMSNSEvent.CONTENT_DOWNLOAD_END_RESPONSE:
			if(se.getReturnCode() == 1)
			{
				//System.out.println("["+se.getUserName()+"] has received SNS contents starting at "
				//		+se.getContentOffset()+" successfully.");
				printMessage("["+se.getUserName()+"] has received SNS contents starting at "
						+se.getContentOffset()+" successfully.\n");
			}
			else
			{
				//System.out.println("!! ["+se.getUserName()+" had a problem while receiving SNS "
				//		+ "contents starting at "+se.getContentOffset()+".");
				printMessage("!! ["+se.getUserName()+" had a problem while receiving SNS "
						+ "contents starting at "+se.getContentOffset()+".\n");
			}
			break;
		case CMSNSEvent.CONTENT_UPLOAD_REQUEST:
			//System.out.println("content upload requested by ("+se.getUserName()+"), attached file path: "
			//			+se.getAttachedFileName()+", message: "+se.getMessage());
			printMessage("content upload requested by ("+se.getUserName()+"), message("+se.getMessage()
					+"), #attachement("+se.getNumAttachedFiles()+"), replyID("+se.getReplyOf()
					+"), lod("+se.getLevelOfDisclosure()+")\n");
			break;
		case CMSNSEvent.REQUEST_ATTACHED_FILE:
			printMessage("["+se.getUserName()+"] requests an attached file ["
					+se.getFileName()+"] of SNS content ID["+se.getContentID()+"] written by ["
					+se.getWriterName()+"].\n");
			break;
		}
		return;
	}
	
	private void processMultiServerEvent(CMEvent cme)
	{
		CMConfigurationInfo confInfo = m_serverStub.getCMInfo().getConfigurationInfo();
		CMMultiServerEvent mse = (CMMultiServerEvent) cme;
		switch(mse.getID())
		{
		case CMMultiServerEvent.REQ_SERVER_REG:
			printMessage("server ("+mse.getServerName()+") requests registration: ip("
					+mse.getServerAddress()+"), port("+mse.getServerPort()+"), udpport("
					+mse.getServerUDPPort()+").\n");
			break;
		case CMMultiServerEvent.RES_SERVER_REG:
			if( mse.getReturnCode() == 1 )
			{
				m_server.updateTitle();
				printMessage("server["+mse.getServerName()+"] is successfully registered "
						+ "to the default server.\n");
			}
			else
			{
				printMessage("server["+mse.getServerName()+"] is not registered to the "
						+ "default server.\n");
			}
			break;
		case CMMultiServerEvent.REQ_SERVER_DEREG:
			printMessage("server["+mse.getServerName()+"] requests deregistration.\n");
			break;
		case CMMultiServerEvent.RES_SERVER_DEREG:
			if( mse.getReturnCode() == 1 )
			{
				printMessage("server["+mse.getServerName()+"] is successfully deregistered "
						+ "from the default server.\n");
			}
			else
			{
				printMessage("server["+mse.getServerName()+"] is not deregistered from the "
						+ "default server.\n");
			}
			break;
		case CMMultiServerEvent.ADD_LOGIN:
			if( confInfo.isLoginScheme() )
			{
				// user authentication omitted for the login to an additional server
				//CMInteractionManager.replyToADD_LOGIN(mse, true, m_serverStub.getCMInfo());
				m_serverStub.replyEvent(mse, 1);
			}
			printMessage("["+mse.getUserName()+"] requests login to this server("
								+mse.getServerName()+").\n");
			break;
		case CMMultiServerEvent.ADD_LOGOUT:
			printMessage("["+mse.getUserName()+"] log out this server("+mse.getServerName()
					+").\n");
			break;
		case CMMultiServerEvent.ADD_REQUEST_SESSION_INFO:
			printMessage("["+mse.getUserName()+"] requests session information.\n");
			break;
		}

		return;
	}
	
	private void processMqttEvent(CMEvent cme)
	{
		switch(cme.getID())
		{
		case CMMqttEvent.CONNECT:
			CMMqttEventCONNECT conEvent = (CMMqttEventCONNECT)cme;
			//printMessage("received "+conEvent+"\n");
			printMessage("["+conEvent.getUserName()
				+"] requests to connect MQTT service.\n");
			break;
		case CMMqttEvent.PUBLISH:
			CMMqttEventPUBLISH pubEvent = (CMMqttEventPUBLISH)cme;
			//printMessage("received "+pubEvent+"\n");
			printMessage("["+pubEvent.getSender()+"] requests to publish, ");
			printMessage("[packet ID: "+pubEvent.getPacketID()
					+"], [topic: "+pubEvent.getTopicName()+"], [msg: "
					+pubEvent.getAppMessage()+"], [qos: "+pubEvent.getQoS()+"]\n");
			break;
		case CMMqttEvent.PUBACK:
			CMMqttEventPUBACK pubackEvent = (CMMqttEventPUBACK)cme;
			//printMessage("received "+pubackEvent+"\n");
			printMessage("["+pubackEvent.getSender()+"] sent CMMqttEvent.PUBACK, "
					+ "[packet ID: "+pubackEvent.getPacketID()+"]\n");
			break;
		case CMMqttEvent.PUBREC:
			CMMqttEventPUBREC pubrecEvent = (CMMqttEventPUBREC)cme;
			//printMessage("received "+pubrecEvent+"\n");
			printMessage("["+pubrecEvent.getSender()+"] sent CMMqttEvent.PUBREC, "
					+ "[packet ID: "+pubrecEvent.getPacketID()+"]\n");
			break;
		case CMMqttEvent.PUBREL:
			CMMqttEventPUBREL pubrelEvent = (CMMqttEventPUBREL)cme;
			//printMessage("received "+pubrelEvent+"\n");
			printMessage("["+pubrelEvent.getSender()+"] sent CMMqttEventPUBREL, "
					+ "[packet ID: "+pubrelEvent.getPacketID()+"]\n");
			break;
		case CMMqttEvent.PUBCOMP:
			CMMqttEventPUBCOMP pubcompEvent = (CMMqttEventPUBCOMP)cme;
			//printMessage("received "+pubcompEvent+"\n");
			printMessage("["+pubcompEvent.getSender()+"] sent CMMqttEvent.PUBCOMP, "
					+ "[packet ID: "+pubcompEvent.getPacketID()+"]\n");
			break;
		case CMMqttEvent.SUBSCRIBE:
			CMMqttEventSUBSCRIBE subEvent = (CMMqttEventSUBSCRIBE)cme;
			//printMessage("received "+subEvent+"\n");
			printMessage("["+subEvent.getSender()+"] requests to subscribe, "
					+ subEvent.getTopicQoSList()+"\n");
			break;
		case CMMqttEvent.UNSUBSCRIBE:
			CMMqttEventUNSUBSCRIBE unsubEvent = (CMMqttEventUNSUBSCRIBE)cme;
			//printMessage("received "+unsubEvent+"\n");
			printMessage("["+unsubEvent.getSender()+"] requests to unsubscribe, "
					+ unsubEvent.getTopicList()+"\n");
			break;
		case CMMqttEvent.DISCONNECT:
			CMMqttEventDISCONNECT disconEvent = (CMMqttEventDISCONNECT)cme;
			//printMessage("received "+disconEvent+"\n");
			printMessage("["+disconEvent.getSender()
				+"] requests to disconnect MQTT service.\n");
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
		m_server.printMessage(strText);
	}
	
	/*
	private void setMessage(String strText)
	{
		m_outTextArea.setText(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
	}
	*/

}
