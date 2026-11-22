package ec.uta.multijunta.model;

public enum EstadoCuenta {
    SIN_CUENTA("Sin cuenta"),
    ACTIVA("Activa"),
    DESACTIVADA("Desactivada");

    private final String etiqueta;

    EstadoCuenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
