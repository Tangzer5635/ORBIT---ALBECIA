// JSON Patch (RFC 6902) envoyé aux PATCH du back : seules les opérations « replace » sont acceptées,
// et seulement sur les champs modifiables de la ressource.
export interface OperationPatch {
    op: "replace";
    path: string;
    value: unknown;
}

const identiques = (a: unknown, b: unknown) => JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

// Une opération « replace » par champ qui a changé : on n'envoie que ce que l'utilisateur a modifié.
// Ex. versPatch({nom: "A", version: "1"}, {nom: "B", version: "1"}) -> [{op: "replace", path: "/nom", value: "B"}]
export function versPatch<T extends object>(avant: T, apres: T): OperationPatch[] {
    return (Object.keys(apres) as (keyof T & string)[])
        .filter(champ => apres[champ] !== undefined && !identiques(avant[champ], apres[champ]))
        .map(champ => ({op: "replace", path: `/${champ}`, value: apres[champ]}));
}
