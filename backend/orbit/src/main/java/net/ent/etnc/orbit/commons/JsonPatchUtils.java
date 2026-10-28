package net.ent.etnc.orbit.commons;

import jakarta.json.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.StringReader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applique un JSON Patch (RFC 6902) sur un DTO, en contrôlant les champs autorisés (whitelist).
 *
 * Le patch cible les attributs du DTO (Payload). La correspondance avec les attributs de l'entité
 * (qui peuvent différer, ex. {@code trainId} ↔ {@code train}) est ensuite assurée par l'assembler
 * ({@code toEntity}). On reste indépendant de la version de Jackson : le patch est traité via
 * Jakarta JSON-P, l'ObjectMapper ne sert qu'à convertir DTO ↔ JSON.
 *
 * <p>Le DTO obtenu est validé (Bean Validation) comme le serait un {@code @Valid @RequestBody} :
 * un patch ne peut pas laisser la ressource dans un état invalide.
 */
@Component
public class JsonPatchUtils {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Autowired
    public JsonPatchUtils(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
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
        P patched = applyPatch(operations, currentDto, dtoType);
        validate(patched);
        return patched;
    }

    /** Mêmes contraintes que pour un {@code @Valid @RequestBody} : « champ : message ; champ : message ». */
    private <P> void validate(P patched) {
        Set<ConstraintViolation<P>> violations = validator.validate(patched);
        if (!violations.isEmpty()) {
            throw new ServiceException("Patch invalide : " + violations.stream()
                    .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(" ; ")));
        }
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
            JsonObjectBuilder builder = Json.createObjectBuilder(target);
            for (JsonValue op : operations) {
                String field = topSegment(op.asJsonObject().getString("path", ""));
                if (!field.isEmpty() && !target.containsKey(field)) builder.add(field, JsonValue.NULL);
            }
            target = builder.build();
            JsonObject patched = Json.createPatch(operations).apply(target);
            return objectMapper.readValue(patched.toString(), dtoType);        } catch (JsonException e) {
            throw new ServiceException("Patch invalide : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ServiceException("Patch invalide : impossible d'appliquer le patch.", e);
        }
    }
}
