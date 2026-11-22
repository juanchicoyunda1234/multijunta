package ec.uta.multijunta;

import ec.uta.multijunta.config.UsuarioAutenticado;
import ec.uta.multijunta.model.EstadoSocio;
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
