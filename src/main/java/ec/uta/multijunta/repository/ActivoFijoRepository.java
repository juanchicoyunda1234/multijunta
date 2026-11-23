package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.ActivoFijo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivoFijoRepository extends JpaRepository<ActivoFijo, Long> {

    Optional<ActivoFijo> findByIdAndJuntaId(Long id, Long juntaId);

    List<ActivoFijo> findByJuntaIdOrderByIdDesc(Long juntaId);

    long countByJuntaIdAndCodigoStartingWith(Long juntaId, String prefijo);
}
