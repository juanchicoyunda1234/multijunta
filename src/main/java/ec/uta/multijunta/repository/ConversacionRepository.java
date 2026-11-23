package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Conversacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findBySocioId(Long socioId);

    Optional<Conversacion> findByMiembroAIdAndMiembroBId(Long miembroAId, Long miembroBId);

    List<Conversacion> findByJuntaIdAndSocioIsNotNullOrderByActualizadaDesc(Long juntaId);

    @Query("select c from Conversacion c where c.miembroA.id = :usuarioId or c.miembroB.id = :usuarioId"
            + " order by c.actualizada desc")
    List<Conversacion> internasDe(@Param("usuarioId") Long usuarioId);
}
