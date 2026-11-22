package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoEvento;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {

    private final EventoRepository eventos;
    private final JuntaRepository juntas;
    private final UsuarioActual usuarioActual;

    public EventoService(EventoRepository eventos, JuntaRepository juntas, UsuarioActual usuarioActual) {
        this.eventos = eventos;
        this.juntas = juntas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Evento> delMes(YearMonth mes) {
        return eventos.findByJuntaIdAndFechaBetweenOrderByFechaAscHoraAsc(usuarioActual.juntaId(), mes.atDay(1),
                mes.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public Evento obtener(Long id) {
        return eventos.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional
    public Evento crear(DatosEvento datos) {
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        Evento evento = new Evento();
        evento.setJunta(junta);
        evento.setEstado(EstadoEvento.PROGRAMADO);
        aplicar(evento, datos);
        return eventos.save(evento);
    }

    @Transactional
    public Evento editar(Long id, DatosEvento datos) {
        Evento evento = modificable(id);
        aplicar(evento, datos);
        return eventos.save(evento);
    }

    @Transactional
    public void cancelar(Long id) {
        Evento evento = modificable(id);
        evento.setEstado(EstadoEvento.CANCELADO);
        eventos.save(evento);
    }

    public boolean admiteToma(Evento evento) {
        return evento.admiteToma(LocalDate.now());
    }

    private Evento modificable(Long id) {
        Evento evento = obtener(id);
        if (evento.isAsistenciaCerrada()) {
            throw new ReglaNegocioException("evento.cerrado");
        }
        return evento;
    }

    private void aplicar(Evento evento, DatosEvento datos) {
        evento.setTipo(datos.tipo());
        evento.setFecha(datos.fecha());
        evento.setHora(datos.hora());
        evento.setLugar(datos.lugar().trim());
        evento.setDescripcion(datos.descripcion() == null || datos.descripcion().isBlank()
                ? null : datos.descripcion().trim());
    }
}
