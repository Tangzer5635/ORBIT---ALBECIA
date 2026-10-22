package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.agent.ProcessusLanceDto;
import net.ent.etnc.orbit.dtos.agent.ProcessusPosteDto;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto.ProcessusAgent;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.services.AgentService;
import net.ent.etnc.orbit.services.AgentService.ResultatArret;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.ProcessusLanceService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lit et arrête les processus d'un poste en passant par l'Agent ORBIT de son UC.
 * L'UC est identifiée par son adresse MAC : l'agent est connecté au back par WebSocket ({@link AgentService}),
 * le back lit la dernière liste qu'il a envoyée et lui transmet les ordres d'arrêt par la même connexion.
 *
 * <p>Pas de @Transactional : on ne garde pas une connexion à la base pendant l'attente de l'agent.
 */
@Service
public class ProcessusLanceServiceImpl implements ProcessusLanceService {

    private final PosteService posteService;
    private final AgentService agentService;
    private final JournalisationService journalisationService;

    @Autowired
    public ProcessusLanceServiceImpl(PosteService posteService, AgentService agentService,
                                     JournalisationService journalisationService) {
        this.posteService = posteService;
        this.agentService = agentService;
        this.journalisationService = journalisationService;
    }

    @Override
    public ProcessusPosteDto lister(Long idPoste) throws ServiceException {
        UC uc = ucDuPoste(trouverPoste(idPoste));
        ReponseAgentDto reponse = agentService.etat(uc.getAdresseMac());
        String ip = agentService.adresseIp(uc.getAdresseMac());
        boolean conforme = estConforme(uc, reponse);
        // machine non conforme : on ne montre pas les processus d'une autre machine
        List<ProcessusLanceDto> processus = conforme ? regrouper(processusDe(reponse)) : List.of();
        return new ProcessusPosteDto(uc.getNomMachine(), uc.getAdresseMac(), ip, reponse.host(), reponse.os(),
                reponse.ramGo(), conforme, processus);
    }

    @Override
    public int tuer(Long idPoste, String nom) throws ServiceException {
        if (nom == null || nom.isBlank())
            throw new ServiceException("Le nom du processus est obligatoire");
        Poste poste = trouverPoste(idPoste);
        UC uc = ucDuPoste(poste);
        ReponseAgentDto reponse = agentService.etat(uc.getAdresseMac());
        if (!estConforme(uc, reponse))
            throw new ServiceException("La machine qui répond (" + reponse.host() + ", " + reponse.mac()
                    + ") n'est pas l'UC " + uc.getNomMachine() + " du " + Libelles.poste(poste) + " : arrêt refusé");

        List<Long> pids = new ArrayList<>();
        for (ProcessusAgent p : processusDe(reponse)) {
            if (nom.trim().equalsIgnoreCase(p.name())) pids.add(p.pid());
        }
        if (pids.isEmpty())
            throw new ServiceException("Aucun processus " + nom + " n'est lancé sur le " + Libelles.poste(poste));

        int arretes = 0;
        List<String> echecs = new ArrayList<>();
        for (Long pid : pids) {
            ResultatArret resultat;
            try {
                resultat = agentService.tuer(uc.getAdresseMac(), pid);
            } catch (ServiceException e) {
                echecs.add("PID " + pid + " : " + e.getMessage());
                continue;
            }
            // 404 : l'instance s'est déjà fermée (ex. fermée avec la fenêtre principale)
            if (resultat.ok() || resultat.statut() == 404) arretes++;
            else echecs.add("PID " + pid + " : " + resultat.erreur());
        }

        if (arretes > 0)
            journalisationService.journaliser(TypeAction.SUPPRESSION, "Processus " + nom.trim() + " arrêté sur le "
                    + Libelles.poste(poste) + " (" + uc.getNomMachine() + ", " + arretes + " instance(s))");
        if (!echecs.isEmpty())
            throw new ServiceException(arretes + " instance(s) de " + nom.trim() + " sur " + pids.size()
                    + " arrêtée(s). Échecs : " + String.join(" ; ", echecs));
        return arretes;
    }

    private Poste trouverPoste(Long idPoste) throws ServiceException {
        return posteService.findByIdAvecDetails(idPoste)
                .orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
    }

    /** La machine du poste : sa première UC non archivée, qui doit avoir une adresse MAC. */
    private UC ucDuPoste(Poste poste) throws ServiceException {
        for (Materiel materiel : poste.getMateriels()) {
            // unproxy : un matériel chargé en LAZY est un proxy de Materiel, pas de UC
            if (!materiel.isArchive() && Hibernate.unproxy(materiel) instanceof UC uc) {
                if (uc.getAdresseMac() == null)
                    throw new ServiceException("L'UC " + uc.getNumSerie() + " du " + Libelles.poste(poste)
                            + " n'a pas d'adresse MAC : renseignez-la avec « Modifier » sur sa fiche");
                return uc;
            }
        }
        throw new ServiceException("Aucune UC sur le " + Libelles.poste(poste) + " : pas de processus à afficher");
    }

    /**
     * La machine qui répond est bien l'UC du poste : même adresse MAC (c'est la clé de l'agent, mais l'UC a pu être
     * modifiée depuis) et même nom machine, sans tenir compte de la casse.
     */
    private boolean estConforme(UC uc, ReponseAgentDto reponse) {
        boolean memeMac = uc.getAdresseMac().equals(UC.normaliserMac(reponse.mac()));
        boolean memeNom = uc.getNomMachine() != null && uc.getNomMachine().trim().equalsIgnoreCase(reponse.host());
        return memeMac && memeNom;
    }

    /** Une ligne par nom de processus, triées par nom ; les instances sont cumulées. */
    private List<ProcessusLanceDto> regrouper(List<ProcessusAgent> processus) {
        Map<String, List<ProcessusAgent>> parNom = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ProcessusAgent p : processus) {
            String nom = p.name() == null || p.name().isBlank() ? "(inconnu)" : p.name();
            parNom.computeIfAbsent(nom, n -> new ArrayList<>()).add(p);
        }

        List<ProcessusLanceDto> groupes = new ArrayList<>();
        for (Map.Entry<String, List<ProcessusAgent>> entree : parNom.entrySet()) {
            List<Long> pids = new ArrayList<>();
            double memoire = 0;
            double cpu = 0;
            for (ProcessusAgent p : entree.getValue()) {
                pids.add(p.pid());
                memoire += p.memoryPercent();
                cpu += p.cpuPercent();
            }
            groupes.add(new ProcessusLanceDto(entree.getKey(), pids.size(), pids, arrondi(memoire), arrondi(cpu)));
        }
        return groupes;
    }

    /** Sans processus (liste absente du JSON) : liste vide. */
    private List<ProcessusAgent> processusDe(ReponseAgentDto reponse) {
        return reponse.processes() == null ? List.of() : reponse.processes();
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }
}