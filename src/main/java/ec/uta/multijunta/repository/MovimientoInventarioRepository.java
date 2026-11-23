package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.MovimientoInventario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByItemIdOrderByFechaAscIdAsc(Long itemId);

    boolean existsByItemId(Long itemId);
}
