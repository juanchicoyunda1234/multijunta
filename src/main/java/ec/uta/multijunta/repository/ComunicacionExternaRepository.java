package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.ComunicacionExterna;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicacionExternaRepository extends JpaRepository<ComunicacionExterna, Long> {

    Optional<ComunicacionExterna> findByIdAndJuntaId(Long id, Long juntaId);

    List<ComunicacionExterna> findByJuntaIdOrderByFechaDescIdDesc(Long juntaId);
}
