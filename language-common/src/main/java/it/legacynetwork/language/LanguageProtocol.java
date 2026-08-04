package it.legacynetwork.language;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class LanguageProtocol {
    public static final int VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 1024;
    public static final int MAX_LANGUAGE_CODE_LENGTH = 8;

    public byte[] serialize(LanguageProtocolMessage message) throws LanguageProtocolException {
        validateMessage(message);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(message.getProtocolVersion());
            output.writeByte(message.getAction().getId());
            output.writeUTF(message.getPlayerUuid().toString());
            output.writeUTF(message.getLanguageCode());
            output.writeBoolean(message.isManualPreference());
            output.flush();
            byte[] payload = bytes.toByteArray();
            validateSize(payload);
            return payload;
        } catch (IOException exception) {
            throw new LanguageProtocolException("Impossibile serializzare il messaggio", exception);
        }
    }

    public LanguageProtocolMessage deserialize(byte[] payload) throws LanguageProtocolException {
        if (payload == null) {
            throw new LanguageProtocolException("Payload mancante");
        }
        validateSize(payload);
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            int version = input.readInt();
            if (version != VERSION) {
                throw new LanguageProtocolException("Versione protocollo non supportata: " + version);
            }
            LanguageProtocolAction action = LanguageProtocolAction.fromId(input.readUnsignedByte());
            String uuidText = input.readUTF();
            String languageCode = input.readUTF();
            boolean manualPreference = input.readBoolean();
            if (input.available() != 0) {
                throw new LanguageProtocolException("Il payload contiene dati inattesi");
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException exception) {
                throw new LanguageProtocolException("UUID non valido", exception);
            }
            LanguageProtocolMessage message = new LanguageProtocolMessage(
                    version, action, uuid, languageCode, manualPreference);
            validateMessage(message);
            return message;
        } catch (EOFException exception) {
            throw new LanguageProtocolException("Payload incompleto", exception);
        } catch (IOException exception) {
            throw new LanguageProtocolException("Impossibile deserializzare il messaggio", exception);
        }
    }

    private void validateMessage(LanguageProtocolMessage message)
            throws LanguageProtocolException {
        if (message == null) {
            throw new LanguageProtocolException("Messaggio mancante");
        }
        if (message.getProtocolVersion() != VERSION) {
            throw new LanguageProtocolException(
                    "Versione protocollo non supportata: " + message.getProtocolVersion());
        }
        String code = message.getLanguageCode();
        if (code.isEmpty() || code.length() > MAX_LANGUAGE_CODE_LENGTH
                || code.getBytes(StandardCharsets.UTF_8).length > MAX_LANGUAGE_CODE_LENGTH) {
            throw new LanguageProtocolException("Codice lingua non valido");
        }
        Optional<Language> language = Language.findByInput(code);
        if (!language.isPresent() || !language.get().getCode().equalsIgnoreCase(code)) {
            throw new LanguageProtocolException("Lingua protocollo non valida: " + code);
        }
    }

    private void validateSize(byte[] payload) throws LanguageProtocolException {
        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw new LanguageProtocolException(
                    "Payload oltre il limite di " + MAX_PAYLOAD_BYTES + " byte");
        }
    }
}
