package ec.uta.multijunta.repository;

import ec.uta.multijunta.model.EstadoEvento;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.TipoEvento;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    Optional<Evento> findByIdAndJuntaId(Long id, Long juntaId);

    List<Evento> findByJuntaIdAndFechaBetweenOrderByFechaAscHoraAsc(Long juntaId, LocalDate desde, LocalDate hasta);

    List<Evento> findByJuntaIdAndTipoInOrderByFechaDescHoraDesc(Long juntaId, Collection<TipoEvento> tipos);

    long countByJuntaIdAndEstadoAndFechaBetween(Long juntaId, EstadoEvento estado, LocalDate desde, LocalDate hasta);

    long countByJuntaIdAndTipoInAndEstadoAndAsistenciaCerradaFalseAndFechaLessThanEqual(
            Long juntaId, Collection<TipoEvento> tipos, EstadoEvento estado, LocalDate hasta);
}
