package ec.uta.multijunta.model;

public enum CategoriaActivo {
    INFRAESTRUCTURA("Infraestructura", 20),
    MAQUINARIA("Maquinaria", 10),
    EQUIPO_OFICINA("Equipo de oficina", 10),
    VEHICULOS("Vehículos", 5);

    private final String etiqueta;
    private final int vidaUtil;

    CategoriaActivo(String etiqueta, int vidaUtil) {
        this.etiqueta = etiqueta;
        this.vidaUtil = vidaUtil;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getVidaUtil() {
        return vidaUtil;
    }
}
