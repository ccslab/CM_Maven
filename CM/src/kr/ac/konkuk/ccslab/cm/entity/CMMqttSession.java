package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBCOMP;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREC;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREL;

/**
 * The CMMqttSession class represents a MQTT session.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttSession {
	// to store will message
	private CMMqttWill m_mqttWill;
	// list of determined subscriptions
	private CMList<CMMqttTopicQoS> m_subscriptionList;
	// list of requested subscriptions (4 client)
	private CMList<CMMqttTopicQoS> m_reqSubscriptionList;
	// list of sent QoS 1 and QoS 2 events (PUBLISH, PUBREL) that have not been completely acknowledged 
	// (4 client: client->server, 4 server: server->client)
	private CMList<CMMqttEvent> m_sentUnAckEventList;
	// list of received QoS 2 PUBLISH and PUBREC events that have not been completely acknowledged 
	// (4 client: client<-server, 4 server: server<-client)
	private CMList<CMMqttEvent> m_recvUnAckEventList;
	// list of pending QoS 1 and QoS 2 PUBLISH events of transmission to the client (4 server)
	private CMList<CMMqttEventPUBLISH> m_pendingTransEventList;
	
	public CMMqttSession()
	{
		m_mqttWill = null;
		m_subscriptionList = new CMList<CMMqttTopicQoS>();
		m_reqSubscriptionList = null;
		m_sentUnAckEventList = new CMList<CMMqttEvent>();
		m_recvUnAckEventList = new CMList<CMMqttEvent>();
		m_pendingTransEventList = new CMList<CMMqttEventPUBLISH>();
	}
	
	//////////////////////// setter/getter
	
	public void setMqttWill(CMMqttWill mqttWill)
	{
		m_mqttWill = mqttWill;
	}
	
	public CMMqttWill getMqttWill()
	{
		return m_mqttWill;
	}
	
	// subscription list
	public void setSubscriptionList(CMList<CMMqttTopicQoS> subscriptionList)
	{
		m_subscriptionList = subscriptionList;
	}
	
	public CMList<CMMqttTopicQoS> getSubscriptionList()
	{
		return m_subscriptionList;
	}
	
	public void setReqSubscriptionList(CMList<CMMqttTopicQoS> reqSubscriptionList)
	{
		m_reqSubscriptionList = reqSubscriptionList;
	}
	
	public CMList<CMMqttTopicQoS> getReqSubscriptionList()
	{
		return m_reqSubscriptionList;
	}
	
	// sent-unack-event list
	public void setSentUnAckEventList(CMList<CMMqttEvent> eventList)
	{
		m_sentUnAckEventList = eventList;
	}
	
	public CMList<CMMqttEvent> getSentUnAckEventList()
	{
		return m_sentUnAckEventList;
	}
	
	// recv-unack-event list
	public void setRecvUnAckEventList(CMList<CMMqttEvent> eventList)
	{
		m_recvUnAckEventList = eventList;
	}
	
	public CMList<CMMqttEvent> getRecvUnAckEventList()
	{
		return m_recvUnAckEventList;
	}
	
	// pending-trans-event list
	public void setPendingTransEventList(CMList<CMMqttEventPUBLISH> eventList)
	{
		m_pendingTransEventList = eventList;
	}
	
	public CMList<CMMqttEventPUBLISH> getPendingTransEventList()
	{
		return m_pendingTransEventList;
	}
	
	//////////////////////// subscription list
	
	public boolean addSubscription(CMMqttTopicQoS topicQoS)
	{
		return m_subscriptionList.addElement(topicQoS);
	}
	
	public CMMqttTopicQoS findSubscription(String strTopic)
	{
		for(CMMqttTopicQoS topicQoS : m_subscriptionList.getList())
		{
			if(topicQoS.getTopic().equals(strTopic))
				return topicQoS;
		}
		
		return null;
	}
	
	public boolean removeSubscription(String strTopic)
	{
		CMMqttTopicQoS topicQoS = findSubscription(strTopic);
		if(topicQoS == null)
			return false;
		
		return m_subscriptionList.removeElement(topicQoS);
	}
	
	public void removeAllSubscription()
	{
		m_subscriptionList.removeAllElements();
		return;
	}
	
	//////////////////////// sent-unack-event list
	
	public boolean addSentUnAckEvent(CMMqttEvent unackEvent)
	{
		int nID = getMQTTPacketID(unackEvent);
		CMMqttEvent mqttEvent = findSentUnAckEvent(nID);
		if(mqttEvent != null)
		{
			System.err.println("CMMqttSession.addSentUnAckEvent(), the same packet ID ("
					+nID+") already exists!");
			System.err.println(mqttEvent.toString());
			return false;			
		}
		
		return m_sentUnAckEventList.addElement(unackEvent);
	}
	
	public CMMqttEvent findSentUnAckEvent(int nPacketID)
	{
		int nID = -1;
		for(CMMqttEvent unackEvent : m_sentUnAckEventList.getList())
		{
			nID = getMQTTPacketID(unackEvent);
			if(nID == nPacketID)
				return unackEvent;
		}
		
		return null;
	}
	
	public boolean removeSentUnAckEvent(int nPacketID)
	{
		CMMqttEvent unackEvent = findSentUnAckEvent(nPacketID);
		if(unackEvent == null)
			return false;
		
		return m_sentUnAckEventList.removeElement(unackEvent);
	}
	
	public void removeAllSentUnAckEvent()
	{
		m_sentUnAckEventList.removeAllElements();
		return;
	}
	
	//////////////////////// recv-unack-event list

	public boolean addRecvUnAckEvent(CMMqttEvent mqttEvent)
	{
		int nID = getMQTTPacketID(mqttEvent);
		CMMqttEvent event = findRecvUnAckEvent(nID);
		if(event != null)
		{
			System.err.println("CMMqttSession.addRecvUnAckEvent(), the same packet ID ("
					+nID+") already exists!");
			if(event instanceof CMMqttEventPUBLISH)
				System.err.println(((CMMqttEventPUBLISH)event).toString());
			else if(event instanceof CMMqttEventPUBREC)
				System.err.println(((CMMqttEventPUBREC)event).toString());
			
			return false;
		}
		
		return m_recvUnAckEventList.addElement(mqttEvent);
	}
	
	public CMMqttEvent findRecvUnAckEvent(int nPacketID)
	{
		int nID = -1;
		for(CMMqttEvent mqttEvent : m_recvUnAckEventList.getList())
		{
			nID = getMQTTPacketID(mqttEvent);
			if(nID == nPacketID)
				return mqttEvent;
		}
		
		return null;
	}
	
	public boolean removeRecvUnAckEvent(int nPacketID)
	{
		CMMqttEvent mqttEvent = findRecvUnAckEvent(nPacketID);
		if(mqttEvent == null)
			return false;
		
		return m_recvUnAckEventList.removeElement(mqttEvent);
	}
	
	public void removeAllRecvUnAckEvent()
	{
		m_recvUnAckEventList.removeAllElements();
		return;
	}

	//////////////////////// pending-trans-event list
	
	public boolean addPendingTransEvent(CMMqttEventPUBLISH pendingEvent)
	{
		int nID = pendingEvent.getPacketID();
		CMMqttEvent mqttEvent = findPendingTransEvent(nID);
		if(mqttEvent != null)
		{
			System.err.println("CMMqttSession.addPendingTransEvent(), the same packet ID ("
					+nID+") already exists!");
			System.err.println(mqttEvent.toString());
			return false;			
		}
		
		return m_pendingTransEventList.addElement(pendingEvent);
	}
	
	public CMMqttEventPUBLISH findPendingTransEvent(int nPacketID)
	{
		int nID = -1;
		for(CMMqttEventPUBLISH pendingEvent : m_pendingTransEventList.getList())
		{
			nID = pendingEvent.getPacketID();
			if(nID == nPacketID)
				return pendingEvent;
		}
		
		return null;
	}
	
	public boolean removePendingTransEvent(int nPacketID)
	{
		CMMqttEventPUBLISH pendingEvent = findPendingTransEvent(nPacketID);
		if(pendingEvent == null)
			return false;
		
		return m_pendingTransEventList.removeElement(pendingEvent);
	}
	
	public void removeAllPendingTransEvent()
	{
		m_pendingTransEventList.removeAllElements();
		return;
	}

	//////////////////////////////////// Overridden methods
	
	@Override
	public String toString()
	{
		StringBuffer strBuf = new StringBuffer();
		strBuf.append("CMMqttSession {\n");
		if(m_mqttWill == null)
			strBuf.append("Mqtt-Will is null!\n");
		else
			strBuf.append(m_mqttWill.toString()+"\n");
		
		strBuf.append("Subscription List: "+m_subscriptionList.toString()+"\n");
		strBuf.append("}\n");
		
		return strBuf.toString();
	}
	
	///////////////////////////////////// private methods
	
	private int getMQTTPacketID(CMMqttEvent mqttEvent)
	{
		int nID = -1;
		if(mqttEvent instanceof CMMqttEventPUBLISH)
			nID = ((CMMqttEventPUBLISH)mqttEvent).getPacketID();
		else if(mqttEvent instanceof CMMqttEventPUBACK)
			nID = ((CMMqttEventPUBACK)mqttEvent).getPacketID();
		else if(mqttEvent instanceof CMMqttEventPUBREC)
			nID = ((CMMqttEventPUBREC)mqttEvent).getPacketID();
		else if(mqttEvent instanceof CMMqttEventPUBREL)
			nID = ((CMMqttEventPUBREL)mqttEvent).getPacketID();
		else if(mqttEvent instanceof CMMqttEventPUBCOMP)
			nID = ((CMMqttEventPUBCOMP)mqttEvent).getPacketID();
		
		return nID;
	}
	
}
