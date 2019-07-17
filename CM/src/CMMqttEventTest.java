import java.nio.ByteBuffer;

import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;

public class CMMqttEventTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CMMqttEventTest tester = new CMMqttEventTest();
		
		tester.testCONNECT();
		tester.testCONNACK();
		tester.testPUBLISH();
		tester.testPUBACK();
	}

	private void testCONNECT()
	{
		System.out.println("===================");
		CMMqttEventCONNECT mqttCONNECTEvent = new CMMqttEventCONNECT();
		mqttCONNECTEvent.setUserNameFlag(true);
		mqttCONNECTEvent.setWillQoS((byte)2);
		mqttCONNECTEvent.setWillFlag(true);
		mqttCONNECTEvent.setKeepAlive(100);
		
		mqttCONNECTEvent.setClientID("mqtt-test-client");
		mqttCONNECTEvent.setWillTopic("/CM/mqtt");
		mqttCONNECTEvent.setWillMessage("mqtt-connect-test-message");
		mqttCONNECTEvent.setUserName("ccslab");
		mqttCONNECTEvent.setPassword("ccslab");

		System.out.println("------------------- after setting member variables");
		System.out.println(mqttCONNECTEvent.toString());
		
		ByteBuffer buf = mqttCONNECTEvent.marshall();
		CMMqttEventCONNECT mqttCONNECTEvent2 = new CMMqttEventCONNECT(buf);
		
		System.out.println("------------------- after marshalling/unmarshalling the event");
		System.out.println(mqttCONNECTEvent2.toString());		
	}
	
	private void testCONNACK()
	{
		System.out.println("===================");
		CMMqttEventCONNACK mqttConnack = new CMMqttEventCONNACK();
		mqttConnack.setVarHeader(true, (byte)5);
		System.out.println("------------------- after setting member variables");
		System.out.println(mqttConnack.toString());
		
		ByteBuffer buf = mqttConnack.marshall();
		CMMqttEventCONNACK mqttConnack2 = new CMMqttEventCONNACK(buf);
		System.out.println("------------------- after marshalling/unmarshalling the event");
		System.out.println(mqttConnack2.toString());
	}
	
	private void testPUBLISH()
	{
		System.out.println("===================");
		CMMqttEventPUBLISH mqttPublish = new CMMqttEventPUBLISH();
		mqttPublish.setDupFlag(false);
		mqttPublish.setQoS((byte)1);
		mqttPublish.setRetainFlag(true);
		mqttPublish.setTopicName("CM/mqtt/test");
		mqttPublish.setPacketID(1);
		mqttPublish.setAppMessage("test app message");
		
		System.out.println("------------------- after setting member variables");
		System.out.println(mqttPublish.toString());
		
		ByteBuffer buf = mqttPublish.marshall();
		CMMqttEventPUBLISH mqttPublish2 = new CMMqttEventPUBLISH(buf);
		
		System.out.println("------------------- after marshalling/unmarshalling the event");
		System.out.println(mqttPublish2.toString());
	}
	
	private void testPUBACK()
	{
		System.out.println("===================");
		CMMqttEventPUBACK mqttPuback = new CMMqttEventPUBACK();
		mqttPuback.setPacketID(5);
		System.out.println("------------------- after setting member variables");
		System.out.println(mqttPuback.toString());
		
		ByteBuffer buf = mqttPuback.marshall();
		CMMqttEventPUBACK mqttPuback2 = new CMMqttEventPUBACK(buf);
		
		System.out.println("------------------- after marshalling/unmarshalling the event");
		System.out.println(mqttPuback2.toString());
	}
}
