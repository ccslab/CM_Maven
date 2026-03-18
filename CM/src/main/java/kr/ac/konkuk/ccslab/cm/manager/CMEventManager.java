package kr.ac.konkuk.ccslab.cm.manager;

import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
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
import kr.ac.konkuk.ccslab.cm.event.filesync.*;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventDISCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPINGREQ;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPINGRESP;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBCOMP;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREC;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREL;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventSUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventSUBSCRIBE;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventUNSUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventUNSUBSCRIBE;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

public class CMEventManager {

	public synchronized static CMEventReceiver startReceivingEvent()
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMEventInfo eventInfo = CMEventInfo.getInstance();
		ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
		CMEventReceiver eventReceiver = new CMEventReceiver();
		//eventReceiver.start();
		Future<?> future = es.submit(eventReceiver);
		eventInfo.setEventReceiver(eventReceiver);
		eventInfo.setEventReceiverFuture(future);
		
		return eventReceiver;
	}
	
	public synchronized static ByteBuffer marshallEvent(CMEvent cmEvent)
	{
		return cmEvent.marshall();
	}
	
	public synchronized static CMEvent unmarshallEvent(ByteBuffer buf)
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
			case CMInfo.CM_MQTT_EVENT:
				int nEventID = getEventID(buf);
				switch(nEventID)
				{
					case CMMqttEvent.CONNECT:
						CMMqttEventCONNECT conEvent = new CMMqttEventCONNECT(buf);
						return conEvent;
					case CMMqttEvent.CONNACK:
						CMMqttEventCONNACK connackEvent = new CMMqttEventCONNACK(buf);
						return connackEvent;
					case CMMqttEvent.PUBLISH:
						CMMqttEventPUBLISH pubEvent = new CMMqttEventPUBLISH(buf);
						return pubEvent;
					case CMMqttEvent.PUBACK:
						CMMqttEventPUBACK pubackEvent = new CMMqttEventPUBACK(buf);
						return pubackEvent;
					case CMMqttEvent.PUBREC:
						CMMqttEventPUBREC pubrecEvent = new CMMqttEventPUBREC(buf);
						return pubrecEvent;
					case CMMqttEvent.PUBREL:
						CMMqttEventPUBREL pubrelEvent = new CMMqttEventPUBREL(buf);
						return pubrelEvent;
					case CMMqttEvent.PUBCOMP:
						CMMqttEventPUBCOMP pubcompEvent = new CMMqttEventPUBCOMP(buf);
						return pubcompEvent;
					case CMMqttEvent.SUBSCRIBE:
						CMMqttEventSUBSCRIBE subEvent = new CMMqttEventSUBSCRIBE(buf);
						return subEvent;
					case CMMqttEvent.SUBACK:
						CMMqttEventSUBACK subackEvent = new CMMqttEventSUBACK(buf);
						return subackEvent;
					case CMMqttEvent.UNSUBSCRIBE:
						CMMqttEventUNSUBSCRIBE unsubEvent = new CMMqttEventUNSUBSCRIBE(buf);
						return unsubEvent;
					case CMMqttEvent.UNSUBACK:
						CMMqttEventUNSUBACK unsubackEvent = new CMMqttEventUNSUBACK(buf);
						return unsubackEvent;
					case CMMqttEvent.PINGREQ:
						CMMqttEventPINGREQ pingreqEvent = new CMMqttEventPINGREQ(buf);
						return pingreqEvent;
					case CMMqttEvent.PINGRESP:
						CMMqttEventPINGRESP pingrespEvent = new CMMqttEventPINGRESP(buf);
						return pingrespEvent;
					case CMMqttEvent.DISCONNECT:
						CMMqttEventDISCONNECT disconEvent = new CMMqttEventDISCONNECT(buf);
						return disconEvent;
					default:
						System.err.println("CMEventManager.unmarshallEvent(), unknown MQTT event ID: "+nEventID);
						return null;
				}
			case CMInfo.CM_FILE_SYNC_EVENT:
				int eventID = getEventID(buf);
				switch(eventID) {
					case CMFileSyncEvent.START_FILE_LIST:
						CMFileSyncEventStartFileList startFileList = new CMFileSyncEventStartFileList(buf);
						return startFileList;
					case CMFileSyncEvent.START_FILE_LIST_ACK:
						CMFileSyncEventStartFileListAck startFileListAck = new CMFileSyncEventStartFileListAck(buf);
						return startFileListAck;
					case CMFileSyncEvent.FILE_ENTRIES:
						CMFileSyncEventFileEntries fileEntries = new CMFileSyncEventFileEntries(buf);
						return fileEntries;
					case CMFileSyncEvent.FILE_ENTRIES_ACK:
						CMFileSyncEventFileEntriesAck fileEntriesAck = new CMFileSyncEventFileEntriesAck(buf);
						return fileEntriesAck;
					case CMFileSyncEvent.END_FILE_LIST:
						CMFileSyncEventEndFileList endFileList = new CMFileSyncEventEndFileList(buf);
						return endFileList;
					case CMFileSyncEvent.END_FILE_LIST_ACK:
						CMFileSyncEventEndFileListAck endFileListAck = new CMFileSyncEventEndFileListAck(buf);
						return endFileListAck;
					case CMFileSyncEvent.REQUEST_NEW_FILES:
						CMFileSyncEventRequestNewFiles reqNewFiles = new CMFileSyncEventRequestNewFiles(buf);
						return reqNewFiles;
					case CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM:
						CMFileSyncEventStartFileBlockChecksum startChecksum =
								new CMFileSyncEventStartFileBlockChecksum(buf);
						return startChecksum;
					case CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM_ACK:
						CMFileSyncEventStartFileBlockChecksumAck startChecksumAck =
								new CMFileSyncEventStartFileBlockChecksumAck(buf);
						return startChecksumAck;
					case CMFileSyncEvent.FILE_BLOCK_CHECKSUM:
						CMFileSyncEventFileBlockChecksum checksum = new CMFileSyncEventFileBlockChecksum(buf);
						return checksum;
					case CMFileSyncEvent.END_FILE_BLOCK_CHECKSUM:
						CMFileSyncEventEndFileBlockChecksum endChecksum = new CMFileSyncEventEndFileBlockChecksum(buf);
						return endChecksum;
					case CMFileSyncEvent.END_FILE_BLOCK_CHECKSUM_ACK:
						CMFileSyncEventEndFileBlockChecksumAck endChecksumAck =
								new CMFileSyncEventEndFileBlockChecksumAck(buf);
						return endChecksumAck;
					case CMFileSyncEvent.UPDATE_EXISTING_FILE:
						CMFileSyncEventUpdateExistingFile updateFile = new CMFileSyncEventUpdateExistingFile(buf);
						return updateFile;
					case CMFileSyncEvent.COMPLETE_NEW_FILE:
						CMFileSyncEventCompleteNewFile completeNewFile = new CMFileSyncEventCompleteNewFile(buf);
						return completeNewFile;
					case CMFileSyncEvent.COMPLETE_UPDATE_FILE:
						CMFileSyncEventCompleteUpdateFile completeUpdateFile =
								new CMFileSyncEventCompleteUpdateFile(buf);
						return completeUpdateFile;
					case CMFileSyncEvent.SKIP_UPDATE_FILE:
						CMFileSyncEventSkipUpdateFile skipUpdateFile = new CMFileSyncEventSkipUpdateFile(buf);
						return skipUpdateFile;
					case CMFileSyncEvent.COMPLETE_FILE_SYNC:
						CMFileSyncEventCompleteFileSync completeFileSync = new CMFileSyncEventCompleteFileSync(buf);
						return completeFileSync;
					case CMFileSyncEvent.COMPLETE_DELETE_FILES:
						CMFileSyncEventCompleteDeleteFiles completeDeleteFiles =
								new CMFileSyncEventCompleteDeleteFiles(buf);
						return completeDeleteFiles;
					case CMFileSyncEvent.ONLINE_MODE_LIST:
						CMFileSyncEventOnlineModeList onlineModeList = new CMFileSyncEventOnlineModeList(buf);
						return onlineModeList;
					case CMFileSyncEvent.ONLINE_MODE_LIST_ACK:
						CMFileSyncEventOnlineModeListAck onlineModeListAck = new CMFileSyncEventOnlineModeListAck(buf);
						return onlineModeListAck;
					case CMFileSyncEvent.END_ONLINE_MODE_LIST:
						CMFileSyncEventEndOnlineModeList endOnlineMode = new CMFileSyncEventEndOnlineModeList(buf);
						return endOnlineMode;
					case CMFileSyncEvent.END_ONLINE_MODE_LIST_ACK:
						CMFileSyncEventEndOnlineModeListAck endOnlineModeAck = new CMFileSyncEventEndOnlineModeListAck(buf);
						return endOnlineModeAck;
					case CMFileSyncEvent.LOCAL_MODE_LIST:
						CMFileSyncEventLocalModeList localModeList = new CMFileSyncEventLocalModeList(buf);
						return localModeList;
					case CMFileSyncEvent.LOCAL_MODE_LIST_ACK:
						CMFileSyncEventLocalModeListAck localModeListAck = new CMFileSyncEventLocalModeListAck(buf);
						return localModeListAck;
					case CMFileSyncEvent.END_LOCAL_MODE_LIST:
						CMFileSyncEventEndLocalModeList endLocalMode = new CMFileSyncEventEndLocalModeList(buf);
						return endLocalMode;
					case CMFileSyncEvent.END_LOCAL_MODE_LIST_ACK:
						CMFileSyncEventEndLocalModeListAck endLocalModeAck = new CMFileSyncEventEndLocalModeListAck(buf);
						return endLocalModeAck;
					default:
						System.err.println("CMEventManager.unmarshallEvent(), unknown CMFileSyncEvent id: "
								+eventID);
						return null;
				}
			default:
				System.err.println("CMEventManager.unmarshallEvent(), unknown event type: "+nEventType);
				return null;
		}
	}
	
	private static int getEventType(ByteBuffer buf)
	{
		int nType = -1;
		nType = buf.getInt(Integer.BYTES);	// position = 4
		
		return nType;
	}
	
	private static int getEventID(ByteBuffer buf)
	{
		int nID = -1;
		nID = buf.getInt(2*Integer.BYTES);	// position = 8
		return nID;
	}
	
	///////////////////////////////////////////////////////////////
	// event transmission methods
	
	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver)
	{
		return unicastEvent(cme, strReceiver, CMInfo.CM_STREAM, 0, false);
	}

	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, UUID receiverUuid) {
		return unicastEvent(cme, strReceiver, receiverUuid, CMInfo.CM_STREAM, 0, false);
	}
	
	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, int opt)
	{
		boolean bReturn = false;
		
		if(opt == CMInfo.CM_STREAM)
			bReturn = unicastEvent(cme, strReceiver, opt, 0, false);
		else if(opt == CMInfo.CM_DATAGRAM)
		{
			//search for the udp port number of the local default datagram channel
			CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
			bReturn = unicastEvent(cme, strReceiver, opt, confInfo.getUDPPort(), false);
		}
		else
		{
			System.err.println("CMEventManager.unicastEvent(), invalid option!");
			return false;
		}
		
		return bReturn;
	}

	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, UUID receiverUuid, int opt) {
		boolean bReturn = false;

		if(opt == CMInfo.CM_STREAM)
			bReturn = unicastEvent(cme, strReceiver, receiverUuid, opt, 0, false);
		else if(opt == CMInfo.CM_DATAGRAM)
		{
			//search for the udp port number of the local default datagram channel
			CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
			bReturn = unicastEvent(cme, strReceiver, receiverUuid, opt, confInfo.getUDPPort(), false);
		}
		else
		{
			System.err.println("CMEventManager.unicastEvent(), invalid option!");
			return false;
		}

		return bReturn;
	}
	
	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, int opt, int nKey)
	{
		return unicastEvent(cme, strReceiver, opt, nKey, false);
	}
	
	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, int opt, int nKey,
													boolean isBlock)
	{
		return unicastEvent(cme, strReceiver, null, opt, nKey, 0, isBlock);
	}

	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, UUID receiverUuid,
													int opt, int nKey, boolean isBlock) {
		return unicastEvent(cme, strReceiver, receiverUuid, opt, nKey, 0, isBlock);
	}
	
	// nKey: the channel key. For the stream channel, nKey is an integer greater than or equal to 0.
	// For the datagram channel, nKey is an integer that is a port number of this channel.
	// nRecvPort: if this value is 0, the default receiver port number is used.
	public synchronized static boolean unicastEvent(CMEvent cme, String strReceiver, UUID receiverUuid,
													int opt, int nKey, int nRecvPort, boolean isBlock)
	{
		CMMember loginUsers = null;
		ByteBuffer bufEvent = null;
		CMMessage msg = null;
		SocketChannel sc = null;
		DatagramChannel dc = null;
		CMUser user = null;
		List<CMUser> userList = null;
		List<SocketChannel> scList = null;
		List<InetSocketAddress> saddrList = null;
		//int nSentBytes = -1;
		CMServer tServer = null;
		CMChannelInfo<Integer> chInfo = null;
		String strTargetAddress = null;
		int nTargetPort = -1;
		CMCommInfo commInfo = CMCommInfo.getInstance();
		// get the sending queue
		CMBlockingEventQueue sendQueue = commInfo.getSendBlockingEventQueue();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();

		// Set sender and receiver information in the event header if not set
		if( cme.getSender().equals("") ) {
			String strMyName = interInfo.getMyself().getName();
			UUID myUuid = interInfo.getMyself().getUuid();
			cme.setSender(strMyName);
			cme.setSenderUuid(myUuid);
		}

		if( cme.getReceiver().equals("") ) {
			cme.setReceiver(strReceiver);
			cme.setReceiverUuid(receiverUuid);
		}

		//// find a destination channel
		
		// check if the destination is the default server or additional server
		tServer = interInfo.getDefaultServerInfo();
		if(!strReceiver.equals(tServer.getServerName()))
		{
			tServer = interInfo.findAddServer(strReceiver);
		}
		
		if(tServer != null)	// receiver = server
		{
			// target is the default server or an additional server
			if(opt == CMInfo.CM_STREAM)
			{
				if(isBlock)
					chInfo = tServer.getBlockSocketChannelInfo();
				else
					chInfo = tServer.getNonBlockSocketChannelInfo();
				sc = (SocketChannel) chInfo.findChannel(nKey);
				if( sc == null )
				{
					System.err.println("CMEventManager.unicastEvent(), channel ("+strReceiver
							+", "+nKey+") not found.");
					bufEvent = null;
					return false;
				}

			}
			else if(opt == CMInfo.CM_DATAGRAM)
			{
				strTargetAddress = tServer.getServerAddress();
				if(nRecvPort == 0)
					nTargetPort = tServer.getServerUDPPort();
				else
					nTargetPort = nRecvPort;
				if(strTargetAddress.equals("") || nTargetPort == -1)
				{
					System.err.println("CMEventManager.unicastEvent(), datagram target information unavailable, "
							+"addr("+strTargetAddress+"), udp port("+nTargetPort+").");
					return false;
				}
			}
		}	// receiver = server
		else	// receiver = client
		{
			// check if the destination is a user
			if(confInfo.getSystemType().contentEquals("SERVER"))
			{
				loginUsers = interInfo.getLoginUsers();
				if( receiverUuid == null )
					userList = loginUsers.findMemberList(strReceiver);
				else
					user = loginUsers.findMember(strReceiver, receiverUuid);
			}
			else
			{
				// For client, finding group member logic (assuming methods support UUID or return list)
				// Note: CMInteractionManager must implement these methods based on the design
				if( receiverUuid == null )
					userList = CMInteractionManager.findGroupMemberOfClient(strReceiver);
				else
					user = CMInteractionManager.findGroupMemberOfClient(strReceiver, receiverUuid);
			}

			if( (receiverUuid == null && (userList == null || userList.isEmpty()))
					|| (receiverUuid != null && user == null) )
			{
				System.err.println("CMEventManager.unicastEvent(), user (list) of target("+strReceiver
						+") with UUID("+receiverUuid+") not found.");
				return false;
			}

			// set a list of socket/datagram channel(s)
			if(opt == CMInfo.CM_STREAM)
			{
				if(isBlock) {
					if(receiverUuid == null) {
						scList = new ArrayList<>();
						for(CMUser tUser : userList) {
							SocketChannel tSc = (SocketChannel) tUser.getBlockSocketChannelInfo().findChannel(nKey);
							if(tSc != null) scList.add(tSc);
						}
					}
					else {	// receiverUuid != null
						chInfo = user.getBlockSocketChannelInfo();
						sc = (SocketChannel) chInfo.findChannel(nKey);
					}
					//chInfo = user.getBlockSocketChannelInfo();
				}
				else {	// isBlock == false
					if(receiverUuid == null) {
						scList = new ArrayList<>();
						for(CMUser tUser : userList) {
							SocketChannel tSc = (SocketChannel) tUser.getNonBlockSocketChannelInfo().findChannel(nKey);
							if(tSc != null) scList.add(tSc);
						}
					}
					else { // receiverUuid != null
						chInfo = user.getNonBlockSocketChannelInfo();
						sc = (SocketChannel) chInfo.findChannel(nKey);
					}
				}

				if( (receiverUuid != null && sc == null) || (receiverUuid == null && scList.isEmpty()) )
				{
					System.err.println("CMEventManager.unicastEvent(), receiver ("+strReceiver
							+"), uuid("+receiverUuid+"), channel key("+nKey+") not found.");
					return false;
				}
			}	// CM_STREAM
			else if(opt == CMInfo.CM_DATAGRAM) // from here
			{
				if(receiverUuid == null) {
					saddrList = new ArrayList<>();
					for(CMUser tUser : userList) {
						if(tUser.getHost().equals("") || tUser.getUDPPort() == -1) {
							System.err.println("CMEventManager.unicastEvent(), datagram target information unavailable in user list: user("
									+tUser.getName()+"), addr("+tUser.getHost()+"), udp port("+tUser.getUDPPort()+")!");
							return false;
						}

						InetSocketAddress sockAddr;
						if(nRecvPort == 0)
							sockAddr = new InetSocketAddress(tUser.getHost(), tUser.getUDPPort());
						else
							sockAddr = new InetSocketAddress(tUser.getHost(), nRecvPort);

						saddrList.add(sockAddr);
					}
				}
				else { // receiverUuid != null
					strTargetAddress = user.getHost();
					if(nRecvPort == 0)
						nTargetPort = user.getUDPPort();
					else
						nTargetPort = nRecvPort;
					if(strTargetAddress.equals("") || nTargetPort == -1)
					{
						System.err.println("CMEventManager.unicastEvent(), datagram target information unavailable, "
								+"addr("+strTargetAddress+"), udp port("+nTargetPort+").");
						return false;
					}
				}
			}	// CM_DATAGRAM
		}	// receiver = client

		sleepForSimTransDelay();

		// send the event
		// The target is either a single target (Server or specific User)
		// or multiple targets (User with multiple devices)

		// Case 1: Single Target (Server or User with specific UUID)
		if( tServer != null || receiverUuid != null ) {
			// marshall event
			bufEvent = CMEventManager.marshallEvent(cme);
			if( bufEvent == null )
			{
				System.err.println("CMEventManager.unicastEvent(), marshalling error, event(type: "
						+cme.getType()+", id: "+cme.getID()+").");
				return false;
			}

			switch(opt)
			{
				case CMInfo.CM_STREAM:
					msg = new CMMessage(bufEvent, sc);
					sendQueue.push(msg);
					break;
				case CMInfo.CM_DATAGRAM:
					if(isBlock)
					{
						dc = (DatagramChannel) commInfo.getBlockDatagramChannelInfo().findChannel(nKey);
					}
					else
					{
						dc = (DatagramChannel) commInfo.getNonBlockDatagramChannelInfo().findChannel(nKey);
					}

					if(dc == null)
					{
						System.err.println("CMEventManager.unicastEvent(), datagramChannel("+nKey+") not found.");
						return false;
					}

					InetSocketAddress sockAddr = new InetSocketAddress(strTargetAddress, nTargetPort);
					msg = new CMMessage(bufEvent, dc, sockAddr);
					sendQueue.push(msg);
					break;
				default:
					System.err.println("CMEventManager.unicastEvent(), incorrect option: "+opt);
					return false;
			}
		}
		// Case 2: Multiple Targets (User with null UUID - send to all devices)
		else {
			switch( opt ) {
				case CMInfo.CM_STREAM:
					for( SocketChannel tSc : scList ) {
						// Marshall event for each transmission to ensure thread safety of ByteBuffer
						bufEvent = CMEventManager.marshallEvent(cme);
						if( bufEvent == null ) {
							System.err.println("CMEventManager.unicastEvent(), marshalling error, event(type: "+cme.getType()+", id: "+cme.getID()+")!");
							return false;
						}
						msg = new CMMessage(bufEvent, tSc);
						sendQueue.push(msg);
					}
					break;
				case CMInfo.CM_DATAGRAM:
					if( isBlock )
						dc = (DatagramChannel) commInfo.getBlockDatagramChannelInfo().findChannel(nKey);
					else
						dc = (DatagramChannel) commInfo.getNonBlockDatagramChannelInfo().findChannel(nKey);

					if( dc == null ) {
						System.err.println("CMEventManager.unicastEvent(), datagramChannel("+nKey+") not found!");
						return false;
					}

					for(InetSocketAddress sockAddr : saddrList) {
						bufEvent = CMEventManager.marshallEvent(cme);
						if( bufEvent == null ) {
							System.err.println("CMEventManager.unicastEvent(), marshalling error, evnt(type: "+cme.getType()+", id: "+cme.getID()+")!");
							return false;
						}
						msg = new CMMessage(bufEvent, dc, sockAddr);
						sendQueue.push(msg);
					}
					break;
				default:
					System.err.println("CMEventManager.unicastEvent(), incorrect option: "+opt);
					return false;
			}
		}	// case 2: multiple targets

		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMEventManager.unicastEvent(), puts event to the sending queue,"
					+" event(type: "+cme.getType()+", id: "+cme.getID()+").");
			System.out.println("receiver("+strReceiver+"), receiver uuid("+receiverUuid+"), opt("
					+opt+"), ch key("+nKey+"), isBlock("+isBlock+").");
		}

		return true;
	}
	
	public synchronized static boolean unicastEvent(CMEvent cme, SocketChannel sc)
	{
		//int nSentBytes = -1;
		CMMessage msg = null;
		CMCommInfo commInfo = CMCommInfo.getInstance();
		CMBlockingEventQueue sendQueue = commInfo.getSendBlockingEventQueue();

		// [Mod] Get CMInteractionInfo instance to access 'myself' object
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

		// [Mod] Set sender and sender UUID if the sender field is empty (initial value)
		// According to Design Doc 06 (lines 16-19, 250-251)
		if(cme.getSender().equals(""))
		{
			cme.setSender(interInfo.getMyself().getName());
			cme.setSenderUuid(interInfo.getMyself().getUuid());
		}
		// [Mod] Note: Receiver is not set here (n/a) as per Design Doc 06 (line 19),
		// because this method sends directly to a specific SocketChannel.

		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.unicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		msg = new CMMessage(bufEvent, sc);
		sendQueue.push(msg);
		//nSentBytes = CMCommManager.sendMessage(bufEvent, sc);
		//bufEvent = null;
		
		if(CMInfo._CM_DEBUG_2)
		{
			//System.out.println("CMEventManager.unicastEvent(), sent "+nSentBytes+" bytes, with"
			//		+sc.toString());
			System.out.println("CMEventManager.unicastEvent(), puts event to the sending queue.");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public synchronized static boolean multicastEvent(CMEvent cme, String strSessionName, String strGroupName)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMCommInfo commInfo = CMCommInfo.getInstance();
		CMBlockingEventQueue sendQueue = commInfo.getSendBlockingEventQueue();

		// Set the sender and sender UUID if they are not set (initial state)
		if(cme.getSender().equals(""))
		{
			String strMyName = interInfo.getMyself().getName();
			UUID myUuid = interInfo.getMyself().getUuid();
			cme.setSender(strMyName);
			cme.setSenderUuid(myUuid);
		}

		CMSession session = interInfo.findSession(strSessionName);
		CMGroup group = null;
		InetSocketAddress sockAddress = null;
		DatagramChannel dc = null;
		ByteBuffer bufEvent = null;
		CMMessage msg = null;
		//int nSentBytes = -1;
		
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
		
		sockAddress = new InetSocketAddress(group.getGroupAddress(), group.getGroupPort());
		dc = (DatagramChannel) group.getMulticastChannelInfo().findChannel(sockAddress);
		
		if(dc == null)
		{
			System.err.println("CMEventManager.multicastEvent(), channel("+sockAddress.toString()+") not found.");
			return false;
		}

		sleepForSimTransDelay();

		bufEvent = CMEventManager.marshallEvent(cme);
		
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.multicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		msg = new CMMessage(bufEvent, dc, sockAddress);
		sendQueue.push(msg);
		//nSentBytes = CMCommManager.sendMessage(bufEvent, dc, group.getGroupAddress(), group.getGroupPort());
		//bufEvent = null;
		
		if(CMInfo._CM_DEBUG_2)
		{
			//System.out.println("CMEventManager.multicastEvent(), sent "+nSentBytes+" bytes, with"
			//		+dc.toString()+" session("+strSessionName+"), group("+strGroupName+"), channel("
			//		+sockAddress.toString()+").");
			System.out.println("CMEventManager.multicastEvent(), puts the event to the sending queue, with"
					+dc.toString()+" session("+strSessionName+"), group("+strGroupName+"), channel("
					+sockAddress.toString()+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public synchronized static boolean multicastEvent(CMEvent cme, DatagramChannel dc, String strMA, int nPort)
	{
		//int nSentBytes = -1;
		CMCommInfo commInfo = CMCommInfo.getInstance();
		CMBlockingEventQueue sendQueue = commInfo.getSendBlockingEventQueue();
		CMMessage msg = null;
		
		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.multicastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}
		
		InetSocketAddress sockAddr = new InetSocketAddress(strMA, nPort);
		msg = new CMMessage(bufEvent, dc, sockAddr);
		sendQueue.push(msg);
		//nSentBytes = CMCommManager.sendMessage(bufEvent, dc, strMA, nPort);
		//bufEvent = null;
		
		if(CMInfo._CM_DEBUG_2)
		{
			//System.out.println("CMEventManager.multicastEvent(), sent "+nSentBytes+" bytes, with"
			//		+dc.toString()+", addr("+strMA+"), port("+nPort+").");
			System.out.println("CMEventManager.multicastEvent(), puts the event to the sending queue, with"
					+dc.toString()+", addr("+strMA+"), port("+nPort+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public synchronized static boolean broadcastEvent(CMEvent cme)
	{
		return broadcastEvent(cme, CMInfo.CM_STREAM, 0);
	}
	
	public synchronized static boolean broadcastEvent(CMEvent cme, int opt)
	{
		return broadcastEvent(cme, opt, 0);
	}
	
	// send an event to all login users (server)
	public synchronized static boolean broadcastEvent(CMEvent cme, int opt, int nChNum)
	{
		CMCommInfo commInfo = CMCommInfo.getInstance();
		CMBlockingEventQueue sendQueue = commInfo.getSendBlockingEventQueue();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMMessage msg = null;

		// [Added] Set the sender and sender UUID if they are not set (initial state)
		if(cme.getSender().equals(""))
		{
			String strMyName = interInfo.getMyself().getName();
			UUID myUuid = interInfo.getMyself().getUuid();
			cme.setSender(strMyName);
			cme.setSenderUuid(myUuid);
		}

		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.boradcastEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}

		// [Modified] Structure of CMMember changed to Hashtable<String, List<CMUser>>.
		// Previous iterator logic is replaced with nested for-loops to traverse all devices of all users.
		CMMember loginUsers = interInfo.getLoginUsers();

		switch(opt)
		{
		case CMInfo.CM_STREAM:
			// [Modified] Iterate over all values(user lists) in the hashtable
			for(List<CMUser> userList : loginUsers.getAllMembers().values())
			{
				// [Modified] Iterate over each device(CMUser) in the list
				for(CMUser tuser : userList)
				{
					SocketChannel sc = (SocketChannel) tuser.getNonBlockSocketChannelInfo().findChannel(nChNum);
					if( sc == null )
					{
						System.err.println("CMEventManager.broadcastEvent(), SocketChannel of user("
								+tuser.getName()+"), uuid("+tuser.getUuid()+") not found.");
						continue;
					}
					if( !sc.isOpen() )
					{
						System.err.println("CMEventManager.broadcastEvent(), SocketChannel of user("
								+tuser.getName()+"), uuid("+tuser.getUuid()+") is closed.");
						continue;
					}

					sleepForSimTransDelay();

					msg = new CMMessage(bufEvent, sc);
					sendQueue.push(msg);
					//CMCommManager.sendMessage(bufEvent, sc);
				}
			}
			break;
		case CMInfo.CM_DATAGRAM:
			CMChannelInfo<Integer> dcInfo = CMCommInfo.getInstance().getNonBlockDatagramChannelInfo();
			DatagramChannel dc = (DatagramChannel) dcInfo.findChannel(nChNum);
			if(dc == null)
			{
				System.err.println("CMEventManager.broadcastEvent(), DatagramChannel("+nChNum
						+") not found.");
				return false;
			}
			// [Modified] Iterate over all values(user lists) in the hashtable
			for(List<CMUser> userList : loginUsers.getAllMembers().values())
			{
				// [Modified] Iterate over each device(CMUser) in the list
				for(CMUser tuser : userList)
				{
					sleepForSimTransDelay();

					InetSocketAddress sockAddr = new InetSocketAddress(tuser.getHost(), tuser.getUDPPort());
					msg = new CMMessage(bufEvent, dc, sockAddr);
					sendQueue.push(msg);
					//CMCommManager.sendMessage(bufEvent, dc, tuser.getHost(), tuser.getUDPPort());
				}
			}
			break;
		default:
			System.err.println("CMEventManager.broadcastEvent(), incorrect option: "+opt);
			return false;
		}

		if(CMInfo._CM_DEBUG_2)
		{
			int nUserNum = CMInteractionInfo.getInstance().getLoginUsers().getMemberNum();
			System.out.println("CMEventManager.broadcastEvent(), succeeded to ("+nUserNum
					+") users: opt("+opt+"), ch#("+nChNum+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	public synchronized static boolean castEvent(CMEvent cme, CMMember users)
	{
		return castEvent(cme, users, CMInfo.CM_STREAM, 0);
	}
	
	public synchronized static boolean castEvent(CMEvent cme, CMMember users, int opt)
	{
		return castEvent(cme, users, opt, 0);
	}
	
	// send an event to a specific user group with multiple unicast transmissions
	public synchronized static boolean castEvent(CMEvent cme, CMMember users, int opt, int nChNum)
	{
		CMCommInfo commInfo = CMCommInfo.getInstance();
		CMBlockingEventQueue sendQueue = commInfo.getSendBlockingEventQueue();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMMessage msg = null;

		// [Added] Set the sender and sender UUID if they are not set (initial state)
		if(cme.getSender().equals(""))
		{
			String strMyName = interInfo.getMyself().getName();
			UUID myUuid = interInfo.getMyself().getUuid();
			cme.setSender(strMyName);
			cme.setSenderUuid(myUuid);
		}

		ByteBuffer bufEvent = CMEventManager.marshallEvent(cme);
		if(bufEvent == null)
		{
			System.err.println("CMEventManager.castEvent(), marshalling error, event(type: "
					+cme.getType()+", id: "+cme.getID()+").");
			return false;
		}

		// [Modified] Structure of CMMember changed to Hashtable<String, List<CMUser>>.
		// Previous iterator logic is replaced with nested for-loops to traverse all devices of all users.
		switch(opt)
		{
		case CMInfo.CM_STREAM:
			// [Modified] Iterate over all values(user lists) in the hashtable
			for(List<CMUser> userList : users.getAllMembers().values())
			{
				// [Modified] Iterate over each device(CMUser) in the list
				for(CMUser tuser : userList)
				{
					SocketChannel sc = (SocketChannel) tuser.getNonBlockSocketChannelInfo().findChannel(nChNum);
					if( sc == null )
					{
						System.err.println("CMEventManager.castEvent(), SocketChannel of user("
								+tuser.getName()+"), uuid("+tuser.getUuid()+") not found.");
						continue;
					}
					if( !sc.isOpen() )
					{
						System.err.println("CMEventManager.castEvent(), SocketChannel of user("
								+tuser.getName()+"), uuid("+tuser.getUuid()+") is closed.");
						continue;
					}

					sleepForSimTransDelay();

					msg = new CMMessage(bufEvent, sc);
					sendQueue.push(msg);
					//CMCommManager.sendMessage(bufEvent, sc);
				}
			}
			break;
		case CMInfo.CM_DATAGRAM:
			CMChannelInfo<Integer> dcInfo = CMCommInfo.getInstance().getNonBlockDatagramChannelInfo();
			DatagramChannel dc = (DatagramChannel) dcInfo.findChannel(nChNum);
			if(dc == null)
			{
				System.err.println("CMEventManager.castEvent(), DatagramChannel("+nChNum
						+") not found.");
				return false;
			}
			// [Modified] Iterate over all values(user lists) in the hashtable
			for(List<CMUser> userList : users.getAllMembers().values())
			{
				// [Modified] Iterate over each device(CMUser) in the list
				for(CMUser tuser : userList)
				{
					sleepForSimTransDelay();

					InetSocketAddress sockAddr = new InetSocketAddress(tuser.getHost(), tuser.getUDPPort());
					msg = new CMMessage(bufEvent, dc, sockAddr);
					sendQueue.push(msg);
					//CMCommManager.sendMessage(bufEvent, dc, tuser.getHost(), tuser.getUDPPort());
				}
			}
			break;
		default:
			System.err.println("CMEventManager.castEvent(), incorrect option: "+opt);
			return false;
		}

		if(CMInfo._CM_DEBUG_2)
		{
			int nUserNum = users.getMemberNum();
			System.out.println("CMEventManager.castEvent(), succeeded to ("+nUserNum+") users: opt("
								+opt+"), ch#("+nChNum+").");
			System.out.println("event(type: "+cme.getType()+", id: "+cme.getID()+").");
		}
		
		return true;
	}
	
	//////////////////////////////////////
	// add some sleep in order to simulate transmission delay

	private static void sleepForSimTransDelay()
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		int nSimTransDelay = confInfo.getSimTransDelay();

		if(nSimTransDelay > 0)
		{
			try {
				Thread.sleep(nSimTransDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}

		return;
	}
}
