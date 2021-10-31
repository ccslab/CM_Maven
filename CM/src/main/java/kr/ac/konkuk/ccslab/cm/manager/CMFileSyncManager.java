package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CMFileSyncManager extends CMServiceManager {

    public CMFileSyncManager(CMInfo cmInfo) {
        super(cmInfo);
        m_nType = CMInfo.CM_FILE_SYNC_MANAGER;
    }

    public Path getClientSyncHome() {
        CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
        return confInfo.getTransferedFileHome().resolve(CMFileSyncInfo.SYNC_HOME)
                .toAbsolutePath().normalize();
    }

    public Path getServerSyncHome(String userName) {
        CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
        return confInfo.getTransferedFileHome().resolve(userName)
                .resolve(CMFileSyncInfo.SYNC_HOME).toAbsolutePath().normalize();
    }

    public boolean startFileSync() {

        CMFileSyncInfo fsInfo = m_cmInfo.getFileSyncInfo();

        if(fsInfo.isSyncInProgress()) {
            System.err.println("The file sync is in progress!");
            return false;
        }
        else {
            // set syncInProgress to true.
            fsInfo.setSyncInProgress(true);
        }

        // set file sync home.
        Path syncHome = getClientSyncHome();
        if(Files.notExists(syncHome)) {
            try {
                Files.createDirectories(syncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create a file list in the sync-file-home


        // from here

        return true;
    }

}
