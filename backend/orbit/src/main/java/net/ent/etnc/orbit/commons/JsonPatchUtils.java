package net.ent.etnc.orbit.commons;

import jakarta.json.*;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.StringReader;
import java.util.Set;

/**
 * Applique un JSON Patch (RFC 6902) sur un DTO, en contrôlant les champs autorisés (whitelist).
 *
 * Le patch cible les attributs du DTO (Payload). La correspondance avec les attributs de l'entité
 * (qui peuvent différer, ex. {@code trainId} ↔ {@code train}) est ensuite assurée par l'assembler
 * ({@code toEntity}). On reste indépendant de la version de Jackson : le patch est traité via
 * Jakarta JSON-P, l'ObjectMapper ne sert qu'à convertir DTO ↔ JSON.
 */
@Component
public class JsonPatchUtils {

    private final ObjectMapper objectMapper;

    @Autowired
    public JsonPatchUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param patchJson       le corps de la requête (tableau d'opérations RFC 6902)
     * @param currentDto      l'état courant de la ressource, en représentation DTO
     * @param dtoType         le type du DTO à reconstruire
     * @param patchableFields les champs (1er segment du {@code path}) autorisés à être modifiés
     */
    public <P> P apply(String patchJson, P currentDto, Class<P> dtoType, Set<String> patchableFields) {
        JsonArray operations = parseOperations(patchJson);
        enforceWhitelist(operations, patchableFields);
        return applyPatch(operations, currentDto, dtoType);
    }

    private JsonArray parseOperations(String patchJson) {
        try (var reader = Json.createReader(new StringReader(patchJson))) {
            return reader.readArray();
        } catch (RuntimeException e) {
            throw new ServiceException("Patch invalide : corps JSON Patch illisible.", e);
        }
    }

    private void enforceWhitelist(JsonArray operations, Set<String> patchableFields) {
        for (JsonValue value : operations) {
            if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new ServiceException("Patch invalide : chaque opération doit être un objet.");
            }
            JsonObject op = value.asJsonObject();
            // Base relationnelle : seule l'opération "replace" a du sens (les add/remove/move/copy/test
            // relèvent d'un stockage NoSQL/document).
            String operation = op.getString("op", null);
            if (!"replace".equals(operation)) {
                throw new ServiceException("Opération non supportée : " + operation
                        + " (seul 'replace' est autorisé).");
            }
            checkField(op.getString("path", null), patchableFields);
        }
    }

    private void checkField(String pointer, Set<String> patchableFields) {
        if (pointer == null) {
            return;
        }
        String field = topSegment(pointer);
        if (!patchableFields.contains(field)) {
            throw new ServiceException("Champ non patchable : " + field);
        }
    }

    private String topSegment(String pointer) {
        // "/trainId" -> "trainId" ; "/a/b" -> "a" ; "" -> ""
        String trimmed = pointer.startsWith("/") ? pointer.substring(1) : pointer;
        int slash = trimmed.indexOf('/');
        return slash >= 0 ? trimmed.substring(0, slash) : trimmed;
    }

    private <P> P applyPatch(JsonArray operations, P currentDto, Class<P> dtoType) {
        try {
            JsonObject target;
            try (var reader = Json.createReader(new StringReader(objectMapper.writeValueAsString(currentDto)))) {
                target = reader.readObject();
            }
            JsonObject patched = Json.createPatch(operations).apply(target);
            return objectMapper.readValue(patched.toString(), dtoType);
        } catch (JsonException e) {
            throw new ServiceException("Patch invalide : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ServiceException("Patch invalide : impossible d'appliquer le patch.", e);
        }
    }
}
