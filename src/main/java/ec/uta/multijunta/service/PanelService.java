package ec.uta.multijunta.service;

import ec.uta.multijunta.config.MatrizPermisos;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.repository.SocioRepository;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelService {

    public record Tarjeta(String titulo, String icono, String ruta, Long valor) {
    }

    private record Definicion(String titulo, String icono, String ruta, Supplier<Long> valor) {
    }

    private final SocioRepository socios;
    private final RecuperacionService recuperacion;
    private final UsuarioActual usuarioActual;

    public PanelService(SocioRepository socios, RecuperacionService recuperacion, UsuarioActual usuarioActual) {
        this.socios = socios;
        this.recuperacion = recuperacion;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<Tarjeta> tarjetas() {
        Rol rol = usuarioActual.rol();
        return definiciones().stream()
                .filter(definicion -> MatrizPermisos.puedeVer(rol, definicion.ruta()))
                .filter(definicion -> rol != Rol.SUPER_ADMIN)
                .map(definicion -> new Tarjeta(definicion.titulo(), definicion.icono(), definicion.ruta(),
                        definicion.valor() == null ? null : definicion.valor().get()))
                .toList();
    }

    private List<Definicion> definiciones() {
        return List.of(
                new Definicion("Socios activos", "bi-people", "/socios",
                        () -> socios.countByJuntaIdAndEstado(usuarioActual.juntaId(), EstadoSocio.ACTIVO)),
                new Definicion("Próximos eventos y turnos", "bi-calendar3", "/turnos", null),
                new Definicion("Asistencias pendientes de cerrar", "bi-clipboard-check", "/asistencia", null),
                new Definicion("Saldo actual", "bi-cash-stack", "/reportes", null),
                new Definicion("Deuda por cobrar", "bi-wallet2", "/cuotas", null),
                new Definicion("Ítems con stock bajo", "bi-box-seam", "/inventario", null),
                new Definicion("Mensajes no leídos", "bi-chat-dots", "/mensajes", null),
                new Definicion("Solicitudes de recuperación pendientes", "bi-key", "/recuperacion/solicitudes",
                        () -> (long) recuperacion.listarAtendibles().size()));
    }
}
