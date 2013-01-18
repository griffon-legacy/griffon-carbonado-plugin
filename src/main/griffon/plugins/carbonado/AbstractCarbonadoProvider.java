/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.carbonado;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.carbonado.Repository;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractCarbonadoProvider implements CarbonadoProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCarbonadoProvider.class);
    private static final String DEFAULT = "default";

    public <R> R withCarbonado(Closure<R> closure) {
        return withCarbonado(DEFAULT, closure);
    }

    public <R> R withCarbonado(String repositoryName, Closure<R> closure) {
        if (isBlank(repositoryName)) repositoryName = DEFAULT;
        if (closure != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on repositoryName '" + repositoryName + "'");
            }
            return closure.call(repositoryName, getRepository(repositoryName));
        }
        return null;
    }

    public <R> R withCarbonado(CallableWithArgs<R> callable) {
        return withCarbonado(DEFAULT, callable);
    }

    public <R> R withCarbonado(String repositoryName, CallableWithArgs<R> callable) {
        if (isBlank(repositoryName)) repositoryName = DEFAULT;
        if (callable != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on repositoryName '" + repositoryName + "'");
            }
            callable.setArgs(new Object[]{repositoryName, getRepository(repositoryName)});
            return callable.call();
        }
        return null;
    }

    protected abstract Repository getRepository(String repositoryName);
}