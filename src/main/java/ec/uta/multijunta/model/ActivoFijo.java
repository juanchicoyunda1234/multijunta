package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
public class ActivoFijo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @Column(nullable = false, length = 14)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoriaActivo categoria;

    @Column(nullable = false)
    private LocalDate fechaAdquisicion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAdquisicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoActivo estado;

    private String archivoNombre;

    @Column(length = 255)
    private String motivoBaja;

    private LocalDate fechaBaja;

    public BigDecimal depreciacion(LocalDate hoy) {
        if (categoria == null || !hoy.isAfter(fechaAdquisicion)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long meses = ChronoUnit.MONTHS.between(fechaAdquisicion, hoy);
        BigDecimal calculada = valorAdquisicion.multiply(BigDecimal.valueOf(meses))
                .divide(BigDecimal.valueOf(categoria.getVidaUtil() * 12L), 2, RoundingMode.HALF_UP);
        return calculada.min(valorAdquisicion.setScale(2));
    }

    public BigDecimal valorEnLibros(LocalDate hoy) {
        return valorAdquisicion.setScale(2).subtract(depreciacion(hoy));
    }

    public BigDecimal getDepreciacionActual() {
        return depreciacion(LocalDate.now());
    }

    public BigDecimal getValorEnLibrosActual() {
        return valorEnLibros(LocalDate.now());
    }

    public boolean isDadoDeBaja() {
        return estado == EstadoActivo.DADO_DE_BAJA;
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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public CategoriaActivo getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaActivo categoria) {
        this.categoria = categoria;
    }

    public LocalDate getFechaAdquisicion() {
        return fechaAdquisicion;
    }

    public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion;
    }

    public BigDecimal getValorAdquisicion() {
        return valorAdquisicion;
    }

    public void setValorAdquisicion(BigDecimal valorAdquisicion) {
        this.valorAdquisicion = valorAdquisicion;
    }

    public EstadoActivo getEstado() {
        return estado;
    }

    public void setEstado(EstadoActivo estado) {
        this.estado = estado;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public void setArchivoNombre(String archivoNombre) {
        this.archivoNombre = archivoNombre;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public LocalDate getFechaBaja() {
        return fechaBaja;
    }

    public void setFechaBaja(LocalDate fechaBaja) {
        this.fechaBaja = fechaBaja;
    }
}
