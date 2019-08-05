package kr.ac.konkuk.ccslab.cm.manager;

import java.util.Hashtable;
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
		
		return bRet;
	}
	
	public boolean publish(String strTopic, String strMsg)
	{
		// client -> server
		boolean bRet = false;
		bRet = publish("SERVER", -1, strTopic, strMsg, (byte)0, false, false);
		return bRet;
	}
	
	public boolean publish(String strReceiver, String strTopic, String strMsg)
	{
		// client -> server or server -> client
		boolean bRet = false;
		bRet = publish(strReceiver, -1, strTopic, strMsg, (byte)0, false, false);
		return bRet;
	}
	
	public boolean publish(int nPacketID, String strTopic, String strMsg, byte qos)
	{
		// client -> server
		boolean bRet = false;
		bRet = publish("SERVER", nPacketID, strTopic, strMsg, qos, false, false);
		return bRet;
	}
	
	public boolean publish(String strReceiver, int nPacketID, String strTopic, String strMsg, byte qos)
	{
		// client -> server or server -> client
		boolean bRet = false;
		bRet = publish(strReceiver, nPacketID, strTopic, strMsg, qos, false, false);
		return bRet;
	}
	
	public boolean publish(String strReceiver, int nPacketID, String strTopic,
			String strMsg, byte qos, boolean bDupFlag, boolean bRetainFlag)
	{
		// client -> server or server -> client
		boolean bRet = false;
		
		// check whether the client is a log-in user.
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		String strSysType = confInfo.getSystemType();
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		if(strSysType.equals("CLIENT"))
		{
			if(myself.getState() == CMInfo.CM_INIT || myself.getState() == CMInfo.CM_CONNECT)
			{
				System.err.println("CMMqttManager.publish(): you must log in to the default server!");
				return false;
			}
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
		bRet = CMEventManager.unicastEvent(pubEvent,strReceiver, m_cmInfo);
		if(!bRet)
			return false; 
		
		// process QoS 1 or 2
		if( qos == 1 || qos == 2 )
		{
			// to get session information
			CMMqttSession session = null;
			CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
			if(strSysType.equals("CLIENT"))
			{
				session = mqttInfo.getMqttSession();
			}
			else if(strSysType.equals("SERVER"))
			{
				session = mqttInfo.getMqttSessionHashtable().get(strReceiver);
			}
			
			if(session == null)
			{
				System.err.println("CMMqttManager.publish(): QoS("+qos+"), but session is null!");
				return false;
			}
			
			// add the sent event to the sent-unack-event-list
			bRet = session.addSentUnAckEvent(pubEvent);
			if(bRet && CMInfo._CM_DEBUG)
			{
				System.out.println("CMMqttManager.publish(): stored to sent unack event list: "
						+pubEvent.toString());
			}
			if(!bRet)
			{
				System.err.println("CMMqttManager.publish(): error to store to sent unack event list: "
						+pubEvent.toString());
				return false;
			}
		}
		
		return bRet;
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
			return false;
		}
		session.setReqSubscriptionList(topicQoSList);
		
		boolean bRet = false;
		bRet = CMEventManager.unicastEvent(subEvent, "SERVER", m_cmInfo);
		
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
		strBuf.append("All MQTT session list\n");
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		Hashtable<String, CMMqttSession> sessionHashtable = mqttInfo.getMqttSessionHashtable();
		for(String strUser : sessionHashtable.keySet())
		{
			strBuf.append("session of user: "+strUser+"\n");
			strBuf.append(getSessionInfo(strUser)+"\n");
		}
		
		return strBuf.toString();
	}
}
