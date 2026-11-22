package ec.uta.multijunta;

import ec.uta.multijunta.config.UsuarioAutenticado;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Evento;
import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.model.Recurso;
import ec.uta.multijunta.model.TipoEvento;
import ec.uta.multijunta.model.TipoRecurso;
import ec.uta.multijunta.model.Turno;
import ec.uta.multijunta.repository.EventoRepository;
import ec.uta.multijunta.repository.PredioRepository;
import ec.uta.multijunta.repository.RecursoRepository;
import ec.uta.multijunta.repository.TurnoRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.TipoJunta;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@Component
public class Fabrica {

    public static final String CLAVE = "Clave1234";

    private static final AtomicLong SECUENCIA = new AtomicLong(1100000000L);

    private final JuntaRepository juntas;
    private final SectorRepository sectores;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private String claveCodificada;

    public Fabrica(JuntaRepository juntas, SectorRepository sectores, UsuarioRepository usuarios,
            PasswordEncoder codificador) {
        this.juntas = juntas;
        this.sectores = sectores;
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Autowired
    private EventoRepository eventos;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private TurnoRepository turnos;

    @Autowired
    private PredioRepository predios;

    public Evento evento(Junta junta, TipoEvento tipo, LocalDate fecha) {
        Evento evento = new Evento();
        evento.setJunta(junta);
        evento.setTipo(tipo);
        evento.setFecha(fecha);
        evento.setHora(LocalTime.of(9, 0));
        evento.setLugar("Casa comunal");
        return eventos.save(evento);
    }

    public Recurso recurso(Junta junta, Sector sector, String nombre) {
        Recurso recurso = new Recurso();
        recurso.setJunta(junta);
        recurso.setSector(sector);
        recurso.setNombre(nombre);
        recurso.setTipo(TipoRecurso.COMPUERTA);
        recurso.setCaudal(new BigDecimal("30.00"));
        return recursos.save(recurso);
    }

    public Turno turno(Junta junta, Sector sector, Recurso recurso, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Turno turno = new Turno();
        turno.setJunta(junta);
        turno.setSector(sector);
        turno.setRecurso(recurso);
        turno.setFecha(fecha);
        turno.setHoraInicio(inicio);
        turno.setHoraFin(fin);
        turno.setResponsable("Responsable");
        return turnos.save(turno);
    }

    public Predio predio(Socio socio, Sector sector) {
        Predio predio = new Predio();
        predio.setJunta(socio.getJunta());
        predio.setSocio(socio);
        predio.setSector(sector);
        predio.setCodigo("P-" + SECUENCIA.incrementAndGet());
        predio.setArea(new BigDecimal("1200.00"));
        predio.setLatitud(new BigDecimal("-1.241700"));
        predio.setLongitud(new BigDecimal("-78.619700"));
        return predios.save(predio);
    }

    public Usuario secretario(Junta junta) {
        return usuario(Rol.SECRETARIO, junta);
    }

    public static LocalDate proximoDiaLaboral(LocalDate desde) {
        LocalDate fecha = desde;
        while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    public Junta junta(String nombre) {
        Junta junta = new Junta();
        junta.setNombre(nombre);
        junta.setTipo(TipoJunta.RIEGO);
        junta.setRuc(String.format("%013d", SECUENCIA.incrementAndGet()));
        junta.setProvincia("Tungurahua");
        junta.setCanton("Ambato");
        return juntas.save(junta);
    }

    public Sector sector(Junta junta) {
        Sector sector = new Sector();
        sector.setJunta(junta);
        sector.setNombre("Sector " + SECUENCIA.incrementAndGet());
        return sectores.save(sector);
    }

    public Usuario usuario(Rol rol, Junta junta) {
        if (rol == Rol.SOCIO) {
            return socio(junta, sector(junta));
        }
        Usuario usuario = new Usuario();
        completar(usuario, junta);
        usuario.setRol(rol);
        return usuarios.save(usuario);
    }

    public Socio socio(Junta junta, Sector sector) {
        Socio socio = new Socio();
        completar(socio, junta);
        socio.setUsername(socio.getCedula());
        socio.setSector(sector);
        socio.setCodigoSocio("S-" + socio.getCedula());
        socio.setFechaIngreso(LocalDate.of(2021, 3, 1));
        socio.setTieneCuenta(true);
        return usuarios.save(socio);
    }

    public Junta juntaConCatalogo(String nombre) {
        return junta(nombre);
    }

    public Socio socioSinCuenta(Junta junta, Sector sector, EstadoSocio estado) {
        Socio socio = socio(junta, sector);
        socio.setTieneCuenta(false);
        socio.setActivo(false);
        socio.setEstado(estado);
        return usuarios.save(socio);
    }

    public Usuario directiva(Rol rol, Junta junta, String email) {
        Usuario usuario = usuario(rol, junta);
        usuario.setEmail(email);
        return usuarios.save(usuario);
    }

    public Usuario superAdmin() {
        return usuario(Rol.SUPER_ADMIN, null);
    }

    public Usuario guardar(Usuario usuario) {
        return usuarios.save(usuario);
    }

    public Junta guardar(Junta junta) {
        return juntas.save(junta);
    }

    public RequestPostProcessor sesion(Usuario usuario) {
        return user(UsuarioAutenticado.desde(usuario));
    }

    private void completar(Usuario usuario, Junta junta) {
        String cedula = String.valueOf(SECUENCIA.incrementAndGet());
        usuario.setCedula(cedula);
        usuario.setUsername("usuario" + cedula);
        usuario.setNombres("Nombre" + cedula);
        usuario.setApellidos("Apellido");
        usuario.setJunta(junta);
        usuario.setPassword(clave());
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);
    }

    private String clave() {
        if (claveCodificada == null) {
            claveCodificada = codificador.encode(CLAVE);
        }
        return claveCodificada;
    }
}
