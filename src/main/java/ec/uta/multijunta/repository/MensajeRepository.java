package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.Mensaje;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByConversacionIdOrderByIdAsc(Long conversacionId);

    Optional<Mensaje> findFirstByConversacionIdOrderByIdDesc(Long conversacionId);

    @Query("select coalesce(max(m.id), 0) from Mensaje m where m.conversacion.id = :conversacionId")
    long ultimoId(@Param("conversacionId") Long conversacionId);

    @Query("select count(m) from Mensaje m where m.conversacion.socio.id = :socioId"
            + " and m.id > m.conversacion.ultimoLeidoA and m.remitente.id <> :socioId")
    long noLeidosDelSocio(@Param("socioId") Long socioId);

    @Query("select count(m) from Mensaje m where m.conversacion.junta.id = :juntaId and m.conversacion.socio is not null"
            + " and m.id > m.conversacion.ultimoLeidoB and m.remitente.id = m.conversacion.socio.id")
    long noLeidosDeSociosParaDirectiva(@Param("juntaId") Long juntaId);

    @Query("select count(m) from Mensaje m where ((m.conversacion.miembroA.id = :usuarioId and m.id > m.conversacion.ultimoLeidoA)"
            + " or (m.conversacion.miembroB.id = :usuarioId and m.id > m.conversacion.ultimoLeidoB))"
            + " and m.remitente.id <> :usuarioId")
    long noLeidosInternos(@Param("usuarioId") Long usuarioId);

    @Query("select count(m) from Mensaje m where m.conversacion.id = :conversacionId and m.id > :desde"
            + " and m.remitente.id <> :excluir")
    long noLeidosEn(@Param("conversacionId") Long conversacionId, @Param("desde") long desde,
            @Param("excluir") Long excluir);

    @Query("select count(m) from Mensaje m where m.conversacion.id = :conversacionId and m.id > :desde"
            + " and m.remitente.id = :remitente")
    long noLeidosDe(@Param("conversacionId") Long conversacionId, @Param("desde") long desde,
            @Param("remitente") Long remitente);
}
