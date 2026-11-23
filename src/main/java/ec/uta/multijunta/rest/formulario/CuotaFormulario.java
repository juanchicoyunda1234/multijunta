package ec.uta.multijunta.rest.formulario;

import ec.uta.multijunta.service.DatosCuota;
import ec.uta.multijunta.service.DestinoCuota;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class CuotaFormulario {

    @NotNull(message = "{campo.obligatorio}")
    private DestinoCuota destino = DestinoCuota.TODOS;

    private Long sectorId;

    private Long socioId;

    @NotBlank(message = "{campo.obligatorio}")
    @Size(max = 120)
    private String concepto;

    @NotNull(message = "{monto.positivo}")
    @DecimalMin(value = "0.01", message = "{monto.positivo}")
    @Digits(integer = 10, fraction = 2, message = "{monto.decimales}")
    private BigDecimal monto;

    @NotNull(message = "{campo.obligatorio}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    public DatosCuota datos() {
        return new DatosCuota(destino, sectorId, socioId, concepto, monto, fechaVencimiento);
    }

    public DestinoCuota getDestino() {
        return destino;
    }

    public void setDestino(DestinoCuota destino) {
        this.destino = destino;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public Long getSocioId() {
        return socioId;
    }

    public void setSocioId(Long socioId) {
        this.socioId = socioId;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
