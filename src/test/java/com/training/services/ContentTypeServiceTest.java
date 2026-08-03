package com.training.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ContentTypeServiceTest {
// ---------- Full, valid signatures ----------

    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x01, 0x02};
    private static final byte[] XLS_OLE_BYTES = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1, 0x00
    };
    private static final byte[] ZIP_SIGNATURE_BYTES = {'P', 'K', 0x03, 0x04, 0x05, 0x06};

    // ---------- Null / empty ----------

    @Test
    void shouldReturnNullWhenDataIsNull() {
        assertNull(ContentTypeService.detectType(null));
    }

    @Test
    void shouldReturnNullWhenDataIsEmpty() {
        assertNull(ContentTypeService.detectType(new byte[0]));
    }

    @ParameterizedTest
    @MethodSource("recognizedSignatures")
    void shouldDetectKnownContentTypes(byte[] payload, String expectedType) {
        assertEquals(expectedType, ContentTypeService.detectType(payload));
    }

    private static Stream<Arguments> recognizedSignatures() {
        return Stream.of(
                Arguments.of(PDF_BYTES, "pdf"),
                Arguments.of(XLS_OLE_BYTES, "excel"),
                Arguments.of(ZIP_SIGNATURE_BYTES, "excel")
        );
    }

    @ParameterizedTest
    @MethodSource("tooShortForSignatures")
    void shouldReturnNullWhenDataTooShortForSignature(byte[] payload) {
        assertNull(ContentTypeService.detectType(payload));
    }

    private static Stream<byte[]> tooShortForSignatures() {
        return Stream.of(
                new byte[]{0x25, 0x50, 0x44},                                   // 3 bytes, PDF needs 4
                new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                        (byte) 0xA1, (byte) 0xB1, 0x1A},                     // 7 bytes, OLE needs 8
                new byte[]{'P', 'K', 0x03}                                      // 3 bytes, ZIP needs 4
        );
    }


    @ParameterizedTest
    @MethodSource("nearMissSignatures")
    void shouldReturnNullWhenSignatureBytesDontMatchExactly(byte[] payload) {
        assertNull(ContentTypeService.detectType(payload));
    }

    // Asked claude for this to test most of the branches
    private static Stream<byte[]> nearMissSignatures() {
        return Stream.of(
                // PDF near-misses
                new byte[]{0x00, 0x50, 0x44, 0x46},
                new byte[]{0x25, 0x00, 0x44, 0x46},
                new byte[]{0x25, 0x50, 0x00, 0x46},
                new byte[]{0x25, 0x50, 0x44, 0x00},

                // OLE near-misses (flip one byte in each position)
                new byte[]{0x00, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1},
                new byte[]{(byte) 0xD0, 0x00, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1},
                new byte[]{(byte) 0xD0, (byte) 0xCF, 0x00, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1},
                new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, 0x00, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1},

                // ZIP near-misses
                new byte[]{'X', 'K', 0x03, 0x04},
                new byte[]{'P', 'X', 0x03, 0x04},
                new byte[]{'P', 'K', 0x00, 0x04},
                new byte[]{'P', 'K', 0x03, 0x00}
        );
    }

    @ParameterizedTest
    @MethodSource("unrecognizedData")
    void shouldReturnNullForUnrecognizedData(byte[] payload) {
        assertNull(ContentTypeService.detectType(payload));
    }

    private static Stream<byte[]> unrecognizedData() {
        return Stream.of(
                new byte[]{0x00, 0x01, 0x02, 0x03},
                new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0xFD, (byte) 0xFC},
                new byte[]{'a', 'b', 'c', 'd', 'e', 'f'}
        );
    }
}
