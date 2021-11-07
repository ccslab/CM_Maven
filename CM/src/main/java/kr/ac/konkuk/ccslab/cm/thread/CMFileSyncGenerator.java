package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CMFileSyncGenerator implements Runnable {
    private String userName;
    private CMInfo cmInfo;
    private List<CMFileSyncEntry> fileEntryList;
    private List<Path> basisFileList;

    public CMFileSyncGenerator(String userName, CMInfo cmInfo) {
        this.userName = userName;
        this.cmInfo = cmInfo;
        fileEntryList = null;
        basisFileList = null;
    }

    @Override
    public void run() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncGenerator.run() called..");
        }

        // get client file-entry-list
        fileEntryList = cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
        if(fileEntryList == null) {
            System.err.println("CMFileSyncGenerator.run(), fileEntryList is null!");
            return;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("fileEntryList = " + fileEntryList);
        }

        // create a basis file-entry-list at the server
        basisFileList = createBasisFileList();
        if(basisFileList == null) {
            System.err.println("CMFileSyncGenerator.run(), basisFileList is null!");
            return;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFileList = " + basisFileList);
        }

        //// compare the client file-entry-list and the basis file-entry-list

        // create and delete a file-entry-list that exists only at the server

        // from here

        // create a new file-entry-list that will be added to the server

        // request the files in the new file-entry-list from the client

        // update the files at the server by synchronizing with those at the client

    }

    private List<Path> createBasisFileList() {

        // get the file sync manager
        CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        if(syncManager == null) {
            System.err.println("CMFileSyncGenerator.createBasisFileList(), file-sync manager is null!");
            return null;
        }
        // get the server sync home
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        // check if the sync home exists or not
        if(Files.notExists(serverSyncHome)) {
            System.err.println("CMFileSyncGenerator.createBasisFileList(), the server sync-home does not exist!");
            return null;
        }
        // create a basis file list
        return syncManager.createPathList(serverSyncHome);
    }
}
