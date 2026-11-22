package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Sector;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    List<Sector> findAllByJuntaId(Long juntaId);

    Optional<Sector> findByIdAndJuntaId(Long id, Long juntaId);
}
