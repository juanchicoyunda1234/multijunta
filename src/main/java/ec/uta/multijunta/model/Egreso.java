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
import java.time.LocalDate;

@Entity
public class Egreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaEgreso categoria;

    @Column(nullable = false, length = 160)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 120)
    private String proveedor;

    private String comprobanteNombre;

    @ManyToOne(optional = false)
    private Usuario registradoPor;

    public boolean isPendienteSoporte() {
        return comprobanteNombre == null;
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

    public CategoriaEgreso getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaEgreso categoria) {
        this.categoria = categoria;
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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getComprobanteNombre() {
        return comprobanteNombre;
    }

    public void setComprobanteNombre(String comprobanteNombre) {
        this.comprobanteNombre = comprobanteNombre;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }
}
