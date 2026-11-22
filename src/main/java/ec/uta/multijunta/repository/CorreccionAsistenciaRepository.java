package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.CorreccionAsistencia;
import ec.uta.multijunta.model.EstadoCorreccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreccionAsistenciaRepository extends JpaRepository<CorreccionAsistencia, Long> {

    List<CorreccionAsistencia> findByJuntaIdOrderByFechaSolicitudDesc(Long juntaId);

    List<CorreccionAsistencia> findByAsistenciaEventoIdOrderByFechaSolicitudDesc(Long eventoId);

    Optional<CorreccionAsistencia> findByIdAndJuntaId(Long id, Long juntaId);

    boolean existsByAsistenciaIdAndEstado(Long asistenciaId, EstadoCorreccion estado);
}
