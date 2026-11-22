package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Socio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    List<Socio> findAllByJuntaId(Long juntaId);

    Optional<Socio> findByIdAndJuntaId(Long id, Long juntaId);

    boolean existsByCodigoSocioAndJuntaId(String codigoSocio, Long juntaId);
}
