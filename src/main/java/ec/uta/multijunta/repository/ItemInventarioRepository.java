package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.ItemInventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemInventarioRepository extends JpaRepository<ItemInventario, Long> {

    Optional<ItemInventario> findByIdAndJuntaId(Long id, Long juntaId);

    List<ItemInventario> findByJuntaIdOrderByNombreAsc(Long juntaId);

    boolean existsByJuntaIdAndNombreIgnoreCase(Long juntaId, String nombre);

    long countByJuntaId(Long juntaId);

    @Query("select count(i) from ItemInventario i where i.junta.id = :juntaId"
            + " and i.estado = ec.uta.multijunta.model.EstadoItem.ACTIVO and i.stock <= i.stockMinimo")
    long contarStockBajo(@Param("juntaId") Long juntaId);
}
