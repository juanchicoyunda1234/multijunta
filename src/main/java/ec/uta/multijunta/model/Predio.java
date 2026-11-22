package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
public class Predio {

    public static final BigDecimal LATITUD_MINIMA = new BigDecimal("-5.1");
    public static final BigDecimal LATITUD_MAXIMA = new BigDecimal("2.0");
    public static final BigDecimal LONGITUD_MINIMA = new BigDecimal("-92.1");
    public static final BigDecimal LONGITUD_MAXIMA = new BigDecimal("-75.1");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @ManyToOne(optional = false)
    private Socio socio;

    @ManyToOne(optional = false)
    private Sector sector;

    @Column(nullable = false, length = 20)
    private String codigo;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal area;

    @Size(max = 200)
    @Column(length = 200)
    private String ubicacion;

    @NotNull
    @DecimalMin("-5.1")
    @DecimalMax("2.0")
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @NotNull
    @DecimalMin("-92.1")
    @DecimalMax("-75.1")
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(length = 100)
    private String fotoNombre;

    public static boolean dentroDelEcuador(BigDecimal latitud, BigDecimal longitud) {
        return latitud.compareTo(LATITUD_MINIMA) >= 0 && latitud.compareTo(LATITUD_MAXIMA) <= 0
                && longitud.compareTo(LONGITUD_MINIMA) >= 0 && longitud.compareTo(LONGITUD_MAXIMA) <= 0;
    }

    public Long getId() {
        return id;
    }

    public Junta getJunta() {
        return junta;
    }

    public void setJunta(Junta junta) {
        this.junta = junta;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
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

    public String getFotoNombre() {
        return fotoNombre;
    }

    public void setFotoNombre(String fotoNombre) {
        this.fotoNombre = fotoNombre;
    }
}
