package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.EstadoSolicitud;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.SolicitudRecuperacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRecuperacionRepository extends JpaRepository<SolicitudRecuperacion, Long> {

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoSolicitud estado);

    List<SolicitudRecuperacion> findByJuntaIdAndEstadoAndUsuarioRolNotOrderByFechaSolicitudAsc(
            Long juntaId, EstadoSolicitud estado, Rol rol);

    List<SolicitudRecuperacion> findByEstadoAndUsuarioRolOrderByFechaSolicitudAsc(EstadoSolicitud estado, Rol rol);

    Optional<SolicitudRecuperacion> findByIdAndJuntaId(Long id, Long juntaId);

    long countByUsuarioId(Long usuarioId);
}
