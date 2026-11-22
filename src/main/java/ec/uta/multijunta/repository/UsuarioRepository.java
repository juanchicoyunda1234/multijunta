package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByCedula(String cedula);

    Optional<Usuario> findByIdAndJuntaId(Long id, Long juntaId);

    List<Usuario> findByJuntaIdAndRolAndActivoTrue(Long juntaId, Rol rol);

    List<Usuario> findByRolAndActivoTrue(Rol rol);

    boolean existsByCedula(String cedula);

    boolean existsByEmailIgnoreCaseAndRolIn(String email, Collection<Rol> roles);

    boolean existsByEmailIgnoreCaseAndRolInAndIdNot(String email, Collection<Rol> roles, Long id);

    long countByJuntaIdAndRolAndActivoTrue(Long juntaId, Rol rol);

    List<Usuario> findByJuntaIdAndRolInOrderByApellidosAsc(Long juntaId, Collection<Rol> roles);
}
