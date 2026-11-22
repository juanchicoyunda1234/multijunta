package ec.uta.multijunta.model;

public enum TipoRecurso {
    COMPUERTA("Compuerta"),
    CANAL("Canal"),
    RESERVORIO("Reservorio");

    private final String etiqueta;

    TipoRecurso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
