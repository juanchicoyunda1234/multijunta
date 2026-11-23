package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Egreso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EgresoRepository extends JpaRepository<Egreso, Long> {

    Optional<Egreso> findByIdAndJuntaId(Long id, Long juntaId);

    List<Egreso> findByJuntaIdOrderByFechaDescIdDesc(Long juntaId);

    List<Egreso> findByJuntaIdAndFechaBetweenOrderByFechaAscIdAsc(Long juntaId, LocalDate desde, LocalDate hasta);

    @Query("select coalesce(sum(e.monto), 0) from Egreso e where e.junta.id = :juntaId")
    BigDecimal totalDeJunta(@Param("juntaId") Long juntaId);
}
