package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
public class Socio extends Usuario {

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String codigoSocio;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaIngreso;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoSocio estado = EstadoSocio.ACTIVO;

    @Pattern(regexp = "\\d{10}")
    @Column(length = 10)
    private String celular;

    @Column(nullable = false)
    private boolean tieneCuenta;

    @NotNull
    @ManyToOne(optional = false)
    private Sector sector;

    public Socio() {
        setRol(Rol.SOCIO);
    }

    public EstadoCuenta getEstadoCuenta() {
        if (!tieneCuenta) {
            return EstadoCuenta.SIN_CUENTA;
        }
        return isActivo() ? EstadoCuenta.ACTIVA : EstadoCuenta.DESACTIVADA;
    }

    public String getCodigoSocio() {
        return codigoSocio;
    }

    public void setCodigoSocio(String codigoSocio) {
        this.codigoSocio = codigoSocio;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public EstadoSocio getEstado() {
        return estado;
    }

    public void setEstado(EstadoSocio estado) {
        this.estado = estado;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    @Override
    public boolean isPuedeIngresar() {
        return super.isPuedeIngresar() && estado == EstadoSocio.ACTIVO;
    }

    public boolean isNotificable() {
        return tieneCuenta && isPuedeIngresar();
    }

    public boolean isTieneCuenta() {
        return tieneCuenta;
    }

    public void setTieneCuenta(boolean tieneCuenta) {
        this.tieneCuenta = tieneCuenta;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }
}
