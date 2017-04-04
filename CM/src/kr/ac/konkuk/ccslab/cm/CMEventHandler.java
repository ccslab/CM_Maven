package kr.ac.konkuk.ccslab.cm;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;

public interface CMEventHandler {
	void processEvent(CMEvent cme);
}
