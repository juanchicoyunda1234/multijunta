package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = "socio_id"),
        @UniqueConstraint(columnNames = {"miembro_a_id", "miembro_b_id"})})
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @ManyToOne
    @JoinColumn(name = "socio_id")
    private Socio socio;

    @ManyToOne
    @JoinColumn(name = "miembro_a_id")
    private Usuario miembroA;

    @ManyToOne
    @JoinColumn(name = "miembro_b_id")
    private Usuario miembroB;

    @Column(nullable = false)
    private long ultimoLeidoA;

    @Column(nullable = false)
    private long ultimoLeidoB;

    @Column(nullable = false)
    private LocalDateTime actualizada = LocalDateTime.now();

    public boolean isConSocio() {
        return socio != null;
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

    public Usuario getMiembroA() {
        return miembroA;
    }

    public void setMiembroA(Usuario miembroA) {
        this.miembroA = miembroA;
    }

    public Usuario getMiembroB() {
        return miembroB;
    }

    public void setMiembroB(Usuario miembroB) {
        this.miembroB = miembroB;
    }

    public long getUltimoLeidoA() {
        return ultimoLeidoA;
    }

    public void setUltimoLeidoA(long ultimoLeidoA) {
        this.ultimoLeidoA = ultimoLeidoA;
    }

    public long getUltimoLeidoB() {
        return ultimoLeidoB;
    }

    public void setUltimoLeidoB(long ultimoLeidoB) {
        this.ultimoLeidoB = ultimoLeidoB;
    }

    public LocalDateTime getActualizada() {
        return actualizada;
    }

    public void setActualizada(LocalDateTime actualizada) {
        this.actualizada = actualizada;
    }
}
