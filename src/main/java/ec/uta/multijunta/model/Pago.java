package ec.uta.multijunta.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"junta_id", "numero"}))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @ManyToOne(optional = false)
    private Socio socio;

    @Column(nullable = false, length = 12)
    private String numero;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MetodoPago metodo;

    @ManyToOne(optional = false)
    private Usuario registradoPor;

    @Column(nullable = false)
    private boolean reversado;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<AplicacionPago> aplicaciones = new ArrayList<>();

    @OneToOne(mappedBy = "pago")
    private ReversoPago reverso;

    public void agregar(Obligacion obligacion, BigDecimal importe) {
        AplicacionPago aplicacion = new AplicacionPago();
        aplicacion.setPago(this);
        aplicacion.setObligacion(obligacion);
        aplicacion.setMonto(importe);
        aplicaciones.add(aplicacion);
        obligacion.aplicar(importe);
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

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    public boolean isReversado() {
        return reversado;
    }

    public void setReversado(boolean reversado) {
        this.reversado = reversado;
    }

    public List<AplicacionPago> getAplicaciones() {
        return aplicaciones;
    }

    public ReversoPago getReverso() {
        return reverso;
    }

    public void setReverso(ReversoPago reverso) {
        this.reverso = reverso;
    }
}
