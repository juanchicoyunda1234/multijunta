package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.TipoRecurso;
import ec.uta.multijunta.service.DatosRecurso;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class RecursoFormulario {

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 80)
    private String nombre;

    @NotNull(message = "{campo.obligatorio}")
    private TipoRecurso tipo;

    @NotNull(message = "{campo.obligatorio}")
    private Long sectorId;

    @NotNull(message = "{recurso.caudal}")
    @DecimalMin(value = "0.01", message = "{recurso.caudal}")
    private BigDecimal caudal;

    public DatosRecurso datos() {
        return new DatosRecurso(nombre, tipo, sectorId, caudal);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public void setTipo(TipoRecurso tipo) {
        this.tipo = tipo;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public BigDecimal getCaudal() {
        return caudal;
    }

    public void setCaudal(BigDecimal caudal) {
        this.caudal = caudal;
    }
}
