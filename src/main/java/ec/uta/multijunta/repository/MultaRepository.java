package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Multa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MultaRepository extends JpaRepository<Multa, Long> {

    Optional<Multa> findByIdAndJuntaId(Long id, Long juntaId);

    List<Multa> findByJuntaIdOrderByFechaDescIdDesc(Long juntaId);

    List<Multa> findBySocioIdAndJuntaIdAndEstadoIn(Long socioId, Long juntaId, Collection<EstadoObligacion> estados);

    boolean existsByAsistenciaId(Long asistenciaId);

    Optional<Multa> findByAsistenciaIdAndJuntaIdAndEstadoNot(Long asistenciaId, Long juntaId, EstadoObligacion estado);

    @Query("select m.asistenciaId from Multa m where m.junta.id = :juntaId")
    Set<Long> asistenciasConMulta(@Param("juntaId") Long juntaId);

    @Query("select coalesce(sum(m.saldoPendiente), 0) from Multa m where m.junta.id = :juntaId"
            + " and m.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA")
    BigDecimal deudaDeJunta(@Param("juntaId") Long juntaId);

    @Query("select coalesce(sum(m.saldoPendiente), 0) from Multa m where m.socio.id = :socioId"
            + " and m.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA")
    BigDecimal deudaDeSocio(@Param("socioId") Long socioId);

    @Query("select m.socio.id, sum(m.saldoPendiente) from Multa m where m.junta.id = :juntaId"
            + " and m.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA group by m.socio.id")
    List<Object[]> deudaPorSocio(@Param("juntaId") Long juntaId);

    @Query("select m from Multa m where m.junta.id = :juntaId and m.fecha <= :fin and m.saldoPendiente > 0"
            + " and m.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA order by m.fecha")
    List<Multa> morosas(@Param("juntaId") Long juntaId, @Param("fin") LocalDate fin);
}
