package ec.uta.multijunta.model;

public enum FormatoReporte {
    PDF("PDF"),
    EXCEL("Excel");

    private final String etiqueta;

    FormatoReporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
