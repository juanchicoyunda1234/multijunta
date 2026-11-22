package ec.uta.multijunta.service;

import ec.uta.multijunta.model.Socio;
import ec.uta.multijunta.repository.SocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocioService {

    private final SocioRepository socios;

    public SocioService(SocioRepository socios) {
        this.socios = socios;
    }

    @Transactional
    public Socio registrar(Socio socio) {
        if (socio.getJunta() == null
                || socios.existsByCodigoSocioAndJuntaId(socio.getCodigoSocio(), socio.getJunta().getId())) {
            throw new ReglaNegocioException("socio.codigoDuplicado");
        }
        return socios.save(socio);
    }

    @Transactional(readOnly = true)
    public String siguienteCodigo(Long juntaId) {
        long numero = socios.countByJuntaId(juntaId) + 1;
        String codigo = String.format("S-%04d", numero);
        while (socios.existsByCodigoSocioAndJuntaId(codigo, juntaId)) {
            numero++;
            codigo = String.format("S-%04d", numero);
        }
        return codigo;
    }
}
