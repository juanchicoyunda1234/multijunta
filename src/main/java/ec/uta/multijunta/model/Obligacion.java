package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@MappedSuperclass
public abstract class Obligacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @ManyToOne(optional = false)
    private Socio socio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoObligacion estado = EstadoObligacion.PENDIENTE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(length = 255)
    private String motivoAnulacion;

    @Version
    private Long version;

    public abstract String getConcepto();

    public abstract LocalDate getVencimiento();

    public abstract String getClave();

    public abstract String getTipo();

    public boolean tienePagosVigentes() {
        return saldoPendiente.compareTo(monto) < 0;
    }

    public boolean isCobrable() {
        return estado == EstadoObligacion.PENDIENTE || estado == EstadoObligacion.PARCIAL;
    }

    public boolean isVencida() {
        return isCobrable() && getVencimiento() != null && getVencimiento().isBefore(LocalDate.now());
    }

    public LocalDate getFechaOrden() {
        return getVencimiento() != null ? getVencimiento() : fecha;
    }

    public void aplicar(BigDecimal importe) {
        saldoPendiente = saldoPendiente.subtract(importe);
        recalcular();
    }

    public void restituir(BigDecimal importe) {
        saldoPendiente = saldoPendiente.add(importe);
        recalcular();
    }

    public void anular(String motivo) {
        estado = EstadoObligacion.ANULADA;
        saldoPendiente = BigDecimal.ZERO;
        motivoAnulacion = motivo;
    }

    private void recalcular() {
        if (saldoPendiente.signum() == 0) {
            estado = EstadoObligacion.PAGADA;
        } else if (saldoPendiente.compareTo(monto) < 0) {
            estado = EstadoObligacion.PARCIAL;
        } else {
            estado = EstadoObligacion.PENDIENTE;
        }
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

    public EstadoObligacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoObligacion estado) {
        this.estado = estado;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public Long getVersion() {
        return version;
    }
}
