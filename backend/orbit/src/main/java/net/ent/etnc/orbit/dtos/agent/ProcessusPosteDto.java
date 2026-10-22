package net.ent.etnc.orbit.dtos.agent;

import java.util.List;

/**
 * Processus lancés sur la machine (UC) d'un poste, vus par l'Agent ORBIT.
 *
 * @param nomMachine      nom machine de l'UC dans ORBIT
 * @param adresseMac      adresse MAC de l'UC dans ORBIT
 * @param adresseIp       IP d'où l'agent de cette MAC s'est annoncé
 * @param machineDetectee nom de la machine qui a répondu
 * @param os              système de la machine qui a répondu
 * @param ramGo           RAM de la machine qui a répondu, en Go
 * @param conforme        vrai si la machine qui répond est bien l'UC du poste (même MAC et même nom) ;
 *                        sinon la liste est vide et l'arrêt d'un processus est refusé
 * @param processus       processus lancés, regroupés par nom, triés par nom
 */
public record ProcessusPosteDto(String nomMachine, String adresseMac, String adresseIp, String machineDetectee,
                                String os, Double ramGo, boolean conforme, List<ProcessusLanceDto> processus) {}