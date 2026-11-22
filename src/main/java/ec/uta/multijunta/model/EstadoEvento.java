package ec.uta.multijunta.model;

public enum EstadoEvento {
    PROGRAMADO("Programado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
