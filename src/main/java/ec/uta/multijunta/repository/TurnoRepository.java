package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Turno;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByJuntaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(Long juntaId, LocalDate desde, LocalDate hasta);

    Optional<Turno> findByIdAndJuntaId(Long id, Long juntaId);

    long countByJuntaIdAndFechaBetween(Long juntaId, LocalDate desde, LocalDate hasta);

    boolean existsBySectorId(Long sectorId);

    @Query("select count(t) > 0 from Turno t where t.recurso.id = :recursoId and t.fecha = :fecha"
            + " and t.horaInicio < :fin and t.horaFin > :inicio and (:excluirId is null or t.id <> :excluirId)")
    boolean existeConflicto(@Param("recursoId") Long recursoId, @Param("fecha") LocalDate fecha,
            @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin, @Param("excluirId") Long excluirId);
}
