package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.SecuenciaIngreso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SecuenciaIngresoRepository extends JpaRepository<SecuenciaIngreso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SecuenciaIngreso> findByJuntaIdAndAnio(Long juntaId, int anio);
}
