package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttSession;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttTopicQoS;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttWill;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
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
	
	public boolean connect(String strClientID)
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
		
		// process will message
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
			CMList<CMMqttEvent> sentUnackEventList = session.getSentUnAckEventList();
			bRet = sentUnackEventList.addElement(pubEvent);
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
		return true;
	}
	
	public boolean subscribe(String nPacketID, String strTopicFilterString, byte qos)
	{
		return true;
	}
	
	public boolean subscribe(int nPacketID, CMList<CMMqttTopicQoS> topicQoSList)
	{
		return true;
	}
	
	public boolean unsubscribe(String strTopic)
	{
		return true;
	}
	
	public boolean unsubscribe(int nPacketID, String strTopic)
	{
		return true;
	}
	
	public boolean unsubscribe(int nPacketID, CMList<String> topicList)
	{
		return true;
	}
	
	public boolean requestPing()
	{
		return true;
	}
	
	public boolean disconnect()
	{
		return true;
	}
}
