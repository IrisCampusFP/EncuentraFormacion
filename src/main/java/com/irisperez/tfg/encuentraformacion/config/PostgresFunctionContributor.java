package com.irisperez.tfg.encuentraformacion.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

// Registra unaccent en Hibernate 6 con tipo de retorno STRING.
/** Si no se registra, Hibernate no sabe cómo mapear el resultado de unaccent a un
 * tipo Java, lo que causa errores al usar esta función en consultas JPQL o Criteria. */
public class PostgresFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
            "unaccent",
            "unaccent(?1)",
            functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.STRING)
        );
    }
}
