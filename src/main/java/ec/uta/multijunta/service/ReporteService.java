package ec.uta.multijunta.service;

import ec.uta.multijunta.model.CategoriaEgreso;
import ec.uta.multijunta.model.Cuota;
import ec.uta.multijunta.model.Egreso;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Multa;
import ec.uta.multijunta.model.Obligacion;
import ec.uta.multijunta.model.Pago;
import ec.uta.multijunta.model.ReversoPago;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoReporte;
import ec.uta.multijunta.repository.CuotaRepository;
import ec.uta.multijunta.repository.EgresoRepository;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.MultaRepository;
import ec.uta.multijunta.repository.PagoRepository;
import ec.uta.multijunta.repository.ReversoPagoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {

    public record Fila(List<String> textos, BigDecimal monto) {
    }

    public record Total(String etiqueta, BigDecimal monto) {
    }

    public record Reporte(TipoReporte tipo, Junta junta, LocalDate inicio, LocalDate fin, LocalDateTime generado,
                          List<String> columnas, List<Fila> filas, List<Total> totales) {

        public BigDecimal resultado() {
            return totales.get(totales.size() - 1).monto();
        }
    }

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PagoRepository pagos;
    private final ReversoPagoRepository reversos;
    private final EgresoRepository egresos;
    private final MultaRepository multas;
    private final CuotaRepository cuotas;
    private final JuntaRepository juntas;
    private final UsuarioActual usuarioActual;

    public ReporteService(PagoRepository pagos, ReversoPagoRepository reversos, EgresoRepository egresos,
            MultaRepository multas, CuotaRepository cuotas, JuntaRepository juntas, UsuarioActual usuarioActual) {
        this.pagos = pagos;
        this.reversos = reversos;
        this.egresos = egresos;
        this.multas = multas;
        this.cuotas = cuotas;
        this.juntas = juntas;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public Reporte generar(TipoReporte tipo, LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new ReglaNegocioException("reporte.rango", "fin");
        }
        Junta junta = juntas.findById(usuarioActual.juntaId()).orElseThrow(RecursoNoEncontradoException::new);
        Reporte reporte = tipo == TipoReporte.MOROSOS ? morosos(junta, inicio, fin) : balance(junta, inicio, fin);
        if (reporte.filas().isEmpty()) {
            throw new ReglaNegocioException("reporte.sinRegistros");
        }
        return reporte;
    }

    private Reporte balance(Junta junta, LocalDate inicio, LocalDate fin) {
        LocalDateTime desde = inicio.atStartOfDay();
        LocalDateTime hasta = fin.atTime(LocalTime.MAX);
        List<Fila> filas = new ArrayList<>();
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal reversado = BigDecimal.ZERO;
        for (Pago pago : pagos.findByJuntaIdAndFechaBetweenOrderByFechaAsc(junta.getId(), desde, hasta)) {
            filas.add(new Fila(List.of(pago.getFecha().toLocalDate().format(FECHA), "Ingreso", pago.getNumero(),
                    pago.getSocio().getNombreCompleto() + ": " + pago.getConcepto()), pago.getMonto()));
            ingresos = ingresos.add(pago.getMonto());
        }
        for (ReversoPago reverso : reversos.findByJuntaIdAndFechaBetweenOrderByFechaAsc(junta.getId(), desde, hasta)) {
            filas.add(new Fila(List.of(reverso.getFecha().toLocalDate().format(FECHA), "Reverso", reverso.getNumero(),
                    "Reverso de " + reverso.getPago().getNumero() + ": " + reverso.getMotivo()),
                    reverso.getMonto().negate()));
            reversado = reversado.add(reverso.getMonto());
        }
        Map<CategoriaEgreso, BigDecimal> porCategoria = new EnumMap<>(CategoriaEgreso.class);
        BigDecimal gastos = BigDecimal.ZERO;
        for (Egreso egreso : egresos.findByJuntaIdAndFechaBetweenOrderByFechaAscIdAsc(junta.getId(), inicio, fin)) {
            String proveedor = egreso.getProveedor() == null ? "" : " (" + egreso.getProveedor() + ")";
            filas.add(new Fila(List.of(egreso.getFecha().format(FECHA), "Egreso", egreso.getCategoria().getEtiqueta(),
                    egreso.getConcepto() + proveedor), egreso.getMonto().negate()));
            porCategoria.merge(egreso.getCategoria(), egreso.getMonto(), BigDecimal::add);
            gastos = gastos.add(egreso.getMonto());
        }
        List<Total> totales = new ArrayList<>();
        totales.add(new Total("Ingresos por pagos", ingresos));
        totales.add(new Total("Reversos", reversado.negate()));
        porCategoria.forEach((categoria, monto) -> totales.add(new Total("Egresos · " + categoria.getEtiqueta(),
                monto.negate())));
        totales.add(new Total("Total egresos", gastos.negate()));
        totales.add(new Total("Saldo del rango", ingresos.subtract(reversado).subtract(gastos)));
        return new Reporte(TipoReporte.BALANCE, junta, inicio, fin, LocalDateTime.now(),
                List.of("Fecha", "Tipo", "Referencia", "Detalle"), filas, totales);
    }

    private Reporte morosos(Junta junta, LocalDate inicio, LocalDate fin) {
        List<Obligacion> obligaciones = new ArrayList<>();
        obligaciones.addAll(cuotas.morosas(junta.getId(), LocalDate.now(), fin));
        obligaciones.addAll(multas.morosas(junta.getId(), fin));
        obligaciones.sort(Comparator.comparing((Obligacion o) -> o.getSocio().getApellidos())
                .thenComparing(o -> o.getSocio().getNombres()).thenComparing(Obligacion::getFechaOrden));
        Map<Socio, BigDecimal> deudas = new LinkedHashMap<>();
        List<Fila> filas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Obligacion obligacion : obligaciones) {
            Socio socio = obligacion.getSocio();
            String fecha = obligacion instanceof Cuota cuota ? "Vence " + cuota.getFechaVencimiento().format(FECHA)
                    : ((Multa) obligacion).getFecha().format(FECHA);
            filas.add(new Fila(List.of(socio.getNombreCompleto(), socio.getCedula(), obligacion.getTipo() + ": "
                    + obligacion.getConcepto(), fecha), obligacion.getSaldoPendiente()));
            deudas.merge(socio, obligacion.getSaldoPendiente(), BigDecimal::add);
            total = total.add(obligacion.getSaldoPendiente());
        }
        List<Total> totales = new ArrayList<>();
        deudas.forEach((socio, deuda) -> totales.add(new Total("Deuda de " + socio.getNombreCompleto(), deuda)));
        totales.add(new Total("Total en mora", total));
        return new Reporte(TipoReporte.MOROSOS, junta, inicio, fin, LocalDateTime.now(),
                List.of("Socio", "Cédula", "Obligación", "Fecha"), filas, totales);
    }
}
