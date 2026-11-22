package ec.uta.multijunta;

import static org.assertj.core.api.Assertions.assertThat;

import ec.uta.multijunta.service.CatalogoUbicacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FR-004 Catálogo de provincias y cantones del Ecuador")
class CatalogoUbicacionTest {

    private final CatalogoUbicacionService catalogo = new CatalogoUbicacionService();

    @Test
    @DisplayName("tiene 24 provincias y 221 cantones")
    void totales() {
        assertThat(catalogo.provincias()).hasSize(24);
        assertThat(catalogo.provincias().stream().mapToInt(provincia -> provincia.cantones().size()).sum())
                .isEqualTo(221);
    }

    @Test
    @DisplayName("cada cantón pertenece a su provincia")
    void cantonesPorProvincia() {
        assertThat(catalogo.cantones("Tungurahua")).contains("Ambato", "San Pedro de Pelileo");
        assertThat(catalogo.esValido("Tungurahua", "Ambato")).isTrue();
        assertThat(catalogo.esValido("Pichincha", "Ambato")).isFalse();
        assertThat(catalogo.esValido("Inexistente", "Ambato")).isFalse();
        assertThat(catalogo.esValido(null, null)).isFalse();
    }
}
