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

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.ConfigUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.sql.Sql
import java.sql.Connection
import javax.sql.DataSource

import org.apache.commons.pool.ObjectPool
import org.apache.commons.pool.impl.GenericObjectPool
import org.apache.commons.dbcp.ConnectionFactory
import org.apache.commons.dbcp.PoolingDataSource
import org.apache.commons.dbcp.PoolableConnectionFactory
import org.apache.commons.dbcp.DriverManagerConnectionFactory

import com.amazon.carbonado.Repository
import com.amazon.carbonado.repo.jdbc.JDBCRepositoryBuilder
import com.amazon.carbonado.repo.jdbc.JDBCConnectionCapability
import com.amazon.carbonado.repo.map.MapRepositoryBuilder
import com.amazon.carbonado.repo.sleepycat.BDBRepositoryBuilder

/**
 * @author Andres Almiray
 */
@Singleton
final class CarbonadoConnector {
    private static final String DEFAULT = 'default'
    private static final Logger LOG = LoggerFactory.getLogger(CarbonadoConnector)
    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.carbonado) {
            app.config.pluginConfig.carbonado = ConfigUtils.loadConfigWithI18n('CarbonadoConfig')
        }
        app.config.pluginConfig.carbonado
    }

    private ConfigObject narrowConfig(ConfigObject config, String repositoryName) {
        return repositoryName == DEFAULT ? config.repository : config.repositories[repositoryName]
    }

    Repository connect(GriffonApplication app, ConfigObject config, String repositoryName = DEFAULT) {
        if (RepositoryHolder.instance.isRepositoryConnected(repositoryName)) {
            return RepositoryHolder.instance.getRepository(repositoryName)
        }

        config = narrowConfig(config, repositoryName)
        app.event('CarbonadoConnectStart', [config, repositoryName])
        Repository repository = startCarbonado(config, repositoryName)
        RepositoryHolder.instance.setRepository(repositoryName, repository)
        bootstrap = app.class.classLoader.loadClass('BootstrapCarbonado').newInstance()
        bootstrap.metaClass.app = app
        resolveCarbonadoProvider(app).withCarbonado { rn, r -> bootstrap.init(rn, r) }
        app.event('CarbonadoConnectEnd', [repositoryName, repository])
        repository
    }

    void disconnect(GriffonApplication app, ConfigObject config, String repositoryName = DEFAULT) {
        if (RepositoryHolder.instance.isRepositoryConnected(repositoryName)) {
            config = narrowConfig(config, repositoryName)
            Repository repository = RepositoryHolder.instance.getRepository(repositoryName)
            app.event('CarbonadoDisconnectStart', [config, repositoryName, repository])
            resolveCarbonadoProvider(app).withCarbonado { rn, r -> bootstrap.destroy(rn, r) }
            stopCarbonado(config, repositoryName, repository)
            app.event('CarbonadoDisconnectEnd', [config, repositoryName])
            RepositoryHolder.instance.disconnectRepository(repositoryName)
        }
    }

    CarbonadoProvider resolveCarbonadoProvider(GriffonApplication app) {
        def carbonadoProvider = app.config.carbonadoProvider
        if (carbonadoProvider instanceof Class) {
            carbonadoProvider = carbonadoProvider.newInstance()
            app.config.carbonadoProvider = carbonadoProvider
        } else if (!carbonadoProvider) {
            carbonadoProvider = DefaultCarbonadoProvider.instance
            app.config.carbonadoProvider = carbonadoProvider
        }
        carbonadoProvider
    }

    private Repository startCarbonado(ConfigObject config, String repositoryName) {
        switch(config.type) {
            case 'jdbc':
                return createJDBCRepository(config.jdbc, repositoryName)
            case 'bdb':
                return createBDBRepository(config.bdb, repositoryName)
        }
        return createMapRepository(config.map, repositoryName)
    }

    private Repository createJDBCRepository(ConfigObject config, String repositoryName) {
        DataSource dataSource = createDataSource(config)
        def skipSchema = config.schema?.skip ?: false
        if (!skipSchema) createSchema(config, repositoryName, dataSource)
        
        JDBCRepositoryBuilder builder = new JDBCRepositoryBuilder()
        builder.name = repositoryName
        builder.dataSource = dataSource
        builder.build()
    }

    private Repository createBDBRepository(ConfigObject config, String repositoryName) {
        BDBRepositoryBuilder builder = new BDBRepositoryBuilder()
        builder.name = repositoryName
        config.each { propName, propValue ->
            builder[propName] = propValue
        }
        builder.build()
    }

    private Repository createMapRepository(ConfigObject config, String repositoryName) {
        MapRepositoryBuilder builder = new MapRepositoryBuilder()
        builder.name = repositoryName
        config.each { propName, propValue ->
            builder[propName] = propValue
        }
        builder.build()
    }

    private void stopCarbonado(ConfigObject config, String repositoryName, Repository repository) {
        switch(config.type) {
            case 'jdbc':
                disconnectJDBCRepository(config.jdbc, repositoryName, repository)
                break
            /*    
            case 'bdb':
                disconnectBDBRepository(config, repositoryName, repository)
                break
            case 'map':
            default:
                disconnectMapRepository(config, repositoryName, repository)
            */
        }
    }

    private DataSource createDataSource(ConfigObject config) {
        Class.forName(config.driverClassName.toString())
        ObjectPool connectionPool = new GenericObjectPool(null)
        if (config.pool) {
            if (config.pool.maxWait != null) connectionPool.maxWait = config.pool.maxWait
            if (config.pool.maxIdle != null) connectionPool.maxIdle = config.pool.maxIdle
            if (config.pool.maxActive != null) connectionPool.maxActive = config.pool.maxActive
        }

        String url = config.url.toString()
        String username = config.username.toString()
        String password = config.password.toString()
        ConnectionFactory connectionFactory = null
        if (username) {
            connectionFactory = new DriverManagerConnectionFactory(url, username, password)
        } else {
            connectionFactory = new DriverManagerConnectionFactory(url, null)
        }
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true)
        new PoolingDataSource(connectionPool)
    }

    private void createSchema(ConfigObject config, String repositoryName, DataSource dataSource) {
        String dbCreate = config.dbCreate.toString()
        if (dbCreate != 'create') return

        String env = getEnvironmentShortName()
        URL ddl = null
        for(String schemaName : [repositoryName + '-schema-'+ env +'.ddl', repositoryName + '-schema.ddl', 'schema-'+ env +'.ddl', 'schema.ddl']) {
            ddl = getClass().classLoader.getResource(schemaName)
            if (!ddl) {
                LOG.warn("DataSource[${repositoryName}].dbCreate was set to 'create' but ${schemaName} was not found in classpath.")
            } else {
                break
            }
        }
        if(!ddl) {
            LOG.error("DataSource[${repositoryName}].dbCreate was set to 'create' but no suitable schema was found in classpath.")
            return
        }

        boolean tokenizeddl = config.tokenizeddl ?: false
        withSql(dataSource) { sql ->
            if (!tokenizeddl) {
                sql.execute(ddl.text)
            } else {
                ddl.text.split(';').each { stmnt ->
                    if (stmnt?.trim()) sql.execute(stmnt + ';')
                }
            }
        }
    }

    private String getEnvironmentShortName() {
        switch(Environment.current) {
            case Environment.DEVELOPMENT: return 'dev'
            case Environment.TEST: return 'test'
            case Environment.PRODUCTION: return 'prod'
            default: return Environment.current.name
        }
    }

    private void withSql(DataSource dataSource, Closure closure) {
        Connection connection = dataSource.getConnection()
        try {
            closure(new Sql(connection))
        } finally {
            connection.close()
        }
    }

    private void disconnectJDBCRepository(ConfigObject config, String repositoryName, Repository repository) {
        Connection connection = null
        try {
            connection = repository.dataSource.getConnection()
            if(connection.metaData.databaseProductName == 'HSQL Database Engine') {
                connection.createStatement().executeUpdate('SHUTDOWN')
            }
        } finally {
            connection?.close()
        }
    }
}