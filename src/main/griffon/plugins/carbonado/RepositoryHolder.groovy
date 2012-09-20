/*
 * Copyright 2011-2012 the original author or authors.
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

package griffon.plugins.carbonado

import com.amazon.carbonado.Repository

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import griffon.util.CallableWithArgs
import static griffon.util.GriffonNameUtils.isBlank

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
class RepositoryHolder implements CarbonadoProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryHolder)
    private static final Object[] LOCK = new Object[0]
    private final Map<String, Repository> repositories = [:]

    String[] getRepositoryNames() {
        List<String> repositoryNames = new ArrayList().addAll(repositories.keySet())
        repositoryNames.toArray(new String[repositoryNames.size()])
    }

    Repository getRepository(String repositoryName = 'default') {
        if(isBlank(repositoryName)) repositoryName = 'default'
        retrieveRepository(repositoryName)
    }

    void setRepository(String repositoryName = 'default', Repository repository) {
        if(isBlank(repositoryName)) repositoryName = 'default'
        storeRepository(repositoryName, repository)
    }

    Object withCarbonado(String repositoryName = 'default', Closure closure) {
        Repository repository = fetchRepository(repositoryName)
        if(LOG.debugEnabled) LOG.debug("Executing statement on repository '$repositoryName'")
        return closure(repositoryName, repository)
    }

    public <T> T withCarbonado(String repositoryName = 'default', CallableWithArgs<T> callable) {
        Repository repository = fetchRepository(repositoryName)
        if(LOG.debugEnabled) LOG.debug("Executing statement on repository '$repositoryName'")
        callable.args = [repositoryName, repository] as Object[]
        return callable.call()
    }
    
    boolean isRepositoryConnected(String repositoryName) {
        if(isBlank(repositoryName)) repositoryName = 'default'
        retrieveRepository(repositoryName) != null
    }
    
    void disconnectRepository(String repositoryName) {
        if(isBlank(repositoryName)) repositoryName = 'default'
        storeRepository(repositoryName, null)        
    }

    private Repository fetchRepository(String repositoryName) {
        if(isBlank(repositoryName)) repositoryName = 'default'
        Repository repository = retrieveRepository(repositoryName)
        if(repository == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = CarbonadoConnector.instance.createConfig(app)
            repository = CarbonadoConnector.instance.connect(app, config, repositoryName)
        }

        if(repository == null) {
            throw new IllegalArgumentException("No such carbonado repository configuration for name $repositoryName")
        }
        repository
    }

    private Repository retrieveRepository(String repositoryName) {
        synchronized(LOCK) {
            repositories[repositoryName]
        }
    }

    private void storeRepository(String repositoryName, Repository repository) {
        synchronized(LOCK) {
            repositories[repositoryName] = repository
        }
    }
}
