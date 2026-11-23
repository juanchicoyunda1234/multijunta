package ec.uta.multijunta.service;

public enum DestinoCuota {
    TODOS("Todos los socios activos"),
    SECTOR("Un sector"),
    SOCIO("Un socio");

    private final String etiqueta;

    DestinoCuota(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
