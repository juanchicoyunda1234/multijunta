package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoObligacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    Optional<Cuota> findByIdAndJuntaId(Long id, Long juntaId);

    @Query("select c from Cuota c where c.junta.id = :juntaId and (:estado is null or c.estado = :estado)"
            + " and (:texto is null or lower(c.concepto) like lower(concat('%', :texto, '%'))"
            + " or c.socio.cedula like concat('%', :texto, '%')"
            + " or lower(concat(c.socio.nombres, ' ', c.socio.apellidos)) like lower(concat('%', :texto, '%')))"
            + " order by c.id desc")
    List<Cuota> buscar(@Param("juntaId") Long juntaId, @Param("texto") String texto,
            @Param("estado") EstadoObligacion estado);

    List<Cuota> findBySocioIdAndJuntaIdAndEstadoIn(Long socioId, Long juntaId, Collection<EstadoObligacion> estados);

    boolean existsBySocioIdAndConceptoNormalizadoAndEstadoNot(Long socioId, String conceptoNormalizado,
            EstadoObligacion estado);

    @Query("select c.socio.id from Cuota c where c.junta.id = :juntaId and c.conceptoNormalizado = :concepto"
            + " and c.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA")
    Set<Long> sociosConConcepto(@Param("juntaId") Long juntaId, @Param("concepto") String concepto);

    @Query("select distinct c.socio.id from Cuota c where c.junta.id = :juntaId and c.fechaVencimiento < :hoy"
            + " and c.saldoPendiente > 0 and c.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA")
    Set<Long> sociosConCuotasVencidas(@Param("juntaId") Long juntaId, @Param("hoy") LocalDate hoy);

    @Query("select coalesce(sum(c.saldoPendiente), 0) from Cuota c where c.junta.id = :juntaId"
            + " and c.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA")
    BigDecimal deudaDeJunta(@Param("juntaId") Long juntaId);

    @Query("select coalesce(sum(c.saldoPendiente), 0) from Cuota c where c.socio.id = :socioId"
            + " and c.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA")
    BigDecimal deudaDeSocio(@Param("socioId") Long socioId);

    @Query("select c.socio.id, sum(c.saldoPendiente) from Cuota c where c.junta.id = :juntaId"
            + " and c.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA group by c.socio.id")
    List<Object[]> deudaPorSocio(@Param("juntaId") Long juntaId);

    @Query("select c from Cuota c where c.junta.id = :juntaId and c.fechaVencimiento < :hoy"
            + " and c.fechaVencimiento <= :fin and c.saldoPendiente > 0"
            + " and c.estado <> ec.uta.multijunta.model.EstadoObligacion.ANULADA order by c.fechaVencimiento")
    List<Cuota> morosas(@Param("juntaId") Long juntaId, @Param("hoy") LocalDate hoy, @Param("fin") LocalDate fin);
}
