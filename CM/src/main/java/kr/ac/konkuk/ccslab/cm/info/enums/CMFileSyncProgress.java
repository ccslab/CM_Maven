package kr.ac.konkuk.ccslab.cm.info.enums;

public enum CMFileSyncProgress {
    NONE,           // 동기화 미진행
    FULL_SYNC,      // sync() — 전체 파일 목록 동기화
    ONLINE_MODE,    // requestOnlineMode()
    LOCAL_MODE,     // requestLocalMode()
    PUSH,           // (향후) 양방향 증분 push
    PULL            // (향후) 양방향 증분 pull
}
