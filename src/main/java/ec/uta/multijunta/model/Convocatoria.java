package ec.uta.multijunta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Convocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Junta junta;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoConvocatoria tipo;

    @Column(nullable = false)
    private LocalDate fechaEvento;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 150)
    private String lugar;

    @Column(nullable = false, length = 4000)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadConvocatoria prioridad;

    @Column(nullable = false)
    private LocalDateTime fechaPublicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoConvocatoria estado = EstadoConvocatoria.BORRADOR;

    @ManyToOne(optional = false)
    private Usuario autor;

    public LocalDateTime getInicioEvento() {
        return fechaEvento.atTime(hora);
    }

    public boolean fijada(LocalDate hoy) {
        return tipo == TipoConvocatoria.ASAMBLEA && !fechaEvento.isBefore(hoy);
    }

    public boolean isFijada() {
        return fijada(LocalDate.now());
    }

    public boolean vencidaSinPublicar(LocalDateTime ahora) {
        return estado == EstadoConvocatoria.BORRADOR && !getInicioEvento().isAfter(ahora);
    }

    public boolean isVencidaSinPublicar() {
        return vencidaSinPublicar(LocalDateTime.now());
    }

    public boolean isBorrador() {
        return estado == EstadoConvocatoria.BORRADOR;
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public TipoConvocatoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoConvocatoria tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(LocalDate fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public PrioridadConvocatoria getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadConvocatoria prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public EstadoConvocatoria getEstado() {
        return estado;
    }

    public void setEstado(EstadoConvocatoria estado) {
        this.estado = estado;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }
}
