package kr.ac.konkuk.ccslab.cm.manager;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.PrimitiveIterator.OfDouble;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttSession;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttTopicQoS;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttWill;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventDISCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventSUBSCRIBE;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventUNSUBSCRIBE;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMMqttInfo;

/**
 * The CMMqttManager class represents a CM service object with which an application can 
 * request Mqtt service.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttManager extends CMServiceManager {

	public CMMqttManager(CMInfo cmInfo)
	{
		super(cmInfo);
		m_nType = CMInfo.CM_MQTT_MANAGER;
	}
	
	public boolean connect()
	{
		// client -> server
		boolean bRet = false;
		bRet = connect(null, null, false, (byte)0, false, false, 0);
		return bRet;
	}
	
	public boolean connect(String strWillTopic, String strWillMessage, boolean bWillRetain,
			byte willQoS, boolean bWillFlag, boolean bCleanSession, int nKeepAlive)
	{
		// client -> server
		// check if the client has logged in to the default server.
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.connect(), the system type is SERVER!");
			return false;
		}
		
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		int nState = myself.getState();
		if(nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.err.println("CMMqttManager.connect(), you must log in to the default "
					+ "server!");
			return false;
		}
		
		// make CONNECT event
		CMMqttEventCONNECT conEvent = new CMMqttEventCONNECT();
		// set CM event header
		conEvent.setSender(myself.getName());
		// set variable header
		conEvent.setUserNameFlag(true);	// to use CM user name
		conEvent.setPasswordFlag(true);	// to use CM user password
		conEvent.setWillRetainFlag(bWillRetain);
		conEvent.setWillQoS(willQoS);
		conEvent.setWillFlag(bWillFlag);
		conEvent.setCleanSessionFlag(bCleanSession);
		conEvent.setKeepAlive(nKeepAlive);
		// set payload
		conEvent.setClientID(myself.getName());	// = CM user name
		conEvent.setWillTopic(strWillTopic);
		conEvent.setWillMessage(strWillMessage);
		conEvent.setUserName(myself.getName());
		conEvent.setPassword(myself.getPasswd());
		
		// process the clean-session flag
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttSession mqttSession = mqttInfo.getMqttSession();
		if(mqttSession == null)
		{
			mqttSession = new CMMqttSession();
			mqttInfo.setMqttSession(mqttSession);
		}
		
		// store will message at the client session
		if(conEvent.isWillFlag())
		{
			CMMqttWill will = new CMMqttWill();
			will.setWillMessage(conEvent.getWillMessage());
			will.setWillTopic(conEvent.getWillTopic());
			will.setWillQoS(conEvent.getWillQoS());
			will.setWillRetain(conEvent.isWillRetainFlag());
			mqttSession.setMqttWill(will);
		}
		
		// send CONNECT event
		boolean bRet = false;
		bRet = CMEventManager.unicastEvent(conEvent, "SERVER", m_cmInfo);
		
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttManager.connect(), sent "+conEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttManager.connect(), error to send "+conEvent.toString());
			return false;
		}
		
		return bRet;
	}
	
	public boolean publish(String strTopic, String strMsg)
	{
		// client -> server or server -> client
		boolean bRet = false;
		bRet = publish(-1, strTopic, strMsg, (byte)0, false, false);
		return bRet;
	}
	
	public boolean publish(int nPacketID, String strTopic, String strMsg, byte qos)
	{
		// client -> server or server -> client
		boolean bRet = false;
		bRet = publish(nPacketID, strTopic, strMsg, qos, false, false);
		return bRet;
	}
	
	public boolean publish(int nPacketID, String strTopic, String strMsg, byte qos, 
				boolean bDupFlag, boolean bRetainFlag)
	{
		// client -> server or server -> client
		boolean bRet = false;
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		String strSysType = confInfo.getSystemType();
		if(strSysType.equals("SERVER"))
			bRet = publishFromServer(nPacketID, strTopic, strMsg, qos, bDupFlag, bRetainFlag);
		else
			bRet = publishFromClient(nPacketID, strTopic, strMsg, qos, bDupFlag, bRetainFlag);
		return bRet;
	}
	
	private boolean publishFromClient(int nPacketID, String strTopic, String strMsg, 
			byte qos, boolean bDupFlag, boolean bRetainFlag)
	{
		// client -> server
		boolean bRet = false;
		
		// check whether the client is a log-in user.
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		if(myself.getState() == CMInfo.CM_INIT || myself.getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMMqttManager.publishFromClient(): "
					+ "you must log in to the default server!");
			return false;
		}
		
		// make PUBLISH event
		CMMqttEventPUBLISH pubEvent = new CMMqttEventPUBLISH();
		// set sender (in CM event header)
		pubEvent.setSender(myself.getName());
		// set fixed header
		pubEvent.setDupFlag(bDupFlag);
		pubEvent.setQoS(qos);
		pubEvent.setRetainFlag(bRetainFlag);
		// set variable header
		pubEvent.setTopicName(strTopic);
		pubEvent.setPacketID(nPacketID);
		// set payload
		pubEvent.setAppMessage(strMsg);
		
		// send PUBLISH event
		String strReceiver = "SERVER";
		bRet = CMEventManager.unicastEvent(pubEvent,strReceiver, m_cmInfo);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttManager.publishFromClient(), sent "
					+pubEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttManager.publishFromClient(), error to send "
					+pubEvent.toString());
			return false;
		}
		
		// process QoS 1 or 2 case for the sent packet
		if( qos == 1 || qos == 2 )
		{
			// to get session information
			CMMqttSession session = null;
			CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
			session = mqttInfo.getMqttSession();
			
			if(session == null)
			{
				System.err.println("CMMqttManager.publishFromClient(): QoS("
						+qos+"), but session is null!");
				System.err.println("To create a new session, execute MQTT connect.");
				return false;
			}
			
			// add the sent event to the sent-unack-publish-list
			bRet = session.addSentUnAckPublish(pubEvent);
			if(bRet && CMInfo._CM_DEBUG)
			{
				System.out.println("CMMqttManager.publishFromClient(): "
						+ "stored to sent unack publish list: "+pubEvent.toString());
			}
			if(!bRet)
			{
				System.err.println("CMMqttManager.publishFromClient(): "
						+ "error to store to sent unack publish list: "+pubEvent.toString());
				return false;
			}
		}
		
		return bRet;
	}
	
	private boolean publishFromServer(int nPacketID, String strTopic, String strMsg, 
			byte qos, boolean bDupFlag, boolean bRetainFlag)
	{
		// server -> client

		// make PUBLISH event
		CMMqttEventPUBLISH pubEvent = new CMMqttEventPUBLISH();
		// set sender (in CM event header)
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		pubEvent.setSender(myself.getName());
		// set fixed header
		pubEvent.setDupFlag(bDupFlag);
		pubEvent.setQoS(qos);
		pubEvent.setRetainFlag(bRetainFlag);
		// set variable header
		pubEvent.setTopicName(strTopic);
		pubEvent.setPacketID(nPacketID);
		// set payload
		pubEvent.setAppMessage(strMsg);
		
		// find subscribers and send the PUBLISH event
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		Hashtable<String, CMMqttSession> sessionHashtable =	mqttInfo.getMqttSessionHashtable();
		Set<String> keys = sessionHashtable.keySet();
		for(String key : keys)
		{
			CMMqttSession session = sessionHashtable.get(key);
			CMList<CMMqttTopicQoS> subList = session.getSubscriptionList();
			publishFromServerToOneClient(pubEvent, key, subList);
		}
	
		return true;
	}
	
	// publish event from server to one client
	public boolean publishFromServerToOneClient(CMMqttEventPUBLISH pubEvent, String strClient, 
			CMList<CMMqttTopicQoS> subscriptionList)
	{
		if(subscriptionList == null)
		{
			System.err.println("CMMqttManager.publishFromServerToOneClient(), subscription list of "
					+"client ("+strClient+") is null!");
			return false;
		}
		
		Vector<CMMqttTopicQoS> filterVector = subscriptionList.getList();
		String strTopic = pubEvent.getTopicName();
		
		boolean bFound = false;
		byte maxQoS = 0;
		for(int i = 0; i < filterVector.size(); i++)
		{
			String strFilter = filterVector.elementAt(i).getTopic();
			bFound = isTopicMatch(strTopic, strFilter);
			if(bFound)	// find maximum matching QoS
			{
				byte reqQoS = filterVector.elementAt(i).getQoS();
				if(reqQoS > maxQoS)
					maxQoS = reqQoS;
			}
		}  // end for

		if(!bFound)
		{
			return false;
		}
		
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		Hashtable<String, CMMqttSession> sessionHashtable = mqttInfo.getMqttSessionHashtable(); 
		CMMqttSession session = sessionHashtable.get(strClient);
		byte qos = pubEvent.getQoS();
		int nPacketID = pubEvent.getPacketID();
		boolean bRet = false;

		// send and clear all pending events of this client (key)
		/*
		if(!sendAndClearPendingTransEvents(strClient, session))
			return false;
		*/
		
		// adapt to the subscribed QoS
		int sentQoS = qos;
		if(maxQoS < qos)
		{
			sentQoS = maxQoS;
			pubEvent.setQoS(maxQoS);
		}
			
		// check whether the same packet ID is in use or not
		if( (sentQoS == 1 || sentQoS == 2) && 
				(session.findSentUnAckPublish(nPacketID) != null) )
		{
			System.err.println("CMMqttManager.publishFromServerToOneClient(), "
					+ "client ("+strClient+"), "
					+ "packet ID ("+nPacketID
					+") is already in use in sent-unack-publish list !");
			bRet = session.addPendingTransPublish(pubEvent);
			if(bRet && CMInfo._CM_DEBUG)
			{
				System.out.println("CMMqttManager.publishFromServerToOneClient(), "
						+ "event ("+nPacketID+") is added to the transmission-pending-publish list.");
			}
			return false;
		}
			
		bRet = CMEventManager.unicastEvent(pubEvent, strClient, m_cmInfo);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttManager.publishFromServerToOneClient(), sent ("
					+strClient+"): "+pubEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttManager.publishFromServerToOneClient(), error to send ("
					+strClient+"): ");
			if(sentQoS == 1 || sentQoS == 2)
			{
				// add this event to the pending event list
				session.addPendingTransPublish(pubEvent);
			}
			return false;
		}
			
		// process QoS 1 or 2 case for the sent packet
		if(bRet && (sentQoS == 1 || sentQoS == 2))
		{
			bRet = session.addSentUnAckPublish(pubEvent);
			if(bRet && CMInfo._CM_DEBUG)
			{
				System.out.println("CMMqttManager.publishFromServerToOneClient(): "
						+ "stored to sent unack publish list of client ("+strClient+") : "
						+pubEvent.toString());
			}
			if(!bRet)
			{
				System.err.println("CMMqttManager.publishFromServer(): error "
						+ "to store to sent unack event publish of client ("+strClient+") : "
						+pubEvent.toString());
				return false;
			}
		} // end if

		return true;
	}
	
	// send and clear all transmission-pending events (QoS 1 or 2)
	public boolean sendAndClearPendingTransPublish(String strReceiver, CMMqttSession session)
	{
		// server -> client
		if(session == null)
		{
			System.err.println("CMMqttManager.sendAndClearPendingTransEvents(), receiver ("
					+strReceiver+"), session is null!");
			return true;
		}
		
		if(session.getPendingTransPublishList().isEmpty())
		{
			System.out.println("CMMqttManager.sendAndClearPendingTransEvents(), the list is empty "
					+"for receiver ("+strReceiver+")");
			return true;
		}

		boolean bRet = false;
		for(CMMqttEvent pendingEvent : session.getPendingTransPublishList().getList())
		{
			bRet = CMEventManager.unicastEvent(pendingEvent, strReceiver, m_cmInfo);
			if(!bRet)
			{
				System.err.println("CMMqttManager.sendAndClearPendingTransEvents(), error: receiver ("
						+strReceiver+"), "+pendingEvent.toString());
				return false;
			}
		}
		session.removeAllPendingTransPublish();

		return true;
	}
	
	// send and clear all sent-unack publish events (QoS 1 or 2)
	public boolean resendSentUnAckPublish(String strReceiver, CMMqttSession session)
	{
		// client -> server or server -> client
		if(session == null)
		{
			System.err.println("CMMqttManager.sendAndClearSentUnAckPublish(), receiver ("
					+strReceiver+"), session is null!");
			return true;
		}
		
		if(session.getSentUnAckPublishList().isEmpty())
		{
			System.out.println("CMMqttManager.sendAndClearSentUnAckPublish(), the list is "
					+ "empty for receiver ("+strReceiver+")");
			return true;
		}
		
		boolean bRet = false;
		for(CMMqttEventPUBLISH unackEvent : session.getSentUnAckPublishList().getList())
		{
			// set DUP flag
			unackEvent.setDupFlag(true);
			
			bRet = CMEventManager.unicastEvent(unackEvent, strReceiver, m_cmInfo);
			if(!bRet)
			{
				System.err.println("CMMqttManager.resendSentUnAckPublish(), error: "
						+ "receiver ("+strReceiver+"), "+unackEvent.toString());
				// do not need to put the event to the pending list because it still exists 
				// in the sent-unack-publish list.
			}
		}
		
		return true;
	}
	
	public static boolean isTopicMatch(String strTopic, String strFilter)
	{
		strTopic = strTopic.trim();
		strFilter = strFilter.trim();
		
		// get tokens
		String[] topicTokens = strTopic.split("/");
		String[] filterTokens = strFilter.split("/");
		
		int i = 0;
		for(i = 0; i < filterTokens.length; i++)
		{
			String filterToken = filterTokens[i];
			if(filterToken.equals("#") && i == filterTokens.length - 1)
				return true;
			
			// # of topic level is less than # of filter level
			if( i == topicTokens.length )
				return false;
			
			String topicToken = topicTokens[i];
			if(filterToken.equals(topicToken) || filterToken.equals("+"))
				continue;
			else
				return false;
		}
		
		// # of topic level is greater than # of topic filter
		if( i < topicTokens.length )
			return false;
		
		return true;
	}
	
	public boolean subscribe(String strTopicFilter, byte qos)
	{
		boolean bRet = subscribe(-1, strTopicFilter, qos); 
		return bRet;
	}
	
	public boolean subscribe(int nPacketID, String strTopicFilter, byte qos)
	{
		CMMqttTopicQoS topicQoS = new CMMqttTopicQoS(strTopicFilter, qos);
		CMList<CMMqttTopicQoS> topicQoSList = new CMList<CMMqttTopicQoS>();
		topicQoSList.addElement(topicQoS);
		boolean bRet = subscribe(nPacketID, topicQoSList);
		
		return bRet;
	}
	
	public boolean subscribe(int nPacketID, CMList<CMMqttTopicQoS> topicQoSList)
	{
		// to check the CM system type
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.subscribe(), the system type is SERVER!");
			return false;
		}
		
		// to check if the user completes to log in to the default server, or not
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		int nState = myself.getState();
		if(nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.err.println("CMMqttManager.subscribe(), you must log in to the "
					+"default server!");
			return false;
		}
		
		// check the topic/qos pair list
		if(topicQoSList.isEmpty())
		{
			System.err.println("CMMqttManager.subscribe(), the topic/QoS pair list "
					+"is empty!");
			return false;
		}
		
		// make and send a SUBSCRIBE event
		CMMqttEventSUBSCRIBE subEvent = new CMMqttEventSUBSCRIBE();
		// set sender (in CM event header)
		subEvent.setSender(myself.getName());
		// set fixed header in the SUBSCRIBE constructor
		// set variable header
		subEvent.setPacketID(nPacketID);
		// set payload
		subEvent.setTopicQoSList(topicQoSList);
		
		// temporarily store the requested topic/qos list at the client session
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttSession session = mqttInfo.getMqttSession();
		if(session == null)
		{
			System.err.println("CMMqttManager.subscribe(), the client session is null!");
			System.err.println("To create a new session, execute MQTT connect.");
			return false;
		}
		session.setReqSubscriptionList(topicQoSList);
		
		boolean bRet = false;
		bRet = CMEventManager.unicastEvent(subEvent, "SERVER", m_cmInfo);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttManager.subscribe(), sent "+subEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttManager.subscribe(), error to send "+subEvent.toString());
			return false;
		}
		
		return bRet;
	}
	
	public boolean unsubscribe(String strTopic)
	{
		boolean bRet = unsubscribe(-1, strTopic);
		return bRet;
	}
	
	public boolean unsubscribe(int nPacketID, String strTopic)
	{
		boolean bRet = false;
		CMList<String> topicList = new CMList<String>();
		topicList.addElement(strTopic);
		bRet = unsubscribe(nPacketID, topicList);
		return bRet;
	}
	
	public boolean unsubscribe(int nPacketID, CMList<String> topicList)
	{
		// to check the CM system type
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.unsubscribe(), the system type is SERVER!");
			return false;
		}
		
		// to check if the user completes to log in to the default server, or not
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		int nState = myself.getState();
		if(nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.err.println("CMMqttManager.unsubscribe(), you must log in to the "
					+"default server!");
			return false;
		}

		// check the topic list
		if(topicList.isEmpty())
		{
			System.err.println("CMMqttManager.unsubscribe(), the topic list is empty!");
			return false;
		}
		
		// remove the local topic filters matched with the requested topics
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttSession session = mqttInfo.getMqttSession();
		if(session == null)
		{
			System.err.println("CMMqttManager.unsubscribe(), the client session is null!");
			System.err.println("To create a new session, execute MQTT connect.");
			return false;
		}
		CMList<CMMqttTopicQoS> subList = session.getSubscriptionList();
		if(subList == null || subList.isEmpty())
		{
			System.err.println("CMMqttManager.unsubscribe(), the subscription list of "
					+"client session is null or empty!");
			return false;
		}
		for(String topic : topicList.getList())
		{
			CMMqttTopicQoS topicQoS = new CMMqttTopicQoS();
			topicQoS.setTopic(topic);
			subList.removeElement(topicQoS);
		}

		// make and send an UNSUBSCRIBE event
		CMMqttEventUNSUBSCRIBE unsubEvent = new CMMqttEventUNSUBSCRIBE();
		// set sender (in CM event header)
		unsubEvent.setSender(myself.getName());
		// set fixed header in the UNSUBSCRIBE constructor
		// set variable header
		unsubEvent.setPacketID(nPacketID);
		// set payload
		unsubEvent.setTopicList(topicList);
		
		boolean bRet = false;
		bRet = CMEventManager.unicastEvent(unsubEvent, "SERVER", m_cmInfo);

		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttManager.unsubscribe(), sent "+unsubEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttManager.unsubscribe(), error to send "+unsubEvent
					.toString());
			return false;
		}

		return bRet;
	}
	
	public boolean requestPing()
	{
		// not yet
		System.err.println("CMMqttManager.requestPing(), not implemented yet!");
		return false;
	}
	
	public boolean disconnect()
	{
		// to check the CM system type
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.disconnect(), the system type is SERVER!");
			return false;
		}
		
		// to check if the user completes to log in to the default server, or not
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		int nState = myself.getState();
		if(nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.err.println("CMMqttManager.disconnect(), you must log in to the "
					+"default server!");
			return false;
		}

		// get the client session
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttSession session = mqttInfo.getMqttSession();
		if(session == null)
		{
			System.err.println("CMMqttManager.disconnect(), the client session is null!");
			return false;
		}

		// remove will info at the client session
		if(session.getMqttWill() != null)
			session.setMqttWill(null);
		
		// make and send DISCONNECT event
		CMMqttEventDISCONNECT disconEvent = new CMMqttEventDISCONNECT();
		// set sender (in CM event header)
		disconEvent.setSender(myself.getName());
		// set fixed header in DISCONNECT constructor
		
		boolean bRet = false;
		bRet = CMEventManager.unicastEvent(disconEvent, "SERVER", m_cmInfo);
		
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttManager.disconnect(), sent "+disconEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttManager.disconnect(), error to send "
					+disconEvent.toString());
			return false;
		}

		return bRet;
	}
	
	// get MQTT session information (4 client)
	public String getMySessionInfo()
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.getMySessionInfo(), the system type is SERVER!");
			return null;
		}
		
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttSession session = mqttInfo.getMqttSession();
		if(session == null)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMMqttManager.getMySessionInfo(), session is null.");
			return "MQTT session is null.";
		}
		
		return session.toString();
	}
	
	// get MQTT session information (4 server)
	public String getSessionInfo(String strUserName)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(!confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.getSessionInfo(), the system type is not SERVER!");
			return null;
		}
		
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttSession session = mqttInfo.getMqttSessionHashtable().get(strUserName);
		if(session == null)
		{
			if(CMInfo._CM_DEBUG)
				System.err.println("CMMqttManager.getSessionInfo(), session of user("
						+strUserName+") not found!");
			String strReturn = "session of user \""+strUserName+"\" not found!";
			return strReturn;
		}
		
		return session.toString();
	}
	
	// get all MQTT session information (4 server)
	public String getAllSessionInfo()
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(!confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.getAllSessionInfo(), the system type is not SERVER!");
			return null;
		}
		
		StringBuffer strBuf = new StringBuffer();
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		Hashtable<String, CMMqttSession> sessionHashtable = mqttInfo.getMqttSessionHashtable();
		strBuf.append("# All MQTT session list: "+sessionHashtable.size()+"\n");
		for(String strUser : sessionHashtable.keySet())
		{
			strBuf.append("session of user: "+strUser+"\n");
			strBuf.append(getSessionInfo(strUser)+"\n");
		}
		
		return strBuf.toString();
	}
	
	// get all MQTT retain information (4 server)
	public String getAllRetainInfo()
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(!confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.getAllRetainInfo(), the system type is not SERVER!");
			return null;
		}
		
		StringBuffer strBuf = new StringBuffer();
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		Hashtable<String, CMMqttEventPUBLISH> retainHashtable = mqttInfo.getMqttRetainHashtable();
		strBuf.append("# All MQTT retained events: "+retainHashtable.size()+"\n");
		for(String strTopic : retainHashtable.keySet())
		{
			strBuf.append("topic : "+strTopic+"\n");
			strBuf.append("event : "+retainHashtable.get(strTopic).toString()+"\n");
		}
		
		return strBuf.toString();
	}
}
