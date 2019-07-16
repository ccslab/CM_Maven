import java.nio.ByteBuffer;

import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;

public class CMMqttEventTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CMMqttEventTest tester = new CMMqttEventTest();
		
		tester.testCONNECT();
		tester.testCONNACK();
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
}