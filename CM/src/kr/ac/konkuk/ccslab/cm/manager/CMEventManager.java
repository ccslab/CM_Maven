package kr.ac.konkuk.ccslab.cm.manager;

import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMConcurrencyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMConsistencyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMDataEvent;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMEventInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

public class CMEventManager {

	public static CMEventReceiver startReceivingEvent(CMInfo cmInfo)
	{
		CMEventInfo eventInfo = cmInfo.getEventInfo();
		CMEventReceiver eventReceiver = new CMEventReceiver(cmInfo);
		eventReceiver.start();
		eventInfo.setEventReceiver(eventReceiver);
		
		return eventReceiver;
	}
	
	public static ByteBuffer marshallEvent(CMEvent cmEvent)
	{
		return cmEvent.marshall();
	}
	
	public static CMEvent unmarshallEvent(ByteBuffer buf)
	{
		if( buf == null )
		{
			System.out.println("CMEventManager.unmarshallEvent(), ByteBuffer is null.");
			return null;
		}
		
		int nEventType = getEventType(buf);
		
		switch(nEventType)
		{
		case CMInfo.CM_SESSION_EVENT:
			CMSessionEvent se = new CMSessionEvent(buf);
			return se;
		case CMInfo.CM_INTEREST_EVENT:
			CMInterestEvent ie = new CMInterestEvent(buf);
			return ie;
		case CMInfo.CM_DATA_EVENT:
			CMDataEvent de = new CMDataEvent(buf);
			return de;
		case CMInfo.CM_CONSISTENCY_EVENT:
			CMConsistencyEvent cce = new CMConsistencyEvent(buf);
			return cce;
		case CMInfo.CM_CONCURRENCY_EVENT:
			CMConcurrencyEvent cue = new CMConcurrencyEvent(buf);
			return cue;
		case CMInfo.CM_FILE_EVENT:
			CMFileEvent fe = new CMFileEvent(buf);
			return fe;
		case CMInfo.CM_MULTI_SERVER_EVENT:
			CMMultiServerEvent mse = new CMMultiServerEvent(buf);
			return mse;
		case CMInfo.CM_SNS_EVENT:
			CMSNSEvent sse = new CMSNSEvent(buf);
			return sse;
		case CMInfo.CM_DUMMY_EVENT:
			CMDummyEvent due = new CMDummyEvent(buf);
			return due;
		case CMInfo.CM_USER_EVENT:
			CMUserEvent ue = new CMUserEvent(buf);
			return ue;
		default:
			System.out.println("CMEventManager.unmarshallEvent(), unknown event type: "+nEventType);
			return null;
		}
	}
	
	public static int getEventType(ByteBuffer buf)
	{
		int nType = -1;
		nType = buf.getInt(Integer.BYTES);
		
		return nType;
	}
	
	///////////////////////////////////////////////////////////////
	// event transmission methods
	
	public static boolean unicastEvent(CMEvent cme, String strReceiver, CMInfo cmInfo)
	{
		return unicastEvent(cme, strReceiver, CMInfo.CM_STREAM, 0, cmInfo);
	}
	
	public static boolean unicastEvent(CMEvent cme, String strReceiver, int opt, CMInfo cmInfo)
	{
		return unicastEvent(cme, strReceiver, opt, 0, cmInfo);
	}
	
	public static boolean unicastEvent(CMEvent cme, String strReceiver, int opt, int nChNum, CMInfo cmInfo)
	{
		CMMember loginUsers = null;
		ByteBuffer bufEvent = null;
		SocketChannel sc = null;
		DatagramChannel dc = null;
		CMUser user = null;
		int nSentBytes = -1;
		CMServer tServer = null;
		CMChannelInfo chInfo = null;
		String strTargetAddress = null;
		int nTargetPort = -1;
		CMCommInfo commInfo = cmInfo.getCommInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
				
		//// find a destination channel
		
		// check if the destination is the default server or additional server
		tServer = interInfo.getDefaultServerInfo();
		if(!strReceiver.equals(tServer.getServerName()))
		{
			tServer = interInfo.findAddServer(strReceiver);
		}
		
		if(tServer != null)
		{
			// target is the default server or an additional server
			if(opt == CMInfo.CM_STREAM)
			{
				chInfo = tServer.getSocketChannelInfo();
				sc = (SocketChannel) chInfo.findChannel(nChNum);
				if( sc == null )
				{
					System.err.println("CMEventManager.unicastEvent(), channel ("+strReceiver
							+", "+nChNum+") not found.");
					bufEvent = null;
					return false;
				}

			}
			else if(opt == CMInfo.CM_DATAGRAM)
			{
				strTargetAddress = tServer.getServerAddress();
				nTargetPort = tServer.getServerUDPPort();
				if(strTargetAddress.equals("") || nTargetPort == -1)
				{
					System.err.println("CMEventManager.unicastEvent(), datagram target information unavailable, "
							+"addr("+strTargetAddress+"), udp port("+nTargetPort+").");
					return false;
				}
			}
		}
		else
		{
			// check if the destination is a user
			loginUsers = interInfo.getLoginUsers();
			user = loginUsers.findMember(strReceiver);
			if( user == null )
			{
				System.err.println("CMEventManager.unicastEvent(), target("+strReceiver+") not found.");
				return false;
			}
			if(opt == CMInfo.CM_STREAM)
			{
				chInfo = user.getNonBlockSocketChannelInfo();
				sc = (SocketChannel) chInfo.findChannel(nChNum);
				if( sc == null )
				{
					System.err.println("CMEventManager.unicastEvent(), channel ("+strReceiver
							+", "+nChNum+") not found.");
					return false;
				}

			}
			else if(opt == CMInfo.CM_DATAGRAM)
			{
				strTargetAddress = user.getHost();
				nTargetPort = user.getUDPPort();
				if(strTargetAddress.equals("") || nTargetPort == -1)
				{
					System.err.println("CMEventManager.unicastEvent(), datagram target information unavailable, "
							+"addr("+strTargetAddress+"), udp port("+nTargetPort+").");
					return false;
				}
			}
		}

		sleepForSimTransDelay(cmInfo);

		// marshall event
		bufEvent = CMEventManager.marshallEvent(cme);
		if( bufEvent == null )
		{
			System.err.println("CMEventManager.unicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}

		// send the event
		switch(opt)
		{
		case CMInfo.CM_STREAM:
			nSentBytes = CMCommManager.sendMessage(bufEvent, sc);
			break;
		case CMInfo.CM_DATAGRAM:
			dc = (DatagramChannel) commInfo.getNonBlockDatagramChannelInfo().findChannel(nChNum);
			if(dc == null)
			{
				System.err.println("CMEventManager.unicastEvent(), datagramChannel("+nChNum+") not found.");
				bufEvent = null;
				return false;
			}
			nSentBytes = CMCommManager.sendMessage(bufEvent, dc, strTargetAddress, nTargetPort);			
			break;
		default:
			System.err.println("CMEventManager.unicastEvent(), incorrect option: "+opt);
			bufEvent = null;
			return false;
		}
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMEventManager.unicastEvent(), sent "+nSentBytes+" bytes,"
							+" event(type: "+cme.getType()+", id: "+cme.getID()+").");
			System.out.println("receiver("+strReceiver+"), opt("+opt+"), ch#("+nChNum+").");
		}
		
		bufEvent = null;	// clear the ByteBuffer
		return true;
	}
	
	public static boolean unicastEvent(CMEvent cme, SocketChannel sc)
	{
		int nSentBytes = -1;

		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.unicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		nSentBytes = CMCommManager.sendMessage(bufEvent, sc);
		bufEvent = null;
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMEventManager.unicastEvent(), sent "+nSentBytes+" bytes, with"
					+sc.toString());
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public static boolean multicastEvent(CMEvent cme, String strSessionName, String strGroupName, CMInfo cmInfo)
	{
		return multicastEvent(cme, strSessionName, strGroupName, 0, cmInfo);
	}
	
	public static boolean multicastEvent(CMEvent cme, String strSessionName, String strGroupName, int nChNum, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMSession session = interInfo.findSession(strSessionName);
		CMGroup group = null;
		DatagramChannel dc = null;
		ByteBuffer bufEvent = null;
		int nSentBytes = -1;
		
		if(session == null)
		{
			System.err.println("CMEventManager.multicastEvent(), session("+strSessionName+") not found.");
			return false;
		}
		
		group = session.findGroup(strGroupName);
		
		if(group == null)
		{
			System.err.println("CMEventManager.multicastEvent(), group("+strGroupName+") not found.");
			return false;
		}
		
		dc = (DatagramChannel) group.getMulticastChannelInfo().findChannel(nChNum);
		
		if(dc == null)
		{
			System.err.println("CMEventManager.multicastEvent(), channel("+nChNum+") not found.");
			return false;
		}

		sleepForSimTransDelay(cmInfo);

		bufEvent = CMEventManager.marshallEvent(cme);
		
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.multicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		nSentBytes = CMCommManager.sendMessage(bufEvent, dc, group.getGroupAddress(), group.getGroupPort());
		bufEvent = null;
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMEventManager.multicastEvent(), sent "+nSentBytes+" bytes, with"
					+dc.toString()+" session("+strSessionName+"), group("+strGroupName+"), channel("
					+nChNum+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public static boolean multicastEvent(CMEvent cme, DatagramChannel dc, String strMA, int nPort)
	{
		int nSentBytes = -1;
		
		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.multicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		nSentBytes = CMCommManager.sendMessage(bufEvent, dc, strMA, nPort);
		bufEvent = null;
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMEventManager.multicastEvent(), sent "+nSentBytes+" bytes, with"
					+dc.toString()+", addr("+strMA+"), port("+nPort+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public static boolean broadcastEvent(CMEvent cme, CMInfo cmInfo)
	{
		return broadcastEvent(cme, CMInfo.CM_STREAM, 0, cmInfo);
	}
	
	public static boolean broadcastEvent(CMEvent cme, int opt, CMInfo cmInfo)
	{
		return broadcastEvent(cme, opt, 0, cmInfo);
	}
	
	// send an event to all login users (server)
	public static boolean broadcastEvent(CMEvent cme, int opt, int nChNum, CMInfo cmInfo)
	{
		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.boradcastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		Iterator<CMUser> iter = cmInfo.getInteractionInfo().getLoginUsers().getAllMembers().iterator();
		CMUser tuser = null;
		
		switch(opt)
		{
		case CMInfo.CM_STREAM:
			while(iter.hasNext())
			{
				tuser = iter.next();
				SocketChannel sc = (SocketChannel) tuser.getNonBlockSocketChannelInfo().findChannel(nChNum);
				if( sc == null )
				{
					System.err.println("CMEventManager.broadcastEvent(), SocketChannel of user("
							+tuser.getName()+") not found.");
					continue;
				}
				if( !sc.isOpen() )
				{
					System.err.println("CMEventManager.broadcastEvent(), SocketChannel of user("
							+tuser.getName()+") is closed.");
					continue;
				}

				sleepForSimTransDelay(cmInfo);

				CMCommManager.sendMessage(bufEvent, sc);
			}
			break;
		case CMInfo.CM_DATAGRAM:
			CMChannelInfo dcInfo = cmInfo.getCommInfo().getNonBlockDatagramChannelInfo();
			DatagramChannel dc = (DatagramChannel) dcInfo.findChannel(nChNum);
			if(dc == null)
			{
				System.err.println("CMEventManager.broadcastEvent(), DatagramChannel("+nChNum
						+") not found.");
				bufEvent = null;
				return false;
			}
			while(iter.hasNext())
			{
				tuser = iter.next();

				sleepForSimTransDelay(cmInfo);

				CMCommManager.sendMessage(bufEvent, dc, tuser.getHost(), tuser.getUDPPort());
			}
			break;
		default:
			System.err.println("CMEventManager.broadcastEvent(), incorrect option: "+opt);
			bufEvent = null;
			return false;
		}

		if(CMInfo._CM_DEBUG_2)
		{
			int nUserNum = cmInfo.getInteractionInfo().getLoginUsers().getMemberNum();
			System.out.println("CMEventManager.broadcastEvent(), succeeded to ("+nUserNum
					+") users: opt("+opt+"), ch#("+nChNum+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		bufEvent = null;
		return true;
	}
	
	public static boolean castEvent(CMEvent cme, CMMember users, CMInfo cmInfo)
	{
		return castEvent(cme, users, CMInfo.CM_STREAM, 0, cmInfo);
	}
	
	public static boolean castEvent(CMEvent cme, CMMember users, int opt, CMInfo cmInfo)
	{
		return castEvent(cme, users, opt, 0, cmInfo);
	}
	
	// send an event to a specific user group with multiple unicast transmissions
	public static boolean castEvent(CMEvent cme, CMMember users, int opt, int nChNum, CMInfo cmInfo)
	{
		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.castEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}

		Iterator<CMUser> iter = users.getAllMembers().iterator();
		CMUser tuser = null;

		switch(opt)
		{
		case CMInfo.CM_STREAM:
			while(iter.hasNext())
			{
				tuser = iter.next();
				SocketChannel sc = (SocketChannel) tuser.getNonBlockSocketChannelInfo().findChannel(nChNum);
				if( sc == null )
				{
					System.err.println("CMEventManager.castEvent(), SocketChannel of user("
							+tuser.getName()+") not found.");
					continue;
				}
				if( !sc.isOpen() )
				{
					System.err.println("CMEventManager.castEvent(), SocketChannel of user("
							+tuser.getName()+") is closed.");
					continue;
				}

				sleepForSimTransDelay(cmInfo);

				CMCommManager.sendMessage(bufEvent, sc);
			}
			break;
		case CMInfo.CM_DATAGRAM:
			CMChannelInfo dcInfo = cmInfo.getCommInfo().getNonBlockDatagramChannelInfo();
			DatagramChannel dc = (DatagramChannel) dcInfo.findChannel(nChNum);
			if(dc == null)
			{
				System.err.println("CMEventManager.castEvent(), DatagramChannel("+nChNum
						+") not found.");
				bufEvent = null;
				return false;
			}
			while(iter.hasNext())
			{
				tuser = iter.next();
				
				sleepForSimTransDelay(cmInfo);

				CMCommManager.sendMessage(bufEvent, dc, tuser.getHost(), tuser.getUDPPort());
			}
			break;
		default:
			System.err.println("CMEventManager.castEvent(), incorrect option: "+opt);
			bufEvent = null;
			return false;
		}

		if(CMInfo._CM_DEBUG_2)
		{
			int nUserNum = users.getMemberNum();
			System.out.println("CMEventManager.castEvent(), succeeded to ("+nUserNum+") users: opt("
								+opt+"), ch#("+nChNum+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		bufEvent = null;
		return true;
	}
	
	///////////////////////////////////////////////////////////////
	// methods related to the management of mapping between channel and users
	
	/*add a channel with a strName, ch ,nChNum, and loginUsers information. If strName exists in loginUsers, add (cs, nNum). 
	Otherwise, addChannel() fails. Initial nChNum value is 0.
	*/
	public static boolean addChannel(String strUserName, SelectableChannel ch, int nChNum, CMMember loginUsers)
	{
		CMUser user = null;
		user = loginUsers.findMember(strUserName);		
		
		if(user == null)
		{
			System.out.println("CMEventManager.addChannel(), user("+strUserName+") not found in the login user list.");
			return false;
		}
		
		boolean result = user.getNonBlockSocketChannelInfo().addChannel(ch, nChNum);
		
		return result;
	}
	
	//remove a channel with strName and nChNum from loginUsers. 
	//If all channels are removed, ??? (not clear)
	public static boolean removeChannel(String strUserName, SelectableChannel ch, int nChNum, CMMember loginUsers)
	{
		CMUser user = null;
		user = loginUsers.findMember(strUserName);
		
		if(user == null)
		{
			System.out.println("CMEventManager.removeChannel(), user("+strUserName+") not found in the login user list.");
			return false;
		}
		
		boolean result = user.getNonBlockSocketChannelInfo().removeChannel(nChNum);
		return result;
	}

	//remove a channel with ch from loginUsers. 
	//If all channels are removed, ??? (not clear)
	public static boolean removeChannel(SelectableChannel ch, CMMember loginUsers)
	{
		CMUser tuser = null;
		int nChNum = -1;
		boolean bFound = false;
		
		if( ch == null )
		{
			System.out.println("CMEventManager.removeChannel(), channel is null.");
			return false;
		}
		
		Iterator<CMUser> iter = loginUsers.getAllMembers().iterator();
		while(iter.hasNext() && !bFound)
		{
			tuser = iter.next();
			nChNum = tuser.getNonBlockSocketChannelInfo().findChannelIndex(ch);
			if(nChNum != -1)
			{
				bFound = true;
			}
		}
		
		if(!bFound)
		{
			System.out.println("CMEventManager.removeChannel(), channel(code: "+ch.hashCode()+") not found.");
			return false;
		}
		
		boolean ret = tuser.getNonBlockSocketChannelInfo().removeChannel(nChNum);
		
		return ret;
	}
	
	//remove all channels of strName from loginUsers
	public static boolean removeAllChannels(String strUserName, CMMember loginUsers)
	{
		CMUser user = null;
		user = loginUsers.findMember(strUserName);
		
		if(user == null)
		{
			System.out.println("CMEventManager.removeAllChannels(), user("+strUserName+") not found in the login user list.");
			return false;
		}
		
		user.getNonBlockSocketChannelInfo().removeAllChannels();
		return true;
	}
	
	// remove all additional channels(ch# greater than 0) of strUserName from loginUsers.
	public static boolean removeAllAddedChannels(String strUserName, CMMember loginUsers)
	{
		CMUser user = null;
		user = loginUsers.findMember(strUserName);
		
		if(user == null)
		{
			System.out.println("CMEventManager.closeAllAddedChannels(), user("+strUserName+") not found in the login user list.");
			return false;
		}
		
		boolean ret = user.getNonBlockSocketChannelInfo().removeAllAddedChannels();

		return ret;
	}
	
	// find a channel with strUserName and nChNum.
	public static SelectableChannel findChannel(String strUserName, int nChNum, CMMember loginUsers)
	{
		CMUser user = null;
		SelectableChannel ch = null;

		user = loginUsers.findMember(strUserName);
		if(user == null)
		{
			System.err.println("CMEventManager.findChannel(), user("+strUserName
					+")"+" ch#("+nChNum+"), user not found.");
			return null;
		}
		
		ch = user.getNonBlockSocketChannelInfo().findChannel(nChNum);
		if(ch == null)
		{
			System.err.println("CMEventManager.findChannel(), user("+strUserName
					+")"+" ch#("+nChNum+"), channel # not found.");
			return null;
		}
		
		return ch;
	}
	
	// find a user who connects with ch in loginUsers.
	public static String findUserWithChannel(SelectableChannel ch, CMMember loginUsers)
	{
		String strUserName = null;
		boolean bFound = false;
		
		if(ch == null)
		{
			System.err.println("CMEventManager.findUserWithChannel(), channel is null.");
			return null;
		}
		
		Iterator<CMUser> iter = loginUsers.getAllMembers().iterator();
		while(iter.hasNext() && !bFound)
		{
			CMUser tuser = iter.next();
			int nChNum = tuser.getNonBlockSocketChannelInfo().findChannelIndex(ch);
			if(nChNum != -1)
			{
				strUserName = tuser.getName();
				bFound = true;
				if(CMInfo._CM_DEBUG_2)
					System.out.println("CMEventManager.findUserWithChannel(), user("+strUserName+").");
			}
		}
		
		return strUserName;
	}

	//////////////////////////////////////
	// add some sleep in order to simulate transmission delay

	private static void sleepForSimTransDelay(CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		int nSimTransDelay = confInfo.getSimTransDelay();

		if(nSimTransDelay > 0)
		{
			try {
				Thread.sleep(nSimTransDelay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}

		return;
	}
}
