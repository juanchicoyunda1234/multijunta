package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    Optional<Convocatoria> findByIdAndJuntaId(Long id, Long juntaId);

    List<Convocatoria> findByJuntaIdOrderByFechaPublicacionDescIdDesc(Long juntaId);

    List<Convocatoria> findByJuntaIdAndEstadoOrderByFechaPublicacionDescIdDesc(Long juntaId, EstadoConvocatoria estado);

    List<Convocatoria> findByEstadoAndFechaPublicacionLessThanEqual(EstadoConvocatoria estado, LocalDateTime hasta);
}
