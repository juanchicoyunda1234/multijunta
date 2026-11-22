package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.model.Predio;
import ec.uta.multijunta.service.DatosPredio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;

public class PredioFormulario {

    @NotNull(message = "{predio.area}")
    @DecimalMin(value = "0.01", message = "{predio.area}")
    private BigDecimal area;

    @NotNull(message = "{campo.obligatorio}")
    private Long sectorId;

    @Size(max = 200)
    private String ubicacion;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private MultipartFile foto;

    public static PredioFormulario desde(Predio predio) {
        PredioFormulario formulario = new PredioFormulario();
        formulario.setArea(predio.getArea());
        formulario.setSectorId(predio.getSector().getId());
        formulario.setUbicacion(predio.getUbicacion());
        formulario.setLatitud(predio.getLatitud());
        formulario.setLongitud(predio.getLongitud());
        return formulario;
    }

    public DatosPredio datos() {
        return new DatosPredio(sectorId, area, ubicacion, latitud, longitud, foto);
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public MultipartFile getFoto() {
        return foto;
    }

    public void setFoto(MultipartFile foto) {
        this.foto = foto;
    }
}
