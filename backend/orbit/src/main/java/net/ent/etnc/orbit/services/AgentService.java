package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.agent.MessageAgentDto;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto.ProcessusAgent;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.List;
import java.util.function.Consumer;

/**
 * Agents ORBIT connectés. Chaque agent ouvre un WebSocket vers le back (via Nginx), se présente avec l'adresse MAC
 * de sa machine, puis envoie sa liste de processus quand elle change. Le back retient le dernier état reçu et
 * écrit à l'agent (ordres d'arrêt) par la même connexion : rien n'est appelé sur la machine, aucun port à ouvrir.
 * Rien n'est stocké en base : après un redémarrage du back, les agents se reconnectent d'eux-mêmes.
 */
public interface AgentService {

    /** Réponse d'un agent à un ordre d'arrêt : 200 arrêté, 403 refusé, 404 processus déjà fermé. */
    record ResultatArret(boolean ok, int statut, String erreur) {}

    /**
     * Vérifie le token de l'agent à l'ouverture de la connexion.
     *
     * @param token en-tête X-Token envoyé par l'agent (doit être agent.token)
     */
    void verifierToken(String token) throws ServiceException;

    /**
     * Enregistre (ou remplace) l'agent de cette machine, quand il se présente.
     *
     * @param mac   adresse MAC de la machine
     * @param envoi comment écrire à cet agent (la connexion WebSocket)
     */
    void connecter(String mac, String nomMachine, String ip, String os, Double ramGo,
                   Consumer<MessageAgentDto> envoi) throws ServiceException;

    /** La connexion est fermée ; sans effet si cette machine s'est déjà reconnectée avec une autre connexion. */
    void deconnecter(String mac, Consumer<MessageAgentDto> envoi);

    /** Nouvelle liste de processus envoyée par l'agent (sert aussi à savoir qu'il est toujours là). */
    void recevoirProcessus(String mac, List<ProcessusAgent> processus);

    /** Réponse d'un agent à l'ordre d'arrêt portant cet identifiant. */
    void recevoirResultat(String id, ResultatArret resultat);

    /** Dernier état connu de la machine ; erreur si l'agent n'est pas connecté ou ne donne plus de nouvelles. */
    ReponseAgentDto etat(String mac) throws ServiceException;

    /** IP de la machine de cet agent, telle qu'il l'a donnée en se présentant. */
    String adresseIp(String mac) throws ServiceException;

    /** Ordonne à l'agent d'arrêter ce processus et attend sa réponse (5 s au plus). */
    ResultatArret tuer(String mac, long pid) throws ServiceException;

    /**
     * Ordonne à l'agent d'arrêter ce processus sans attendre sa réponse (arrêt automatique de la liste noire).
     * Appelé depuis la connexion de l'agent : attendre bloquerait la lecture de sa réponse sur cette même connexion.
     */
    void ordonnerArret(String mac, long pid) throws ServiceException;
}