package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Socio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    List<Socio> findAllByJuntaId(Long juntaId);

    Optional<Socio> findByIdAndJuntaId(Long id, Long juntaId);

    boolean existsByCodigoSocioAndJuntaId(String codigoSocio, Long juntaId);

    long countByJuntaIdAndEstado(Long juntaId, EstadoSocio estado);

    long countByJuntaId(Long juntaId);

    boolean existsBySectorId(Long sectorId);

    List<Socio> findAllByJuntaIdOrderByApellidosAscNombresAsc(Long juntaId);

    List<Socio> findAllByJuntaIdAndEstadoOrderByApellidosAscNombresAsc(Long juntaId, EstadoSocio estado);

    List<Socio> findBySectorIdAndTieneCuentaTrueAndActivoTrue(Long sectorId);

    List<Socio> findAllByJuntaIdAndSectorIdAndEstadoOrderByApellidosAscNombresAsc(Long juntaId, Long sectorId,
            EstadoSocio estado);

    @Query("select s from Socio s where s.junta.id = :juntaId and (:texto = ''"
            + " or s.cedula like concat('%', :texto, '%')"
            + " or lower(concat(s.nombres, ' ', s.apellidos)) like concat('%', :texto, '%'))"
            + " order by s.id desc")
    List<Socio> buscar(@Param("juntaId") Long juntaId, @Param("texto") String texto);
}
