package ec.uta.multijunta.model;

public enum EstadoActivo {
    NUEVO("Nuevo"),
    USADO("Usado"),
    DADO_DE_BAJA("Dado de baja");

    private final String etiqueta;

    EstadoActivo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
