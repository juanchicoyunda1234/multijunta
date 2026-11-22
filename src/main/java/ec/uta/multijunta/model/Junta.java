package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Junta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoJunta tipo;

    @NotBlank
    @Pattern(regexp = "\\d{13}")
    @Column(nullable = false, unique = true, length = 13)
    private String ruc;

    @Size(max = 200)
    @Column(length = 200)
    private String direccion;

    @Size(max = 20)
    @Column(length = 20)
    private String telefono;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String provincia;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String canton;

    @Column(nullable = false)
    private LocalDate fechaRegistro;

    private String logoUrl;

    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2)
    private BigDecimal montoMultaInasistencia;

    @Column(nullable = false)
    private boolean activa = true;

    @Size(max = 300)
    @Column(length = 300)
    private String motivoSuspension;

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoJunta getTipo() {
        return tipo;
    }

    public void setTipo(TipoJunta tipo) {
        this.tipo = tipo;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCanton() {
        return canton;
    }

    public void setCanton(String canton) {
        this.canton = canton;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public BigDecimal getMontoMultaInasistencia() {
        return montoMultaInasistencia;
    }

    public void setMontoMultaInasistencia(BigDecimal montoMultaInasistencia) {
        this.montoMultaInasistencia = montoMultaInasistencia;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public String getMotivoSuspension() {
        return motivoSuspension;
    }

    public void setMotivoSuspension(String motivoSuspension) {
        this.motivoSuspension = motivoSuspension;
    }
}
