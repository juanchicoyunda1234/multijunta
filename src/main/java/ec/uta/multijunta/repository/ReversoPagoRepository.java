package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.ReversoPago;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReversoPagoRepository extends JpaRepository<ReversoPago, Long> {

    long countByJuntaId(Long juntaId);

    Optional<ReversoPago> findByPagoId(Long pagoId);

    List<ReversoPago> findByJuntaIdAndFechaBetweenOrderByFechaAsc(Long juntaId, LocalDateTime desde,
            LocalDateTime hasta);
}
