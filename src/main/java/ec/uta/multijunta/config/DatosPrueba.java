package ec.uta.multijunta.config;

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
import ec.uta.multijunta.service.SocioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatosPrueba implements CommandLineRunner {

    public static final String CLAVE_COMUN = "Multijunta2026";

    private final JuntaRepository juntas;
    private final SectorRepository sectores;
    private final UsuarioRepository usuarios;
    private final SocioService socioService;
    private final PasswordEncoder codificador;
    private final boolean habilitado;
    private String claveCodificada;

    public DatosPrueba(JuntaRepository juntas, SectorRepository sectores, UsuarioRepository usuarios,
            SocioService socioService, PasswordEncoder codificador,
            @Value("${multijunta.datos-prueba:true}") boolean habilitado) {
        this.juntas = juntas;
        this.sectores = sectores;
        this.usuarios = usuarios;
        this.socioService = socioService;
        this.codificador = codificador;
        this.habilitado = habilitado;
    }

    @Override
    public void run(String... argumentos) {
        if (habilitado) {
            cargar();
        }
    }

    public void cargar() {
        if (juntas.count() > 0) {
            return;
        }
        claveCodificada = codificador.encode(CLAVE_COMUN);
        usuario("superadmin", "1700000001", "Paola", "Andrade", Rol.SUPER_ADMIN, null);

        Junta riego = junta("Junta de Riego San Pedro", TipoJunta.RIEGO, "1890000001001", "Ambato", true);
        Sector norte = sector(riego, "Sector Norte");
        Sector sur = sector(riego, "Sector Sur");
        usuario("admin.sanpedro", "1800000011", "Luis", "Paredes", Rol.ADMIN_JUNTA, riego);
        usuario("secretaria.sanpedro", "1800000012", "María", "Chicaiza", Rol.SECRETARIO, riego);
        usuario("tesorero.sanpedro", "1800000013", "Jorge", "Villacís", Rol.TESORERO, riego);
        Usuario desactivado = usuario("secretario.anterior", "1800000014", "Ramiro", "Sánchez", Rol.SECRETARIO, riego);
        desactivado.setActivo(false);
        usuarios.save(desactivado);
        socio(riego, norte, "1800000101", "Rosa", "Masaquiza", "SP-001", true, false, EstadoSocio.ACTIVO);
        socio(riego, sur, "1800000102", "Manuel", "Toaquiza", "SP-002", false, false, EstadoSocio.ACTIVO);
        socio(riego, norte, "1800000103", "Carmen", "Pilla", "SP-003", false, false, EstadoSocio.INACTIVO);

        Junta rosario = junta("Junta Comunitaria El Rosario", TipoJunta.COMUNITARIA, "1890000002001", "San Pedro de Pelileo", true);
        Sector alto = sector(rosario, "Sector Alto");
        Sector bajo = sector(rosario, "Sector Bajo");
        usuario("admin.rosario", "1800000021", "Diana", "Guevara", Rol.ADMIN_JUNTA, rosario);
        usuario("secretario.rosario", "1800000022", "Pablo", "Freire", Rol.SECRETARIO, rosario);
        usuario("tesorera.rosario", "1800000023", "Lucía", "Medina", Rol.TESORERO, rosario);
        socio(rosario, alto, "1800000201", "Segundo", "Caiza", "ER-001", true, true, EstadoSocio.ACTIVO);
        socio(rosario, bajo, "1800000202", "Blanca", "Tisalema", "ER-002", false, false, EstadoSocio.ACTIVO);
        socio(rosario, alto, "1800000203", "Hugo", "Llerena", "ER-003", false, false, EstadoSocio.INACTIVO);

        Junta santaRosa = junta("Junta Parroquial Santa Rosa", TipoJunta.PARROQUIAL, "1890000003001", "Ambato", false);
        usuario("admin.santarosa", "1800000031", "Fernando", "Ortiz", Rol.ADMIN_JUNTA, santaRosa);
    }

    private Junta junta(String nombre, TipoJunta tipo, String ruc, String canton, boolean activa) {
        Junta junta = new Junta();
        junta.setNombre(nombre);
        junta.setTipo(tipo);
        junta.setRuc(ruc);
        junta.setProvincia("Tungurahua");
        junta.setCanton(canton);
        junta.setDireccion("Calle principal s/n");
        junta.setTelefono("032000000");
        junta.setMontoMultaInasistencia(new BigDecimal("5.00"));
        junta.setActiva(activa);
        if (!activa) {
            junta.setMotivoSuspension("Falta de pago del servicio de la plataforma");
        }
        return juntas.save(junta);
    }

    private Sector sector(Junta junta, String nombre) {
        Sector sector = new Sector();
        sector.setJunta(junta);
        sector.setNombre(nombre);
        return sectores.save(sector);
    }

    private Usuario usuario(String username, String cedula, String nombres, String apellidos, Rol rol, Junta junta) {
        Usuario usuario = new Usuario();
        completar(usuario, username, cedula, nombres, apellidos, junta);
        usuario.setRol(rol);
        usuario.setEmail(username + "@multijunta.test");
        usuario.setActivo(true);
        return usuarios.save(usuario);
    }

    private void socio(Junta junta, Sector sector, String cedula, String nombres, String apellidos, String codigo,
            boolean conCuenta, boolean debeCambiar, EstadoSocio estado) {
        Socio socio = new Socio();
        completar(socio, cedula, cedula, nombres, apellidos, junta);
        socio.setSector(sector);
        socio.setCodigoSocio(codigo);
        socio.setFechaIngreso(LocalDate.of(2020, 1, 15));
        socio.setEstado(estado);
        socio.setTieneCuenta(conCuenta);
        socio.setActivo(conCuenta);
        socio.setDebeCambiarPassword(debeCambiar);
        socioService.registrar(socio);
    }

    private void completar(Usuario usuario, String username, String cedula, String nombres, String apellidos,
            Junta junta) {
        usuario.setUsername(username);
        usuario.setCedula(cedula);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setJunta(junta);
        usuario.setPassword(claveCodificada);
        usuario.setDebeCambiarPassword(false);
    }
}
