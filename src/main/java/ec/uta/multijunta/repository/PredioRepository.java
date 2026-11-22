package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Predio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredioRepository extends JpaRepository<Predio, Long> {

    List<Predio> findBySocioIdAndJuntaIdOrderByIdAsc(Long socioId, Long juntaId);

    Optional<Predio> findByIdAndJuntaId(Long id, Long juntaId);

    boolean existsByJuntaIdAndCodigo(Long juntaId, String codigo);

    long countByJuntaId(Long juntaId);

    boolean existsBySectorId(Long sectorId);
}
