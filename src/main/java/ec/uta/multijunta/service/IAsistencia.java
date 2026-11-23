package ec.uta.multijunta.service;

import java.util.List;

public interface IAsistencia {

    List<Inasistencia> inasistenciasDeEventosCerrados(Long juntaId);

    List<Inasistencia> inasistenciasDelEvento(Long juntaId, Long eventoId);

    List<EventoSinCerrar> eventosSinCerrar(Long juntaId);
}
