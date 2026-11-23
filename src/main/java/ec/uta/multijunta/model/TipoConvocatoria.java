package ec.uta.multijunta.model;

public enum TipoConvocatoria {
    ASAMBLEA("Asamblea"),
    MINGA("Minga");

    private final String etiqueta;

    TipoConvocatoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
