package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Pago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByIdAndJuntaId(Long id, Long juntaId);

    long countByJuntaId(Long juntaId);

    List<Pago> findTop10BySocioIdOrderByFechaDescIdDesc(Long socioId);

    @Query("select p from Pago p where p.junta.id = :juntaId and (:texto is null or p.numero like concat('%', :texto, '%')"
            + " or p.socio.cedula like concat('%', :texto, '%')"
            + " or lower(concat(p.socio.nombres, ' ', p.socio.apellidos)) like lower(concat('%', :texto, '%')))"
            + " order by p.fecha desc, p.id desc")
    List<Pago> buscar(@Param("juntaId") Long juntaId, @Param("texto") String texto);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.junta.id = :juntaId and p.reversado = false")
    BigDecimal ingresosVigentes(@Param("juntaId") Long juntaId);

    List<Pago> findByJuntaIdAndFechaBetweenOrderByFechaAsc(Long juntaId, LocalDateTime desde, LocalDateTime hasta);
}
