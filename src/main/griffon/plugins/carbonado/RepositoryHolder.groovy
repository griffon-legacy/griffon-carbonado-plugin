/*
 * Copyright 2011-2013 the original author or authors.
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
import static griffon.util.GriffonNameUtils.isBlank

/**
 * @author Andres Almiray
 */
class RepositoryHolder {
    private static final String DEFAULT = 'default'
    private static final Object[] LOCK = new Object[0]
    private final Map<String, Repository> repositories = [:]

    private static final RepositoryHolder INSTANCE

    static {
        INSTANCE = new RepositoryHolder()
    }

    static RepositoryHolder getInstance() {
        INSTANCE
    }

    String[] getRepositoryNames() {
        List<String> repositoryNames = new ArrayList().addAll(repositories.keySet())
        repositoryNames.toArray(new String[repositoryNames.size()])
    }

    Repository getRepository(String repositoryName = DEFAULT) {
        if(isBlank(repositoryName)) repositoryName = DEFAULT
        retrieveRepository(repositoryName)
    }

    void setRepository(String repositoryName = DEFAULT, Repository repository) {
        if(isBlank(repositoryName)) repositoryName = DEFAULT
        storeRepository(repositoryName, repository)
    }

    boolean isRepositoryConnected(String repositoryName) {
        if(isBlank(repositoryName)) repositoryName = DEFAULT
        retrieveRepository(repositoryName) != null
    }
    
    void disconnectRepository(String repositoryName) {
        if(isBlank(repositoryName)) repositoryName = DEFAULT
        storeRepository(repositoryName, null)
    }

    Repository fetchRepository(String repositoryName) {
        if(isBlank(repositoryName)) repositoryName = DEFAULT
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
