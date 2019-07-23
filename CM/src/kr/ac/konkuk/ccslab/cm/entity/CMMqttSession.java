package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;

/**
 * The CMMqttSession class represents a MQTT session.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttSession {
	// to store will message
	private CMMqttWill m_mqttWill;
	// list of subscriptions (4 server)
	private CMList<CMMqttTopicQoS> m_subscriptionList;
	// list of sent QoS 1 and QoS 2 events that have not been completely acknowledged 
	// (4 client: client->server, 4 server: server->client)
	private CMList<CMMqttEvent> m_sentUnAckEventList;
	// // list of received QoS 2 events that have not been completely acknowledged 
	// (4 client: client<-server, 4 server: server<-client)
	private CMList<CMMqttEvent> m_recvUnAckEventList;
	// list of events pending transmission to the client (4 server)
	private CMList<CMMqttEvent> m_pendingTransEventList;
	
	public CMMqttSession()
	{
		m_mqttWill = null;
		m_subscriptionList = new CMList<CMMqttTopicQoS>();
		m_sentUnAckEventList = new CMList<CMMqttEvent>();
		m_recvUnAckEventList = new CMList<CMMqttEvent>();
		m_pendingTransEventList = new CMList<CMMqttEvent>();
	}
	
	// setter/getter
	public void setMqttWill(CMMqttWill mqttWill)
	{
		m_mqttWill = mqttWill;
	}
	
	public CMMqttWill getMqttWill()
	{
		return m_mqttWill;
	}
	
	public void setSubscriptionList(CMList<CMMqttTopicQoS> subscriptionList)
	{
		m_subscriptionList = subscriptionList;
	}
	
	public CMList<CMMqttTopicQoS> getSubscriptionList()
	{
		return m_subscriptionList;
	}
	
	public void setSentUnAckEventList(CMList<CMMqttEvent> eventList)
	{
		m_sentUnAckEventList = eventList;
	}
	
	public CMList<CMMqttEvent> getSentUnAckEventList()
	{
		return m_sentUnAckEventList;
	}
	
	public void setRecvUnAckEventList(CMList<CMMqttEvent> eventList)
	{
		m_recvUnAckEventList = eventList;
	}
	
	public CMList<CMMqttEvent> getRecvUnAckEventList()
	{
		return m_recvUnAckEventList;
	}
	
	public void setPendingTransEventList(CMList<CMMqttEvent> eventList)
	{
		m_pendingTransEventList = eventList;
	}
	
	public CMList<CMMqttEvent> getPendingTransEventList()
	{
		return m_pendingTransEventList;
	}
}
