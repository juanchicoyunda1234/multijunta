package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Recurso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findByJuntaIdOrderByNombreAsc(Long juntaId);

    List<Recurso> findByJuntaIdAndActivoTrueOrderByNombreAsc(Long juntaId);

    Optional<Recurso> findByIdAndJuntaId(Long id, Long juntaId);

    boolean existsByJuntaIdAndNombreIgnoreCase(Long juntaId, String nombre);

    boolean existsByJuntaIdAndNombreIgnoreCaseAndIdNot(Long juntaId, String nombre, Long id);

    boolean existsBySectorId(Long sectorId);
}
