package kr.ac.konkuk.ccslab.cm.sns;

import kr.ac.konkuk.ccslab.cm.entity.CMObject;
import java.util.Calendar;

/**
 * This class contains SNS-related user information which was previously in CMUser.
 * To support concurrent login with a single user ID, this information is separated from CMUser.
 */
public class CMSNSUserInfo extends CMObject {
    private CMSNSAttachAccessHistoryList m_historyList;
    private Calendar m_lastLoginDate;

    public CMSNSUserInfo() {
        m_historyList = new CMSNSAttachAccessHistoryList();
        m_lastLoginDate = null;
    }

    // Setter methods
    public synchronized void setSNSAttachAccessHistoryList(CMSNSAttachAccessHistoryList list) {
        m_historyList = list;
    }

    public synchronized void setLastLoginDate(Calendar date) {
        m_lastLoginDate = date;
    }

    // Getter methods
    public synchronized CMSNSAttachAccessHistoryList getSNSAttachAccessHistoryList() {
        return m_historyList;
    }

    public synchronized Calendar getLastLoginDate() {
        return m_lastLoginDate;
    }
}
