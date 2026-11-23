package ec.uta.multijunta.model;

public enum PrioridadConvocatoria {
    NORMAL("Normal"),
    URGENTE("Urgente");

    private final String etiqueta;

    PrioridadConvocatoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
