package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Convocatoria;
import ec.uta.multijunta.model.EstadoConvocatoria;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Obligacion;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Turno;
import ec.uta.multijunta.repository.ConvocatoriaRepository;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiInformacionService {

    public record Resumen(Socio socio, BigDecimal deuda, Optional<Turno> proximoTurno, List<Convocatoria> convocatorias) {
    }

    public record EstadoCuenta(Socio socio, BigDecimal deuda, List<Obligacion> obligaciones, List<Pago> pagos) {

        public boolean isAlDia() {
            return deuda.signum() == 0;
        }
    }

    public static final int SEMANAS_CALENDARIO = 4;
    public static final int CONVOCATORIAS_RESUMEN = 5;

    private static final Set<EstadoObligacion> COBRABLES = Set.of(EstadoObligacion.PENDIENTE, EstadoObligacion.PARCIAL);

    private final SocioRepository socios;
    private final MultaRepository multas;
    private final CuotaRepository cuotas;
    private final PagoRepository pagos;
    private final PredioRepository predios;
    private final TurnoRepository turnos;
    private final ConvocatoriaRepository convocatorias;
    private final FinanzasService finanzas;
    private final ArchivoService archivos;
    private final UsuarioActual usuarioActual;

    public MiInformacionService(SocioRepository socios, MultaRepository multas, CuotaRepository cuotas,
            PagoRepository pagos, PredioRepository predios, TurnoRepository turnos,
            ConvocatoriaRepository convocatorias, FinanzasService finanzas, ArchivoService archivos,
            UsuarioActual usuarioActual) {
        this.socios = socios;
        this.multas = multas;
        this.cuotas = cuotas;
        this.pagos = pagos;
        this.predios = predios;
        this.turnos = turnos;
        this.convocatorias = convocatorias;
        this.finanzas = finanzas;
        this.archivos = archivos;
        this.usuarioActual = usuarioActual;
    }

    public static List<Convocatoria> ordenarAnuncios(List<Convocatoria> publicadas) {
        LocalDate hoy = LocalDate.now();
        return publicadas.stream()
                .sorted(Comparator.comparing((Convocatoria c) -> !c.fijada(hoy))
                        .thenComparing(Convocatoria::getFechaPublicacion, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Socio socio() {
        return socios.findByIdAndJuntaId(usuarioActual.id(), usuarioActual.juntaId())
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        Socio socio = socio();
        List<Turno> proximos = turnosDe(socio);
        List<Convocatoria> anuncios = ordenarAnuncios(convocatorias
                .findByJuntaIdAndEstadoOrderByFechaPublicacionDescIdDesc(usuarioActual.juntaId(),
                        EstadoConvocatoria.PUBLICADA));
        return new Resumen(socio, finanzas.deudaDeSocio(socio.getId()), proximos.stream().findFirst(),
                anuncios.stream().limit(CONVOCATORIAS_RESUMEN).toList());
    }

    @Transactional(readOnly = true)
    public EstadoCuenta estadoCuenta() {
        Socio socio = socio();
        Long juntaId = usuarioActual.juntaId();
        List<Obligacion> obligaciones = new ArrayList<>();
        obligaciones.addAll(multas.findBySocioIdAndJuntaIdAndEstadoIn(socio.getId(), juntaId, COBRABLES));
        obligaciones.addAll(cuotas.findBySocioIdAndJuntaIdAndEstadoIn(socio.getId(), juntaId, COBRABLES));
        obligaciones.sort(Comparator.comparing(Obligacion::getFechaOrden));
        List<Pago> ultimos = pagos.findTop10BySocioIdOrderByFechaDescIdDesc(socio.getId());
        ultimos.forEach(pago -> pago.getAplicaciones().size());
        return new EstadoCuenta(socio, finanzas.deudaDeSocio(socio.getId()), obligaciones, ultimos);
    }

    @Transactional(readOnly = true)
    public List<Predio> predios() {
        return predios.findBySocioIdAndJuntaIdOrderByIdAsc(usuarioActual.id(), usuarioActual.juntaId());
    }

    @Transactional(readOnly = true)
    public Path foto(Long predioId) {
        Predio predio = predios().stream().filter(p -> p.getId().equals(predioId)).findFirst()
                .orElseThrow(RecursoNoEncontradoException::new);
        return archivos.leer(ArchivoService.PREDIOS, predio.getFotoNombre()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Turno> calendario() {
        return turnosDe(socio());
    }

    private List<Turno> turnosDe(Socio socio) {
        LocalDate hoy = LocalDate.now();
        return turnos.findBySectorIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(socio.getSector().getId(), hoy,
                hoy.plusWeeks(SEMANAS_CALENDARIO));
    }
}
