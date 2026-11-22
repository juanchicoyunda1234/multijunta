package ec.uta.multijunta.model;

public enum TipoEvento {
    ASAMBLEA("Asamblea"),
    MINGA("Minga"),
    REUNION("Reunión");

    private final String etiqueta;

    TipoEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
