package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Acta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActaRepository extends JpaRepository<Acta, Long> {

    Optional<Acta> findByIdAndJuntaId(Long id, Long juntaId);

    Optional<Acta> findByIdAndJuntaIdAndPublicaSociosTrue(Long id, Long juntaId);

    List<Acta> findByJuntaIdOrderByFechaDescIdDesc(Long juntaId);

    List<Acta> findByJuntaIdAndPublicaSociosTrueOrderByFechaDescIdDesc(Long juntaId);
}
