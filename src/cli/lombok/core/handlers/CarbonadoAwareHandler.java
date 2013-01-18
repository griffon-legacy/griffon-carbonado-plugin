/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lombok.core.handlers;

import lombok.ast.Expression;
import lombok.ast.IMethod;
import lombok.ast.IType;

import static lombok.ast.AST.*;

/**
 * @author Andres Almiray
 */
public abstract class CarbonadoAwareHandler<TYPE_TYPE extends IType<? extends IMethod<?, ?, ?, ?>, ?, ?, ?, ?, ?>> extends AbstractHandler<TYPE_TYPE> implements CarbonadoAwareConstants {
    private Expression<?> defaultCarbonadoProviderInstance() {
        return Call(Name(DEFAULT_CARBONADO_PROVIDER_TYPE), "getInstance");
    }

    public void addCarbonadoProviderField(final TYPE_TYPE type) {
        addField(type, CARBONADO_PROVIDER_TYPE, CARBONADO_PROVIDER_FIELD_NAME, defaultCarbonadoProviderInstance());
    }

    public void addCarbonadoProviderAccessors(final TYPE_TYPE type) {
        type.editor().injectMethod(
            MethodDecl(Type(VOID), METHOD_SET_CARBONADO_PROVIDER)
                .makePublic()
                .withArgument(Arg(Type(CARBONADO_PROVIDER_TYPE), PROVIDER))
                .withStatement(
                    If(Equal(Name(PROVIDER), Null()))
                        .Then(Block()
                            .withStatement(Assign(Field(CARBONADO_PROVIDER_FIELD_NAME), defaultCarbonadoProviderInstance())))
                        .Else(Block()
                            .withStatement(Assign(Field(CARBONADO_PROVIDER_FIELD_NAME), Name(PROVIDER)))))
        );

        type.editor().injectMethod(
            MethodDecl(Type(CARBONADO_PROVIDER_TYPE), METHOD_GET_CARBONADO_PROVIDER)
                .makePublic()
                .withStatement(Return(Field(CARBONADO_PROVIDER_FIELD_NAME)))
        );
    }

    public void addCarbonadoContributionMethods(final TYPE_TYPE type) {
        delegateMethodsTo(type, METHODS, Field(CARBONADO_PROVIDER_FIELD_NAME));
    }
}
