package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMFileSyncEventHandler extends CMEventHandler {

    public CMFileSyncEventHandler(CMInfo cmInfo) {
        super(cmInfo);
        m_nType = CMInfo.CM_FILE_SYNC_EVENT_HANDLER;
    }

    @Override
    public boolean processEvent(CMEvent event) {
        boolean processResult;
        CMFileSyncEvent fse = (CMFileSyncEvent)event;
        int eventId = fse.getID();
        switch(eventId) {
            case CMFileSyncEvent.START_FILE_LIST:
                processResult = processSTART_FILE_LIST(fse);
                break;
            case CMFileSyncEvent.START_FILE_LIST_ACK:
                processResult = processSTART_FILE_LIST_ACK(fse);
                break;
            case CMFileSyncEvent.FILE_ENTRIES:
                processResult = processFILE_ENTRIES(fse);
                break;
            case CMFileSyncEvent.FILE_ENTRIES_ACK:
                processResult = processFILE_ENTRIES_ACK(fse);
                break;
            case CMFileSyncEvent.END_FILE_LIST:
                processResult = processEND_FILE_LIST(fse);
                break;
            case CMFileSyncEvent.END_FILE_LIST_ACK:
                processResult = processEND_FILE_LIST_ACK(fse);
                break;
            default:
                System.err.println("CMFileSyncEventHandler::processEvent(), invalid event id(" + eventId + ")!");
                return false;
        }

        return processResult;
    }
    
    private boolean processSTART_FILE_LIST(CMFileSyncEvent fse) {
        // from here
        return false;
    }

    private boolean processSTART_FILE_LIST_ACK(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processFILE_ENTRIES(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processFILE_ENTRIES_ACK(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processEND_FILE_LIST(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processEND_FILE_LIST_ACK(CMFileSyncEvent fse) {
        return false;
    }
}
