package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Asistencia;
import ec.uta.multijunta.model.EstadoAsistencia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByEventoIdOrderBySocioApellidosAscSocioNombresAsc(Long eventoId);

    Optional<Asistencia> findByIdAndEventoJuntaId(Long id, Long juntaId);

    List<Asistencia> findByEventoJuntaIdAndEventoAsistenciaCerradaTrueAndEstadoOrderByEventoFechaAscSocioApellidosAsc(
            Long juntaId, EstadoAsistencia estado);

    List<Asistencia> findByEventoIdAndEventoJuntaIdAndEventoAsistenciaCerradaTrueAndEstadoOrderBySocioApellidosAsc(
            Long eventoId, Long juntaId, EstadoAsistencia estado);
}
