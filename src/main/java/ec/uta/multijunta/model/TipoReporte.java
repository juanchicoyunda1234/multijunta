package ec.uta.multijunta.model;

public enum TipoReporte {
    BALANCE("Balance General de Ingresos y Egresos"),
    MOROSOS("Socios Morosos");

    private final String etiqueta;

    TipoReporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
