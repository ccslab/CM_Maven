package kr.ac.konkuk.ccslab.cm.util;

import java.util.UUID;

public final class CMUUIDConverter {

    private CMUUIDConverter() {
        // utility class
    }

    // Converts UUID to String
    public static String uuidToString(UUID uuid) {
        // If uuid is null, return empty string
        if (uuid == null) {
            return "";
        }
        return uuid.toString();
    }

    // Converts String to UUID
    public static UUID stringToUuid(String strUuid) {
        //  If string is null or empty, return null
        if (strUuid == null || strUuid.isEmpty()) {
            return null;
        }

        try {
            //  Convert string to UUID object
            return UUID.fromString(strUuid);
        } catch (IllegalArgumentException e) {
            System.err.println("CMUUIDConverter.stringToUuid(), Invalid UUID format: " + strUuid);
            return null;
        }
    }
}
