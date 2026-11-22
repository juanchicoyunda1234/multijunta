package ec.uta.multijunta.service;

import ec.uta.multijunta.config.MatrizPermisos;
import ec.uta.multijunta.model.Rol;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    public List<MatrizPermisos.Entrada> opciones(Rol rol) {
        return MatrizPermisos.menuPara(rol);
    }
}
