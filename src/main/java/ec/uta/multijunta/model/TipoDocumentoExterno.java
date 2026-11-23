package ec.uta.multijunta.model;

public enum TipoDocumentoExterno {
    OFICIO("Oficio"),
    NOTIFICACION("Notificación"),
    OTRO("Otro");

    private final String etiqueta;

    TipoDocumentoExterno(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
