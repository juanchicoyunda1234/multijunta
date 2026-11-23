package ec.uta.multijunta.model;

public enum EstadoObligacion {
    PENDIENTE("Pendiente"),
    PARCIAL("Parcial"),
    PAGADA("Pagada"),
    ANULADA("Anulada");

    private final String etiqueta;

    EstadoObligacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
