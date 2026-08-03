package com.training.services;

public class ContentTypeService {
    public static String detectType(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        if (data.length >= 4
                && data[0] == (byte) 0x25
                && data[1] == (byte) 0x50
                && data[2] == (byte) 0x44
                && data[3] == (byte) 0x46) {
            return "pdf";
        }

        if (data.length >= 8
                && data[0] == (byte) 0xD0
                && data[1] == (byte) 0xCF
                && data[2] == (byte) 0x11
                && data[3] == (byte) 0xE0
                && data[4] == (byte) 0xA1
                && data[5] == (byte) 0xB1
                && data[6] == (byte) 0x1A
                && data[7] == (byte) 0xE1) {
            return "excel";
        }

        if (data.length >= 4
                && data[0] == 'P'
                && data[1] == 'K'
                && data[2] == 0x03
                && data[3] == 0x04) {
            return "excel";
        }

        return null;
    }
}
