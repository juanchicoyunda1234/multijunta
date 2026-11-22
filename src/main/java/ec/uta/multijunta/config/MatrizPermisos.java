package ec.uta.multijunta.config;

import static ec.uta.multijunta.model.Rol.ADMIN_JUNTA;
import static ec.uta.multijunta.model.Rol.SECRETARIO;
import static ec.uta.multijunta.model.Rol.SOCIO;
import static ec.uta.multijunta.model.Rol.SUPER_ADMIN;
import static ec.uta.multijunta.model.Rol.TESORERO;

import ec.uta.multijunta.model.Rol;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class MatrizPermisos {

    public record Entrada(String cu, String menu, String icono, String ruta, boolean exacta,
                          Set<Rol> completo, Set<Rol> lectura) {

        public List<String> patrones() {
            return exacta ? List.of(ruta) : List.of(ruta, ruta + "/**");
        }

        public boolean visiblePara(Rol rol) {
            return completo.contains(rol) || lectura.contains(rol);
        }

        public boolean soloLectura(Rol rol) {
            return lectura.contains(rol) && !completo.contains(rol);
        }

        public Set<Rol> conLectura() {
            EnumSet<Rol> todos = EnumSet.noneOf(Rol.class);
            todos.addAll(completo);
            todos.addAll(lectura);
            return todos;
        }
    }

    private static final Set<Rol> NINGUNO = Set.of();
    private static final Set<Rol> TODOS = EnumSet.allOf(Rol.class);

    private static final List<Entrada> ENTRADAS = List.of(
            new Entrada("CU20", null, null, "/plataforma", true, Set.of(SUPER_ADMIN), NINGUNO),
            new Entrada("CU01, CU02, CU24", "Juntas", "bi-buildings", "/plataforma/juntas", false,
                    Set.of(SUPER_ADMIN), NINGUNO),
            new Entrada("CU03", "Configuración de Junta", "bi-gear", "/junta/configuracion", false,
                    Set.of(ADMIN_JUNTA), NINGUNO),
            new Entrada("CU04, CU23", "Usuarios", "bi-person-gear", "/usuarios", false,
                    Set.of(ADMIN_JUNTA), NINGUNO),
            new Entrada("CU21", "Solicitudes de recuperación", "bi-key", "/recuperacion/solicitudes", false,
                    Set.of(SUPER_ADMIN, ADMIN_JUNTA), NINGUNO),
            new Entrada("CU25", "Inicio", "bi-house", "/panel", false,
                    Set.of(ADMIN_JUNTA, SECRETARIO, TESORERO), NINGUNO),
            new Entrada("CU05", "Nómina de socios", "bi-people", "/socios", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA, TESORERO)),
            new Entrada("CU06", "Asistencia", "bi-clipboard-check", "/asistencia", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA, TESORERO)),
            new Entrada("CU28", "Correcciones de asistencia", "bi-pencil-square", "/asistencia/correcciones", false,
                    Set.of(ADMIN_JUNTA, SECRETARIO), NINGUNO),
            new Entrada("CU07", "Predios", "bi-map", "/predios", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU08", "Calendario y turnos", "bi-calendar3", "/turnos", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU26", "Eventos", "bi-calendar-event", "/eventos", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU27", "Recursos de riego", "bi-droplet", "/recursos", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU09", "Pagos", "bi-cash-coin", "/pagos", false,
                    Set.of(TESORERO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU10", "Multas", "bi-exclamation-triangle", "/multas", false,
                    Set.of(TESORERO), Set.of(ADMIN_JUNTA, SECRETARIO)),
            new Entrada("CU11", "Egresos", "bi-receipt", "/egresos", false,
                    Set.of(TESORERO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU12", "Reportes", "bi-file-earmark-bar-graph", "/reportes", false,
                    Set.of(ADMIN_JUNTA, TESORERO), NINGUNO),
            new Entrada("CU13", "Inventario", "bi-box-seam", "/inventario", false,
                    Set.of(TESORERO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU14", "Activos fijos", "bi-truck", "/activos", false,
                    Set.of(TESORERO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU30, CU31", "Cuotas", "bi-wallet2", "/cuotas", false,
                    Set.of(TESORERO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU15", "Convocatorias", "bi-megaphone", "/convocatorias", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU16", "Actas", "bi-journal-text", "/actas", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA, TESORERO)),
            new Entrada("CU19", "Comunicaciones externas", "bi-envelope-paper", "/comunicaciones", false,
                    Set.of(SECRETARIO), Set.of(ADMIN_JUNTA)),
            new Entrada("CU17", "Mi información", "bi-person-vcard", "/mi-informacion", false,
                    Set.of(SOCIO), NINGUNO),
            new Entrada("CU32", "Anuncios y repositorio", "bi-newspaper", "/anuncios", false,
                    Set.of(SOCIO), NINGUNO),
            new Entrada("CU18", "Mensajes", "bi-chat-dots", "/mensajes", false,
                    Set.of(ADMIN_JUNTA, SECRETARIO, TESORERO, SOCIO), NINGUNO),
            new Entrada("CU22", null, null, "/cuenta", false, TODOS, NINGUNO),
            new Entrada("CU29", null, null, "/notificaciones", false, TODOS, NINGUNO),
            new Entrada("CU20", null, null, "/inicio", false, TODOS, NINGUNO),
            new Entrada("CU20", null, null, "/en-construccion", false, TODOS, NINGUNO));

    private MatrizPermisos() {
    }

    public static List<Entrada> entradas() {
        return ENTRADAS;
    }

    public static List<Entrada> ordenadasPorEspecificidad() {
        return ENTRADAS.stream()
                .sorted(Comparator.comparingInt((Entrada e) -> e.ruta().length()).reversed())
                .toList();
    }

    public static boolean puedeVer(Rol rol, String ruta) {
        return ENTRADAS.stream().anyMatch(entrada -> entrada.ruta().equals(ruta) && entrada.visiblePara(rol));
    }

    public static List<Entrada> menuPara(Rol rol) {
        return ENTRADAS.stream()
                .filter(e -> e.menu() != null && e.visiblePara(rol))
                .toList();
    }
}
