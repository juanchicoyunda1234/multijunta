package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.SocioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuotaService {

    public static final String ENLACE_ESTADO_CUENTA = "/mi-informacion/estado-cuenta";

    private static final Locale ESPANOL = Locale.forLanguageTag("es");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Previa(List<Socio> destinatarios, int omitidos, BigDecimal total) {
    }

    private final CuotaRepository cuotas;
    private final SocioRepository socios;
    private final SectorRepository sectores;
    private final JuntaRepository juntas;
    private final UsuarioActual usuarioActual;
    private final NotificacionService notificaciones;
    private final MessageSource mensajes;

    public CuotaService(CuotaRepository cuotas, SocioRepository socios, SectorRepository sectores,
            JuntaRepository juntas, UsuarioActual usuarioActual, NotificacionService notificaciones,
            MessageSource mensajes) {
        this.cuotas = cuotas;
        this.socios = socios;
        this.sectores = sectores;
        this.juntas = juntas;
        this.usuarioActual = usuarioActual;
        this.notificaciones = notificaciones;
        this.mensajes = mensajes;
    }

    @Transactional(readOnly = true)
    public List<Cuota> buscar(String texto, EstadoObligacion estado) {
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        return cuotas.buscar(usuarioActual.juntaId(), filtro, estado);
    }

    @Transactional(readOnly = true)
    public List<Sector> sectores() {
        return sectores.findAllByJuntaIdOrderByNombreAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public List<Socio> socios() {
        return socios.findAllByJuntaIdOrderByApellidosAscNombresAsc(usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Previa previa(DatosCuota datos) {
        Montos.validar(datos.monto(), "monto");
        Long juntaId = usuarioActual.juntaId();
        String concepto = Cuota.normalizar(datos.concepto());
        Set<Long> yaTienen = cuotas.sociosConConcepto(juntaId, concepto);
        List<Socio> candidatos = candidatos(datos, juntaId);
        if (datos.destino() == DestinoCuota.SOCIO && yaTienen.contains(candidatos.get(0).getId())) {
            throw new ReglaNegocioException("cuota.duplicada", "socioId");
        }
        List<Socio> destinatarios = candidatos.stream().filter(socio -> !yaTienen.contains(socio.getId())).toList();
        if (destinatarios.isEmpty()) {
            throw new ReglaNegocioException("cuota.ninguna", "destino");
        }
        BigDecimal total = Montos.normalizar(datos.monto()).multiply(BigDecimal.valueOf(destinatarios.size()));
        return new Previa(destinatarios, candidatos.size() - destinatarios.size(), total);
    }

    @Transactional
    public Previa emitir(DatosCuota datos) {
        Previa previa = previa(datos);
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        BigDecimal monto = Montos.normalizar(datos.monto());
        for (Socio socio : previa.destinatarios()) {
            Cuota cuota = new Cuota();
            cuota.setJunta(junta);
            cuota.setSocio(socio);
            cuota.setConcepto(datos.concepto());
            cuota.setMonto(monto);
            cuota.setSaldoPendiente(monto);
            cuota.setFecha(LocalDate.now());
            cuota.setFechaVencimiento(datos.fechaVencimiento());
            cuotas.save(cuota);
            if (socio.isNotificable()) {
                notificaciones.crear(socio, mensajes.getMessage("notificacion.cuota.titulo", null, ESPANOL),
                        cuota.getConcepto() + " · $" + Montos.texto(monto) + " · vence "
                                + cuota.getFechaVencimiento().format(FECHA), ENLACE_ESTADO_CUENTA);
            }
        }
        return previa;
    }

    @Transactional
    public void anular(Long id, String motivo) {
        Cuota cuota = cuotas.findByIdAndJuntaId(id, usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("motivo.obligatorio");
        }
        if (!cuota.isCobrable() || cuota.tienePagosVigentes()) {
            throw new ReglaNegocioException("cuota.conPagos");
        }
        cuota.anular(motivo.trim());
        cuotas.save(cuota);
    }

    private List<Socio> candidatos(DatosCuota datos, Long juntaId) {
        return switch (datos.destino()) {
            case TODOS -> socios.findAllByJuntaIdAndEstadoOrderByApellidosAscNombresAsc(juntaId, EstadoSocio.ACTIVO);
            case SECTOR -> {
                Sector sector = sectores.findByIdAndJuntaId(requerido(datos.sectorId(), "sectorId"), juntaId)
                        .orElseThrow(RecursoNoEncontradoException::new);
                yield socios.findAllByJuntaIdAndSectorIdAndEstadoOrderByApellidosAscNombresAsc(juntaId,
                        sector.getId(), EstadoSocio.ACTIVO);
            }
            case SOCIO -> List.of(socios.findByIdAndJuntaId(requerido(datos.socioId(), "socioId"), juntaId)
                    .orElseThrow(RecursoNoEncontradoException::new));
        };
    }

    private Long requerido(Long valor, String campo) {
        if (valor == null) {
            throw new ReglaNegocioException("campo.obligatorio", campo);
        }
        return valor;
    }
}
