package ec.uta.multijunta.model;

public enum EstadoItem {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String etiqueta;

    EstadoItem(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
