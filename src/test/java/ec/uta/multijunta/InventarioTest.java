package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.uta.multijunta.model.EstadoItem;
import ec.uta.multijunta.model.ItemInventario;
import ec.uta.multijunta.model.Junta;
import ec.uta.multijunta.model.MovimientoInventario;
import ec.uta.multijunta.model.Rol;
import ec.uta.multijunta.model.TipoMovimiento;
import ec.uta.multijunta.model.Usuario;
import ec.uta.multijunta.repository.ItemInventarioRepository;
import ec.uta.multijunta.repository.MovimientoInventarioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CU13 Gestionar inventarios")
class InventarioTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Fabrica fabrica;

    @Autowired
    private ItemInventarioRepository items;

    @Autowired
    private MovimientoInventarioRepository movimientos;

    private Junta junta;
    private Usuario tesorero;

    @BeforeEach
    void preparar() {
        junta = fabrica.junta("Junta Inventario");
        tesorero = fabrica.tesorero(junta);
    }

    private ResultActions crearItem(String nombre, String minimo) throws Exception {
        return mvc.perform(post("/inventario/items").with(fabrica.sesion(tesorero)).with(csrf())
                .param("nombre", nombre).param("unidadMedida", "unid").param("stockMinimo", minimo));
    }

    private ResultActions mover(ItemInventario item, String tipo, String cantidad, String motivo) throws Exception {
        return mvc.perform(post("/inventario/movimientos").with(fabrica.sesion(tesorero)).with(csrf())
                .param("itemId", item.getId().toString()).param("tipo", tipo).param("cantidad", cantidad)
                .param("motivo", motivo));
    }

    private ItemInventario item(String nombre) {
        return items.findByJuntaIdOrderByNombreAsc(junta.getId()).stream()
                .filter(i -> i.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    private ItemInventario recargar(ItemInventario item) {
        return items.findById(item.getId()).orElseThrow();
    }

    @Test
    @DisplayName("HU6-1 y HU6-2 crear ítem con código y stock cero; nombres únicos")
    void crearItems() throws Exception {
        crearItem("Tubería PVC 2 pulg.", "10").andExpect(flash().attribute("exito", "inventario.itemCreado"));
        ItemInventario tuberia = item("Tubería PVC 2 pulg.");
        assertThat(tuberia.getCodigo()).isEqualTo("INV-0001");
        assertThat(tuberia.getStock()).isEqualByComparingTo("0");
        assertThat(tuberia.getEstado()).isEqualTo(EstadoItem.ACTIVO);
        crearItem("Válvula de paso", "2");
        assertThat(item("Válvula de paso").getCodigo()).isEqualTo("INV-0002");
        crearItem("TUBERÍA pvc 2 PULG.", "1")
                .andExpect(content().string(containsString("Ya existe un ítem con ese nombre")));
        crearItem("Medidor", "-1").andExpect(content().string(containsString("El stock mínimo no puede ser negativo")));
        mvc.perform(get("/inventario").with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INV-0001")))
                .andExpect(content().string(containsString("Nuevo Movimiento de Inventario")));
        assertThat(items.findByJuntaIdOrderByNombreAsc(junta.getId())).hasSize(2);
    }

    @Test
    @DisplayName("HU6-3, HU6-4 y HU6-5 entradas, salidas, kardex, stock crítico y stock insuficiente")
    void movimientos() throws Exception {
        crearItem("Válvula de paso", "12");
        ItemInventario valvula = item("Válvula de paso");
        mover(valvula, "ENTRADA", "20", "Compra").andExpect(flash().attribute("exito", "inventario.movimiento"));
        mover(valvula, "SALIDA", "8", "Uso en minga").andExpect(flash().attribute("exito", "inventario.movimiento"));
        assertThat(recargar(valvula).getStock()).isEqualByComparingTo("12");
        assertThat(recargar(valvula).isCritico()).isTrue();
        List<MovimientoInventario> kardex = movimientos.findByItemIdOrderByFechaAscIdAsc(valvula.getId());
        assertThat(kardex).extracting(MovimientoInventario::getTipo)
                .containsExactly(TipoMovimiento.ENTRADA, TipoMovimiento.SALIDA);
        assertThat(kardex.get(1).getStockResultante()).isEqualByComparingTo("12");
        assertThat(kardex.get(1).getUsuario().getId()).isEqualTo(tesorero.getId());
        mvc.perform(get("/inventario").with(fabrica.sesion(tesorero)))
                .andExpect(content().string(containsString("fila-critica")))
                .andExpect(content().string(containsString("CRÍTICO")));
        mover(valvula, "SALIDA", "13", "Reparación")
                .andExpect(content().string(containsString("Stock insuficiente para esta salida")));
        mover(valvula, "ENTRADA", "0", "Compra")
                .andExpect(content().string(containsString("La cantidad debe ser mayor que cero")));
        mover(valvula, "SALIDA", "12", "Reparación de fuga").andExpect(flash().attribute("exito", "inventario.movimiento"));
        assertThat(recargar(valvula).getStock()).isEqualByComparingTo("0");
        mvc.perform(get("/inventario/items/" + valvula.getId()).with(fabrica.sesion(tesorero)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Uso en minga")))
                .andExpect(content().string(containsString("Reparación de fuga")));
    }

    @Test
    @DisplayName("HU6-6 inactivar en vez de eliminar; ítem inactivo sin movimientos nuevos")
    void estados() throws Exception {
        crearItem("Medidor", "1");
        crearItem("Codo", "1");
        ItemInventario medidor = item("Medidor");
        ItemInventario codo = item("Codo");
        mover(medidor, "ENTRADA", "5", "Compra");
        mvc.perform(post("/inventario/items/" + medidor.getId() + "/eliminar").with(fabrica.sesion(tesorero)).with(csrf()))
                .andExpect(flash().attribute("error", "inventario.conMovimientos"));
        mvc.perform(post("/inventario/items/" + medidor.getId() + "/estado").param("activo", "false")
                .with(fabrica.sesion(tesorero)).with(csrf())).andExpect(flash().attribute("exito", "inventario.estado"));
        assertThat(recargar(medidor).getEstado()).isEqualTo(EstadoItem.INACTIVO);
        mover(medidor, "ENTRADA", "1", "Compra").andExpect(content().string(containsString("El ítem está inactivo")));
        assertThat(movimientos.findByItemIdOrderByFechaAscIdAsc(medidor.getId())).hasSize(1);
        mvc.perform(post("/inventario/items/" + medidor.getId() + "/estado").param("activo", "true")
                .with(fabrica.sesion(tesorero)).with(csrf()));
        assertThat(recargar(medidor).getEstado()).isEqualTo(EstadoItem.ACTIVO);
        mvc.perform(post("/inventario/items/" + codo.getId() + "/eliminar").with(fabrica.sesion(tesorero)).with(csrf()))
                .andExpect(flash().attribute("exito", "inventario.eliminado"));
        assertThat(items.findById(codo.getId())).isEmpty();
    }

    @Test
    @DisplayName("HU6-8 ADMIN_JUNTA solo lectura; otra junta responde 404")
    void permisosYAislamiento() throws Exception {
        crearItem("Medidor", "1");
        ItemInventario medidor = item("Medidor");
        Usuario admin = fabrica.usuario(Rol.ADMIN_JUNTA, junta);
        mvc.perform(get("/inventario").with(fabrica.sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Medidor")))
                .andExpect(content().string(not(containsString("Nuevo Movimiento de Inventario"))));
        mvc.perform(get("/inventario/items/" + medidor.getId()).with(fabrica.sesion(admin))).andExpect(status().isOk());
        mvc.perform(post("/inventario/items").with(fabrica.sesion(admin)).with(csrf()).param("nombre", "X"))
                .andExpect(status().isForbidden());
        Junta otra = fabrica.junta("Junta Ajena Inventario");
        Usuario ajeno = fabrica.tesorero(otra);
        mvc.perform(get("/inventario/items/" + medidor.getId()).with(fabrica.sesion(ajeno))).andExpect(status().isNotFound());
        mvc.perform(post("/inventario/movimientos").with(fabrica.sesion(ajeno)).with(csrf())
                .param("itemId", medidor.getId().toString()).param("tipo", "ENTRADA").param("cantidad", "1")
                .param("motivo", "x")).andExpect(status().isNotFound());
        mvc.perform(post("/inventario/items/" + medidor.getId() + "/eliminar").with(fabrica.sesion(ajeno)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/inventario").with(fabrica.sesion(ajeno)))
                .andExpect(content().string(not(containsString("Medidor"))));
    }
}
