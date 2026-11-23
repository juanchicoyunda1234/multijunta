package ec.uta.multijunta.service;

import ec.uta.multijunta.config.MatrizPermisos;
import ec.uta.multijunta.model.EstadoEvento;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelService {

    public record Tarjeta(String titulo, String icono, String ruta, Long valor, BigDecimal monto) {
    }

    private record Definicion(String titulo, String icono, String ruta, Supplier<Long> valor,
                              Supplier<BigDecimal> monto) {

        Definicion(String titulo, String icono, String ruta, Supplier<Long> valor) {
            this(titulo, icono, ruta, valor, null);
        }
    }

    public static final int DIAS_PROXIMOS = 7;

    private final SocioRepository socios;
    private final RecuperacionService recuperacion;
    private final EventoRepository eventos;
    private final TurnoRepository turnos;
    private final UsuarioActual usuarioActual;
    private final FinanzasService finanzas;
    private final InventarioService inventario;

    public PanelService(SocioRepository socios, RecuperacionService recuperacion, EventoRepository eventos,
            TurnoRepository turnos, UsuarioActual usuarioActual, FinanzasService finanzas,
            InventarioService inventario) {
        this.socios = socios;
        this.recuperacion = recuperacion;
        this.eventos = eventos;
        this.turnos = turnos;
        this.usuarioActual = usuarioActual;
        this.finanzas = finanzas;
        this.inventario = inventario;
    }

    private long proximosEventosYTurnos() {
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusDays(DIAS_PROXIMOS);
        Long juntaId = usuarioActual.juntaId();
        return eventos.countByJuntaIdAndEstadoAndFechaBetween(juntaId, EstadoEvento.PROGRAMADO, hoy, hasta)
                + turnos.countByJuntaIdAndFechaBetween(juntaId, hoy, hasta);
    }

    @Transactional(readOnly = true)
    public List<Tarjeta> tarjetas() {
        Rol rol = usuarioActual.rol();
        return definiciones().stream()
                .filter(definicion -> MatrizPermisos.puedeVer(rol, definicion.ruta()))
                .filter(definicion -> rol != Rol.SUPER_ADMIN)
                .map(definicion -> new Tarjeta(definicion.titulo(), definicion.icono(), definicion.ruta(),
                        definicion.valor() == null ? null : definicion.valor().get(),
                        definicion.monto() == null ? null : definicion.monto().get()))
                .toList();
    }

    private List<Definicion> definiciones() {
        return List.of(
                new Definicion("Socios activos", "bi-people", "/socios",
                        () -> socios.countByJuntaIdAndEstado(usuarioActual.juntaId(), EstadoSocio.ACTIVO)),
                new Definicion("Próximos eventos y turnos", "bi-calendar3", "/turnos", this::proximosEventosYTurnos),
                new Definicion("Asistencias pendientes de cerrar", "bi-clipboard-check", "/asistencia",
                        () -> eventos.countByJuntaIdAndTipoInAndEstadoAndAsistenciaCerradaFalseAndFechaLessThanEqual(
                                usuarioActual.juntaId(), AsistenciaService.TIPOS_CON_ASISTENCIA,
                                EstadoEvento.PROGRAMADO, LocalDate.now())),
                new Definicion("Saldo actual", "bi-cash-stack", "/reportes", null, finanzas::saldoActual),
                new Definicion("Deuda por cobrar", "bi-wallet2", "/pagos", null, finanzas::deudaPorCobrar),
                new Definicion("Ítems con stock bajo", "bi-box-seam", "/inventario", inventario::itemsConStockBajo),
                new Definicion("Mensajes no leídos", "bi-chat-dots", "/mensajes", null),
                new Definicion("Solicitudes de recuperación pendientes", "bi-key", "/recuperacion/solicitudes",
                        () -> (long) recuperacion.listarAtendibles().size()));
    }
}
