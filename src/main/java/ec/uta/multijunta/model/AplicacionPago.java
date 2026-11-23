package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class AplicacionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Pago pago;

    @ManyToOne
    private Multa multa;

    @ManyToOne
    private Cuota cuota;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    public Obligacion getObligacion() {
        return multa != null ? multa : cuota;
    }

    public void setObligacion(Obligacion obligacion) {
        if (obligacion instanceof Multa m) {
            this.multa = m;
            this.cuota = null;
        } else {
            this.cuota = (Cuota) obligacion;
            this.multa = null;
        }
    }

    public String getTipo() {
        return multa != null ? "Multa" : "Cuota";
    }

    public Long getId() {
        return id;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public Multa getMulta() {
        return multa;
    }

    public Cuota getCuota() {
        return cuota;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}
