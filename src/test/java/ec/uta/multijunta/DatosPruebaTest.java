package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;

import ec.uta.multijunta.config.DatosPrueba;
import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.JuntaRepository;
import ec.uta.multijunta.repository.SectorRepository;
import ec.uta.multijunta.repository.SocioRepository;
import ec.uta.multijunta.repository.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Datos de prueba al arrancar")
class DatosPruebaTest {

    @Autowired
    private DatosPrueba datosPrueba;

    @Autowired
    private JuntaRepository juntas;

    @Autowired
    private SectorRepository sectores;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private SocioRepository socios;

    @Autowired
    private PasswordEncoder codificador;

    @Test
    @DisplayName("HU6-1, HU6-2 y HU6-4 carga juntas, roles, sectores y socios sin duplicar")
    void cargaCompletaSinDuplicados() {
        datosPrueba.cargar();
        long juntasIniciales = juntas.count();
        long usuariosIniciales = usuarios.count();
        datosPrueba.cargar();
        assertThat(juntas.count()).isEqualTo(juntasIniciales).isEqualTo(3);
        assertThat(usuarios.count()).isEqualTo(usuariosIniciales);

        assertThat(usuarios.findByRolAndActivoTrue(Rol.SUPER_ADMIN)).hasSize(1);
        List<Junta> activas = juntas.findAll().stream().filter(Junta::isActiva).toList();
        assertThat(activas).hasSize(2);
        assertThat(activas.get(0).getTipo()).isNotEqualTo(activas.get(1).getTipo());
        for (Junta junta : activas) {
            for (Rol rol : new Rol[] {Rol.ADMIN_JUNTA, Rol.SECRETARIO, Rol.TESORERO}) {
                assertThat(usuarios.findByJuntaIdAndRolAndActivoTrue(junta.getId(), rol)).isNotEmpty();
            }
            assertThat(sectores.findAllByJuntaId(junta.getId())).hasSizeGreaterThanOrEqualTo(2);
            List<Socio> sociosJunta = socios.findAllByJuntaId(junta.getId());
            assertThat(sociosJunta).hasSizeGreaterThanOrEqualTo(3);
            assertThat(sociosJunta).anyMatch(Socio::isTieneCuenta);
            assertThat(sociosJunta).anyMatch(socio -> socio.getEstado() == EstadoSocio.INACTIVO);
            assertThat(sociosJunta).allMatch(socio -> socio.getSector().getJunta().getId().equals(junta.getId()));
        }

        Junta suspendida = juntas.findAll().stream().filter(junta -> !junta.isActiva()).findFirst().orElseThrow();
        assertThat(usuarios.findByJuntaIdAndRolAndActivoTrue(suspendida.getId(), Rol.ADMIN_JUNTA)).isNotEmpty();
        assertThat(usuarios.findAll()).anyMatch(Usuario::isDebeCambiarPassword);
        assertThat(usuarios.findAll()).anyMatch(usuario -> !usuario.isActivo() && usuario.getRol() != Rol.SOCIO);
        Usuario admin = usuarios.findByUsername("admin.sanpedro").orElseThrow();
        assertThat(codificador.matches(DatosPrueba.CLAVE_COMUN, admin.getPassword())).isTrue();
    }
}
