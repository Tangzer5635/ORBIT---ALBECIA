package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import net.ent.etnc.orbit.repositories.projections.ComptesEtats;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.repositories.projections.StockageCompte;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BatimentRepository extends BaseRepository<Batiment> {
    boolean existsBySalles_Id(Long idSalle);

    @Query("select " + IdCompte.NEW + "(b.id, count(s)) from Batiment b join b.salles s"
            + " where b.id in :ids and s.type = net.ent.etnc.orbit.models.enums.TypeSalle.SalleDeCours"
            + " group by b.id")
    List<IdCompte> nbSallesDeCours(@Param("ids") Collection<Long> ids);

    /** Salles de stockage des bâtiments avec leur nombre de matériels (count en base). */
    @Query("select " + StockageCompte.NEW + "(b.id, s.id, s.numSalle, count(m))"
            + " from Batiment b join b.salles s left join s.materiels m"
            + " where b.id in :ids and s.type = net.ent.etnc.orbit.models.enums.TypeSalle.SalleStockage"
            + " group by b.id, s.id, s.numSalle order by s.id")
    List<StockageCompte> stockages(@Param("ids") Collection<Long> ids);

    /** Matériel posé directement dans une salle (stockage compris). */
    @Query("select " + ComptesEtats.NEW + "(b.id, " + ComptesEtats.COLONNES + ")"
            + " from Batiment b join b.salles s join s.materiels m"
            + " where b.id in :ids group by b.id")
    List<ComptesEtats> comptesEtatsEnSalle(@Param("ids") Collection<Long> ids, @Param("seuil") LocalDate seuil);

    /** Matériel affecté aux postes des salles. */
    @Query("select " + ComptesEtats.NEW + "(b.id, " + ComptesEtats.COLONNES + ")"
            + " from Batiment b join b.salles s join s.postes p join p.materiels m"
            + " where b.id in :ids group by b.id")
    List<ComptesEtats> comptesEtatsSurPoste(@Param("ids") Collection<Long> ids, @Param("seuil") LocalDate seuil);
}