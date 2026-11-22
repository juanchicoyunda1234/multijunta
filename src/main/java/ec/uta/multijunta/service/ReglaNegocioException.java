package ec.uta.multijunta.service;

public class ReglaNegocioException extends RuntimeException {

    private final String clave;
    private final String campo;

    public ReglaNegocioException(String clave) {
        this(clave, null);
    }

    public ReglaNegocioException(String clave, String campo) {
        super(clave);
        this.clave = clave;
        this.campo = campo;
    }

    public String getClave() {
        return clave;
    }

    public String getCampo() {
        return campo;
    }
}
