package ec.uta.multijunta.model;

public enum EstadoCorreccion {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String etiqueta;

    EstadoCorreccion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
