package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Junta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuntaRepository extends JpaRepository<Junta, Long> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);

    List<Junta> findAllByOrderByNombreAsc();
}
