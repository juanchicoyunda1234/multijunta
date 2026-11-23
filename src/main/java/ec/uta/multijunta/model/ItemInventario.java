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

@Entity
public class ItemInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String unidadMedida;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoItem estado = EstadoItem.ACTIVO;

    public boolean isCritico() {
        return stock.compareTo(stockMinimo) <= 0;
    }

    public boolean isActivo() {
        return estado == EstadoItem.ACTIVO;
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

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public EstadoItem getEstado() {
        return estado;
    }

    public void setEstado(EstadoItem estado) {
        this.estado = estado;
    }
}
