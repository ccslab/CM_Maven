package kr.ac.konkuk.ccslab.cm.thread;
import java.nio.channels.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMSNSInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;

public class CMEventReceiver extends Thread {
	private CMBlockingEventQueue m_queue;
	private CMInfo m_cmInfo;
	
	public CMEventReceiver(CMInfo cmInfo)
	{
		m_cmInfo = cmInfo;
		m_queue = cmInfo.getCommInfo().getRecvBlockingEventQueue();
	}
	
	public void run()
	{
		CMMessage msg = null;
		boolean bForwardToApp = true;
		CMEventSynchronizer eventSync = m_cmInfo.getEventInfo().getEventSynchronizer();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMEventReceiver starts to receive events.");
		while(!Thread.currentThread().isInterrupted())
		{
			msg = m_queue.pop();
			
			if(msg == null)
			{
				if(CMInfo._CM_DEBUG_2)
					System.out.println("CMEventReceiver.run(), msg is null.");
				break;
			}
			
			if(msg.m_buf == null)
			{
				/*
				String strUserName = CMEventManager.findUserWithChannel(msg.m_ch, m_cmInfo.getInteractionInfo().getLoginUsers());
				if(strUserName == null)
					System.out.println("CMEventReceiver.run(), user not found.");
				else
					System.out.println("CMEventRecevier.run(), user: "+strUserName);
				*/
				if(CMInfo._CM_DEBUG_2)
					System.out.println("CMEventRecevier.run(), ByteBuffer is null.");
				
				processUnexpectedDisconnection(msg.m_ch);
				continue;
			}
			
			// deliver msg to interaction manager
			bForwardToApp = CMInteractionManager.processEvent(msg, m_cmInfo);

			// check whether the main thread is waiting for an event
			CMEvent cme = CMEventManager.unmarshallEvent(msg.m_buf);

			if(eventSync.isWaiting() && !cme.getSender().isEmpty() && 
					cme.getSender().equals(eventSync.getWaitedReceiver()) &&
					cme.getType() == eventSync.getWaitedEventType() && 
					cme.getID() == eventSync.getWaitedEventID())
			{
				// waiting for a single reply event
				eventSync.setReplyEvent(cme);
				synchronized(eventSync)
				{
					eventSync.notify();
				}
				// initialize waited event info
				eventSync.setWaitedEvent(-1, -1, null);
			}
			else if(eventSync.isWaiting() && 
					eventSync.getWaitedEventType() == cme.getType() &&
					eventSync.getWaitedEventID() == cme.getID() && 
					eventSync.getMinNumWaitedEvents() >= 0 )
			{
				// waiting for more than one reply event
				eventSync.addReplyEvent(cme);
				synchronized(eventSync)
				{
					if(eventSync.isCompleteReplyEvents())
					{
						eventSync.notify();
						// initialize waited event info
						eventSync.setWaitedEvent(-1, -1, null);
						eventSync.setMinNumWaitedEvents(0);
					}
				}
			}
			else
			{
				if(bForwardToApp)
				{
					// deliver msg to stub module
					m_cmInfo.getEventHandler().processEvent(cme);
					
					if(cme.getType() == CMInfo.CM_USER_EVENT)
					{
						((CMUserEvent)cme).removeAllEventFields();	// clear all event fields
					}
					else if(cme.getType() == CMInfo.CM_FILE_EVENT)
					{
						((CMFileEvent)cme).setFileBlock(null);	// clear the file block
					}
					cme = null;			// clear the event				
				}				
			}
			//msg.m_buf = null;	// clear the received ByteBuffer

		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMEventReceiver is terminated.");
	}
	
	// handle unexpected disconnection with the server or the client
	private void processUnexpectedDisconnection(SelectableChannel ch)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer tserver = null;
		//int nChIndex = -1;
		Integer chKey = null;
		CMChannelInfo<Integer> chInfo = null;
		Iterator<CMServer> iterAddServer = null;
		boolean bFound = false;
		CMFileTransferInfo fInfo = m_cmInfo.getFileTransferInfo();
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			// find channel from default server
			chInfo = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
			chKey = chInfo.findChannelKey(ch);
			if(chKey == null) // ch key not found
			{
				// find channel from additional server list
				iterAddServer = interInfo.getAddServerList().iterator();
				bFound = false;
				while(iterAddServer.hasNext() && !bFound)
				{
					tserver = iterAddServer.next();
					chInfo = tserver.getNonBlockSocketChannelInfo();
					chKey = chInfo.findChannelKey(ch);
					if(chKey != null)
						bFound = true;
				}
				if(bFound)
				{
					if(chKey.intValue() == 0)
						chInfo.removeAllChannels();
					else if(chKey.intValue() > 0)
						chInfo.removeChannel(chKey);
				}				
			}
			else if(chKey.intValue() == 0)	// default channel
			{
				// remove all non-blocking channels
				chInfo.removeAllChannels();
				// remove all blocking channels
				interInfo.getDefaultServerInfo().getBlockSocketChannelInfo().removeAllChannels();
				
				// For the clarity, the client must be back to initial state (not yet)
				// stop all the file-transfer threads
				//List<Runnable> ftList = fInfo.getExecutorService().shutdownNow();
				// remove all the ongoing file-transfer info about the default server
				fInfo.removeRecvFileList(interInfo.getDefaultServerInfo().getServerName());
				fInfo.removeSendFileList(interInfo.getDefaultServerInfo().getServerName());
				// remove all the ongoing sns related file-transfer info at the client
				snsInfo.getRecvSNSAttachList().removeAllSNSAttach();
				// remove all session info
				interInfo.getSessionList().removeAllElements();
				// initialize the client state
				interInfo.getMyself().setState(CMInfo.CM_INIT);
				
				System.err.println("CMEventReceiver.processUndexpectedDisconnection(); The default server is disconnected.");
				
				// notify to the application
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.UNEXPECTED_SERVER_DISCONNECTION);
				m_cmInfo.getEventHandler().processEvent(se);
				se = null;
			}
			else if(chKey.intValue() > 0) // additional channel
			{
				chInfo.removeChannel(chKey);
			}
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// find user with channel
			String strUser = CMEventManager.findUserWithSocketChannel(ch, interInfo.getLoginUsers());
			if(strUser != null)
			{
				CMUser user = interInfo.getLoginUsers().findMember(strUser);
				// find channel index
				chKey = user.getNonBlockSocketChannelInfo().findChannelKey(ch);
				if(chKey == null)
				{
					System.err.println("CMEventReceiver.processUnexpectedDisconnection(), key not found "
							+ "for the channel(hash code: "+ ch.hashCode() +")!");
				}
				else if(chKey.intValue() == 0)
				{
					// if the removed channel is default channel (#ch:0), process logout of the user
					CMSessionEvent tse = new CMSessionEvent();
					tse.setID(CMSessionEvent.LOGOUT);
					tse.setUserName(user.getName());
					CMMessage msg = new CMMessage();
					msg.m_buf = CMEventManager.marshallEvent(tse);
					CMInteractionManager.processEvent(msg, m_cmInfo);
					m_cmInfo.getEventHandler().processEvent(tse);
					tse = null;
					msg.m_buf = null;
				}
				else if(chKey.intValue() > 0)
				{
					// remove the channel
					user.getNonBlockSocketChannelInfo().removeChannel(chKey);
				}
				
			}
			else if(CMConfigurator.isDServer(m_cmInfo))
			{
				// process disconnection with additional server
				iterAddServer = interInfo.getAddServerList().iterator();
				bFound = false;
				while(iterAddServer.hasNext() && !bFound)
				{
					tserver = iterAddServer.next();
					chInfo = tserver.getNonBlockSocketChannelInfo();
					chKey = chInfo.findChannelKey(ch);
					if(chKey != null)
						bFound = true;
				}
				if(bFound)
				{
					if(chKey.intValue() == 0)
					{
						// notify clients of the deregistration
						CMMultiServerEvent mse = new CMMultiServerEvent();
						mse.setID(CMMultiServerEvent.NOTIFY_SERVER_LEAVE);
						mse.setServerName(tserver.getServerName());
						CMEventManager.broadcastEvent(mse, m_cmInfo);

						chInfo.removeAllChannels();
						interInfo.removeAddServer(tserver.getServerName());
						mse = null;
					}
					else if(chKey.intValue() > 0)
						chInfo.removeChannel(chKey);
				}
			}
			else
			{
				// process disconnection with the default server
				// find channel from default server
				chInfo = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
				chKey = chInfo.findChannelKey(ch);
				
				if(chKey == null)
				{
					System.err.println("CMEventReceiver.processUnexpectedDisconnection(); key not found "
							+"for the channel(hash code: "+ch.hashCode()+")!");
				}
				else if(chKey == 0)	// default channel
				{
					chInfo.removeAllChannels();
					// For the clarity, the client must be back to initial state (not yet)
					interInfo.getMyself().setState(CMInfo.CM_INIT);
					System.err.println("The default server is disconnected.");
				}
				else if(chKey > 0) // additional channel
				{
					chInfo.removeChannel(chKey);
				}
			}
		}
		
		if(CMInfo._CM_DEBUG_2)
			System.out.println("CMEventReceiver.processUnexpectedDisconnection(), ends.");
		
		return;
	}
}
