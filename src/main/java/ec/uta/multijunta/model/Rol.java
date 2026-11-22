package ec.uta.multijunta.model;

public enum Rol {
    SUPER_ADMIN("Super Administrador"),
    ADMIN_JUNTA("Administrador de la Junta"),
    SECRETARIO("Secretario/a"),
    TESORERO("Tesorero/a"),
    SOCIO("Socio");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
