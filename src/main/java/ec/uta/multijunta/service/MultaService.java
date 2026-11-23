package ec.uta.multijunta.service;

import ec.uta.multijunta.model.EstadoAsistencia;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MultaService {

    public record Resultado(int cantidad, BigDecimal total) {
    }

    private static final Locale ESPANOL = Locale.forLanguageTag("es");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IAsistencia asistencia;
    private final MultaRepository multas;
    private final SocioRepository socios;
    private final JuntaRepository juntas;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final MessageSource mensajes;
    private final UsuarioActual usuarioActual;

    public MultaService(IAsistencia asistencia, MultaRepository multas, SocioRepository socios, JuntaRepository juntas,
            UsuarioRepository usuarios, NotificacionService notificaciones, MessageSource mensajes,
            UsuarioActual usuarioActual) {
        this.asistencia = asistencia;
        this.multas = multas;
        this.socios = socios;
        this.juntas = juntas;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.mensajes = mensajes;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Multa> listar() {
        return multas.findByJuntaIdOrderByFechaDescIdDesc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public List<Inasistencia> pendientes() {
        Long juntaId = usuarioActual.juntaId();
        Set<Long> conMulta = multas.asistenciasConMulta(juntaId);
        return asistencia.inasistenciasDeEventosCerrados(juntaId).stream()
                .filter(inasistencia -> !conMulta.contains(inasistencia.asistenciaId()))
                .sorted(Comparator.comparing(Inasistencia::fecha).reversed()
                        .thenComparing(Inasistencia::nombreCompleto))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventoSinCerrar> eventosSinCerrar() {
        return asistencia.eventosSinCerrar(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public BigDecimal montoEstandar() {
        BigDecimal monto = junta().getMontoMultaInasistencia();
        return monto == null ? new BigDecimal("0.00") : monto.setScale(2);
    }

    @Transactional
    public Resultado aprobar(List<Long> seleccion, Map<Long, BigDecimal> montos) {
        if (seleccion == null || seleccion.isEmpty()) {
            throw new ReglaNegocioException("multa.seleccion");
        }
        Long juntaId = usuarioActual.juntaId();
        Map<Long, Inasistencia> disponibles = asistencia.inasistenciasDeEventosCerrados(juntaId).stream()
                .collect(Collectors.toMap(Inasistencia::asistenciaId, Function.identity()));
        Junta junta = junta();
        List<Multa> nuevas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Long asistenciaId : new LinkedHashSet<>(seleccion)) {
            Inasistencia inasistencia = disponibles.get(asistenciaId);
            if (inasistencia == null) {
                throw new RecursoNoEncontradoException();
            }
            if (multas.existsByAsistenciaId(asistenciaId)) {
                throw new ReglaNegocioException("multa.duplicada");
            }
            BigDecimal monto = montos.get(asistenciaId);
            Montos.validar(monto, null);
            monto = Montos.normalizar(monto);
            Socio socio = socios.findByIdAndJuntaId(inasistencia.socioId(), juntaId)
                    .orElseThrow(RecursoNoEncontradoException::new);
            Multa multa = new Multa();
            multa.setJunta(junta);
            multa.setSocio(socio);
            multa.setAsistenciaId(asistenciaId);
            multa.setEventoId(inasistencia.eventoId());
            multa.setTipoEvento(inasistencia.tipoEvento());
            multa.setFechaEvento(inasistencia.fecha());
            multa.setMotivo(mensajes.getMessage("multa.motivo", new Object[] {inasistencia.tipoEvento().getEtiqueta(),
                    inasistencia.fecha().format(FECHA)}, ESPANOL));
            multa.setMonto(monto);
            multa.setSaldoPendiente(monto);
            multa.setFecha(LocalDate.now());
            nuevas.add(multa);
            total = total.add(monto);
        }
        multas.saveAll(nuevas);
        String titulo = mensajes.getMessage("notificacion.multa.titulo", null, ESPANOL);
        nuevas.stream()
                .filter(multa -> multa.getSocio().isNotificable())
                .forEach(multa -> notificaciones.crear(multa.getSocio(), titulo,
                        multa.getMotivo() + " · $" + Montos.texto(multa.getMonto()), CuotaService.ENLACE_ESTADO_CUENTA));
        return new Resultado(nuevas.size(), total);
    }

    @EventListener
    public void asistenciaCorregida(AsistenciaCorregida evento) {
        if (evento.estadoAnterior() != EstadoAsistencia.AUSENTE || evento.estadoNuevo() == EstadoAsistencia.AUSENTE) {
            return;
        }
        multas.findByAsistenciaIdAndJuntaIdAndEstadoNot(evento.asistenciaId(), evento.juntaId(), EstadoObligacion.ANULADA)
                .ifPresent(multa -> {
                    if (multa.tienePagosVigentes()) {
                        String titulo = mensajes.getMessage("notificacion.multaPagadaCorregida.titulo", null, ESPANOL);
                        String detalle = multa.getSocio().getNombreCompleto() + " · " + multa.getMotivo()
                                + ": revise y reverse el pago para tratar la multa";
                        usuarios.findByJuntaIdAndRolAndActivoTrue(evento.juntaId(), Rol.TESORERO)
                                .forEach(tesorero -> notificaciones.crear(tesorero, titulo, detalle, "/multas"));
                    } else {
                        multa.anular(mensajes.getMessage("multa.anuladaCorreccion", null, ESPANOL));
                        multas.save(multa);
                    }
                });
    }

    private Junta junta() {
        return juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }
}
