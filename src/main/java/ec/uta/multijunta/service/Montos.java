package ec.uta.multijunta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Montos {

    private Montos() {
    }

    public static void validar(BigDecimal monto, String campo) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("monto.positivo", campo);
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("monto.decimales", campo);
        }
    }

    public static BigDecimal normalizar(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static String texto(BigDecimal monto) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return formato.format(monto == null ? BigDecimal.ZERO : monto);
    }
}
