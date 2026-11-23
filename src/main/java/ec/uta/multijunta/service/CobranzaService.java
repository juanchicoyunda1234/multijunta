package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.EstadoObligacion;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Obligacion;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.SocioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CobranzaService {

    public record FilaCobranza(Socio socio, BigDecimal deuda, boolean vencida) {

        public boolean isAlDia() {
            return deuda.signum() == 0;
        }
    }

    private static final Set<EstadoObligacion> COBRABLES = Set.of(EstadoObligacion.PENDIENTE, EstadoObligacion.PARCIAL);
    private static final Comparator<Obligacion> ANTIGUEDAD = Comparator.comparing(Obligacion::getFechaOrden)
            .thenComparing(Obligacion::getFecha).thenComparing(Obligacion::getClave);

    private final SocioRepository socios;
    private final MultaRepository multas;
    private final CuotaRepository cuotas;
    private final PagoRepository pagos;
    private final JuntaRepository juntas;
    private final FinanzasService finanzas;
    private final UsuarioActual usuarioActual;

    public CobranzaService(SocioRepository socios, MultaRepository multas, CuotaRepository cuotas,
            PagoRepository pagos, JuntaRepository juntas, FinanzasService finanzas, UsuarioActual usuarioActual) {
        this.socios = socios;
        this.multas = multas;
        this.cuotas = cuotas;
        this.pagos = pagos;
        this.juntas = juntas;
        this.finanzas = finanzas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<FilaCobranza> socios(String texto) {
        Long juntaId = usuarioActual.juntaId();
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        Map<Long, BigDecimal> deudas = new HashMap<>();
        acumular(deudas, multas.deudaPorSocio(juntaId));
        acumular(deudas, cuotas.deudaPorSocio(juntaId));
        Set<Long> conVencidas = cuotas.sociosConCuotasVencidas(juntaId, LocalDate.now());
        return socios.buscar(juntaId, filtro).stream()
                .map(socio -> new FilaCobranza(socio, deudas.getOrDefault(socio.getId(), BigDecimal.ZERO),
                        conVencidas.contains(socio.getId())))
                .sorted(Comparator.comparing(FilaCobranza::deuda).reversed()
                        .thenComparing(fila -> fila.socio().getApellidos())
                        .thenComparing(fila -> fila.socio().getNombres()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Socio socio(Long socioId) {
        return socios.findByIdAndJuntaId(socioId, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Obligacion> obligaciones(Long socioId) {
        Socio socio = socio(socioId);
        Long juntaId = usuarioActual.juntaId();
        List<Obligacion> lista = new ArrayList<>();
        lista.addAll(multas.findBySocioIdAndJuntaIdAndEstadoIn(socio.getId(), juntaId, COBRABLES));
        lista.addAll(cuotas.findBySocioIdAndJuntaIdAndEstadoIn(socio.getId(), juntaId, COBRABLES));
        lista.sort(ANTIGUEDAD);
        return lista;
    }

    @Transactional(readOnly = true)
    public BigDecimal deuda(Long socioId) {
        return finanzas.deudaDeSocio(socio(socioId).getId());
    }

    @Transactional
    public Pago cobrar(Long socioId, DatosCobro datos) {
        Socio socio = socio(socioId);
        Long juntaId = usuarioActual.juntaId();
        if (datos.obligaciones().isEmpty()) {
            throw new ReglaNegocioException("pago.seleccion");
        }
        List<Obligacion> seleccion = new ArrayList<>();
        for (String clave : new LinkedHashSet<>(datos.obligaciones())) {
            Obligacion obligacion = buscar(clave, juntaId);
            if (!obligacion.getSocio().getId().equals(socio.getId()) || !obligacion.isCobrable()) {
                throw new ReglaNegocioException("pago.cambio");
            }
            seleccion.add(obligacion);
        }
        seleccion.sort(ANTIGUEDAD);
        BigDecimal total = seleccion.stream().map(Obligacion::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monto = datos.monto();
        if (monto == null || monto.signum() <= 0 || monto.compareTo(total) > 0) {
            throw new ReglaNegocioException("pago.montoInvalido", "monto");
        }
        Montos.validar(monto, "monto");
        if (monto.compareTo(total) < 0 && !datos.confirmarParcial()) {
            throw new ReglaNegocioException("pago.confirmarParcial");
        }
        Junta junta = juntas.findById(juntaId).orElseThrow(RecursoNoEncontradoException::new);
        Pago pago = new Pago();
        pago.setJunta(junta);
        pago.setSocio(socio);
        pago.setNumero(finanzas.siguienteNumero(juntaId));
        pago.setMonto(Montos.normalizar(monto));
        pago.setFecha(LocalDateTime.now());
        pago.setMetodo(datos.metodo());
        pago.setRegistradoPor(usuarioActual.entidad());
        BigDecimal restante = monto;
        List<String> conceptos = new ArrayList<>();
        for (Obligacion obligacion : seleccion) {
            if (restante.signum() == 0) {
                break;
            }
            BigDecimal importe = restante.min(obligacion.getSaldoPendiente());
            pago.agregar(obligacion, importe);
            conceptos.add(obligacion.getConcepto());
            restante = restante.subtract(importe);
        }
        String concepto = String.join(", ", conceptos);
        pago.setConcepto(concepto.length() > 255 ? concepto.substring(0, 252) + "..." : concepto);
        return pagos.saveAndFlush(pago);
    }

    @Transactional(readOnly = true)
    public Pago pago(Long id) {
        Pago pago = pagos.findByIdAndJuntaId(id, usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        pago.getAplicaciones().size();
        return pago;
    }

    @Transactional(readOnly = true)
    public List<Pago> historial(String texto) {
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        return pagos.buscar(usuarioActual.juntaId(), filtro);
    }

    @Transactional(readOnly = true)
    public Junta junta() {
        return juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
    }

    private Obligacion buscar(String clave, Long juntaId) {
        Long id = identificador(clave);
        if (clave.startsWith("M-")) {
            Multa multa = multas.findByIdAndJuntaId(id, juntaId).orElseThrow(RecursoNoEncontradoException::new);
            return multa;
        }
        if (clave.startsWith("C-")) {
            Cuota cuota = cuotas.findByIdAndJuntaId(id, juntaId).orElseThrow(RecursoNoEncontradoException::new);
            return cuota;
        }
        throw new ReglaNegocioException("pago.cambio");
    }

    private Long identificador(String clave) {
        try {
            return Long.valueOf(clave.substring(2));
        } catch (RuntimeException excepcion) {
            throw new ReglaNegocioException("pago.cambio");
        }
    }

    private void acumular(Map<Long, BigDecimal> deudas, List<Object[]> filas) {
        filas.stream().collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (BigDecimal) fila[1]))
                .forEach((socioId, deuda) -> deudas.merge(socioId, deuda, BigDecimal::add));
    }
}
