package ec.uta.multijunta.model;

public enum CategoriaEgreso {
    MATERIALES("Materiales"),
    HONORARIOS("Honorarios"),
    SERVICIOS_BASICOS("Servicios básicos"),
    MANTENIMIENTO("Mantenimiento"),
    OTROS("Otros");

    private final String etiqueta;

    CategoriaEgreso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
