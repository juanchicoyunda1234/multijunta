package ec.uta.multijunta.model;

public enum EstadoConvocatoria {
    BORRADOR("Borrador"),
    PUBLICADA("Publicada");

    private final String etiqueta;

    EstadoConvocatoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
