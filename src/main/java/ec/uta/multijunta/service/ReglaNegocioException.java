package ec.uta.multijunta.service;

public class ReglaNegocioException extends RuntimeException {

    private final String clave;

    public ReglaNegocioException(String clave) {
        super(clave);
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
