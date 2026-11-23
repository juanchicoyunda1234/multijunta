package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.AplicacionPago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicacionPagoRepository extends JpaRepository<AplicacionPago, Long> {

    List<AplicacionPago> findByPagoIdOrderByIdAsc(Long pagoId);
}
