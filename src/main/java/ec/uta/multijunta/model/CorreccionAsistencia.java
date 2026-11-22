package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
public class CorreccionAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @ManyToOne(optional = false)
    private Asistencia asistencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAsistencia estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAsistencia estadoNuevo;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCorreccion estado = EstadoCorreccion.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaResolucion;

    @ManyToOne(optional = false)
    private Usuario solicitante;

    @ManyToOne
    private Usuario resolutor;

    @PrePersist
    void alCrear() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
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

    public Asistencia getAsistencia() {
        return asistencia;
    }

    public void setAsistencia(Asistencia asistencia) {
        this.asistencia = asistencia;
    }

    public EstadoAsistencia getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoAsistencia estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoAsistencia getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoAsistencia estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public EstadoCorreccion getEstado() {
        return estado;
    }

    public void setEstado(EstadoCorreccion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public Usuario getResolutor() {
        return resolutor;
    }

    public void setResolutor(Usuario resolutor) {
        this.resolutor = resolutor;
    }
}
