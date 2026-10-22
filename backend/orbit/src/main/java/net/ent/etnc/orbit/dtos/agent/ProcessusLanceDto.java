package net.ent.etnc.orbit.dtos.agent;

import java.util.List;

/**
 * Un processus lancé sur un poste, toutes ses instances regroupées sous son nom.
 *
 * @param nom     nom de l'exécutable (ex. notepad.exe)
 * @param nombre  nombre d'instances lancées
 * @param pids    PID de chaque instance
 * @param memoire mémoire cumulée des instances, en % de la RAM de la machine
 * @param cpu     CPU cumulé des instances, en %
 */
public record ProcessusLanceDto(String nom, int nombre, List<Long> pids, double memoire, double cpu) {}