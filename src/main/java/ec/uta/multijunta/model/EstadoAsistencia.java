package ec.uta.multijunta.model;

public enum EstadoAsistencia {
    PRESENTE("Presente"),
    AUSENTE("Ausente"),
    JUSTIFICADO("Justificado");

    private final String etiqueta;

    EstadoAsistencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
