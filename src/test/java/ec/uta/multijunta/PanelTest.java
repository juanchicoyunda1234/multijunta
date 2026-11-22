package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import ec.uta.multijunta.model.EstadoSocio;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.Sector;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.service.PanelService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU25 Panel de inicio de la directiva")
class PanelTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    private Junta junta;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Panel");
        Sector sector = fabrica.sector(junta);
        fabrica.socio(junta, sector);
        fabrica.socioSinCuenta(junta, sector, EstadoSocio.ACTIVO);
        fabrica.socioSinCuenta(junta, sector, EstadoSocio.INACTIVO);
        Junta otra = fabrica.junta("Junta Otra Panel");
        fabrica.socioSinCuenta(otra, fabrica.sector(otra), EstadoSocio.ACTIVO);
    }

    @SuppressWarnings("unchecked")
    private Map<String, PanelService.Tarjeta> tarjetas(Usuario usuario) throws Exception {
        List<PanelService.Tarjeta> lista = (List<PanelService.Tarjeta>) mvc.perform(get("/panel").with(fabrica.sesion(usuario)))
                .andReturn().getModelAndView().getModel().get("tarjetas");
        return lista.stream().collect(Collectors.toMap(PanelService.Tarjeta::titulo, tarjeta -> tarjeta));
    }

    @Test
    @DisplayName("HU7-1 cada rol ve solo sus tarjetas")
    void tarjetasPorRol() throws Exception {
        assertThat(tarjetas(fabrica.usuario(Rol.ADMIN_JUNTA, junta)).keySet()).containsExactlyInAnyOrder(
                "Socios activos", "Próximos eventos y turnos", "Asistencias pendientes de cerrar", "Saldo actual",
                "Deuda por cobrar", "Ítems con stock bajo", "Mensajes no leídos", "Solicitudes de recuperación pendientes");
        assertThat(tarjetas(fabrica.usuario(Rol.SECRETARIO, junta)).keySet()).containsExactlyInAnyOrder(
                "Socios activos", "Próximos eventos y turnos", "Asistencias pendientes de cerrar", "Mensajes no leídos");
        assertThat(tarjetas(fabrica.usuario(Rol.TESORERO, junta)).keySet()).containsExactlyInAnyOrder(
                "Socios activos", "Asistencias pendientes de cerrar", "Saldo actual", "Deuda por cobrar",
                "Ítems con stock bajo", "Mensajes no leídos");
    }

    @Test
    @DisplayName("HU7-2 y HU7-5 valores reales solo de la junta del usuario")
    void valoresReales() throws Exception {
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        Usuario socio = fabrica.socio(junta, fabrica.sector(junta));
        mvc.perform(post("/recuperar").param("identificador", socio.getCedula()).with(csrf()));
        Map<String, PanelService.Tarjeta> panel = tarjetas(admin);
        assertThat(panel.get("Socios activos").valor()).isEqualTo(3);
        assertThat(panel.get("Solicitudes de recuperación pendientes").valor()).isEqualTo(1);
    }

    @Test
    @DisplayName("HU7-3 y HU7-4 tarjetas sin módulo muestran 'Disponible próximamente' y cada una enlaza a su módulo")
    void proximamenteYEnlaces() throws Exception {
        Usuario tesorero = fabrica.usuario(Rol.TESORERO, junta);
        Map<String, PanelService.Tarjeta> panel = tarjetas(tesorero);
        assertThat(panel.get("Saldo actual").valor()).isNull();
        assertThat(panel.get("Saldo actual").ruta()).isEqualTo("/reportes");
        assertThat(panel.get("Socios activos").ruta()).isEqualTo("/socios");
        mvc.perform(get("/panel").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("Disponible próximamente")))
                .andExpect(content().string(containsString("href=\"/cuotas\"")));
    }
}
