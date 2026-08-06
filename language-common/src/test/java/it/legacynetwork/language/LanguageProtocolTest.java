package it.legacynetwork.language;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageProtocolTest {
    private final LanguageProtocol protocol = new LanguageProtocol();
    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void serializesAndDeserializes() throws Exception {
        LanguageProtocolMessage source =
                LanguageProtocolMessage.languageSync(uuid, Language.ITALIAN, true);
        byte[] first = protocol.serialize(source);
        byte[] second = protocol.serialize(source);
        assertArrayEquals(first, second);

        LanguageProtocolMessage decoded = protocol.deserialize(first);
        assertEquals(LanguageProtocol.VERSION, decoded.getProtocolVersion());
        assertEquals(LanguageProtocolAction.LANGUAGE_SYNC, decoded.getAction());
        assertEquals(uuid, decoded.getPlayerUuid());
        assertEquals("it", decoded.getLanguageCode());
        assertTrue(decoded.isManualPreference());
    }

    @Test
    void rejectsWrongVersion() throws Exception {
        assertThrows(LanguageProtocolException.class,
                () -> protocol.deserialize(rawPayload(2, uuid.toString(), "en")));
    }

    @Test
    void rejectsInvalidUuid() throws Exception {
        assertThrows(LanguageProtocolException.class,
                () -> protocol.deserialize(rawPayload(1, "not-a-uuid", "en")));
    }

    @Test
    void rejectsInvalidLanguage() throws Exception {
        assertThrows(LanguageProtocolException.class,
                () -> protocol.deserialize(rawPayload(1, uuid.toString(), "zz")));
    }

    @Test
    void rejectsOversizedPayload() {
        assertThrows(LanguageProtocolException.class,
                () -> protocol.deserialize(new byte[LanguageProtocol.MAX_PAYLOAD_BYTES + 1]));
    }

    private byte[] rawPayload(int version, String uuidText, String language) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(version);
        output.writeByte(LanguageProtocolAction.LANGUAGE_SYNC.getId());
        output.writeUTF(uuidText);
        output.writeUTF(language);
        output.writeBoolean(false);
        output.flush();
        return Arrays.copyOf(bytes.toByteArray(), bytes.size());
    }
}
