/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by getApplication()licable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
 
/**
 * @author Andres Almiray
 */
class CarbonadoGriffonPlugin {
    // the plugin version
    String version = '0.4'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '0.9.5 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [:]
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, gtk
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/griffon/griffon-carbonado-plugin'

    List authors = [
        [
            name: 'Andres Almiray',
            email: 'aalmiray@yahoo.com'
        ]
    ]
    String title = 'Carbonado support'
    String description = '''
The Carbonado plugin enables lightweight access to JDBC, BerkeleyDB or in-memory datastores using [Carbonado][1].
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * CarbonadoConfig.groovy - contains the repository definitions.
 * BootstrapCarbonado.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

A new dynamic method named `withCarbonado` will be injected into all controllers,
giving you access to a `com.amazon.carbonado.Repository` object, with which you'll be able
to make calls to the repository. Remember to make all repository calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.
This method is aware of multiple repositories. If no repositoryName is specified when calling
it then the default repository will be selected. Here are two example usages, the first
queries against the default repository while the second queries a repository whose name has
been configured as 'internal'

	package sample
	class SampleController {
	    def queryAllRepisitories = {
	        withCarbonado { repositoryName, repository -> ... }
	        withCarbonado('internal') { repositoryName, repository -> ... }
	    }
	}
	
This method is also accessible to any component through the singleton `griffon.plugins.carbonado.CarbonadoConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`CarbonadoEnhancer.enhance(metaClassInstance, carbonadoProviderInstance)`.

Configuration
-------------
### Dynamic method injection

The `withCarbonado()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.carbonado.injectInto = ['controller', 'service']

### Selecting a repository type

This plugin supports 3 types of repositories: JDBC, BerkeleyDB and in-memory. You can select which one to use by
configuring the selected type in the repository configuration (`CarbonadoConfig.groovy`).

This flag accepts the following values: `jdbc`, `bdb` and `map`. The last option is the default if no preference is specified.

### Events

The following events will be triggered by this addon

 * CarbonadoConnectStart[config, repositoryName] - triggered before connecting to the repository
 * CarbonadoConnectEnd[repositoryName, repository] - triggered after connecting to the repository
 * CarbonadoDisconnectStart[config, repositoryName, repository] - triggered before disconnecting from the repository
 * CarbonadoDisconnectEnd[config, repositoryName] - triggered after disconnecting from the repository

### Multiple Stores

The config file `CarbonadoConfig.groovy` defines a default repository block. As the name
implies this is the repository used by default, however you can configure named repositories
by adding a new config block. For example connecting to a repository whose name is 'internal'
can be done in this way

	repositories {
	    internal {
		    type = 'map'
		}
	}

This block can be used inside the `environments()` block in the same way as the
default repository block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/carbonado][2]

Testing
-------
The `withCarbonado()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `CarbonadoEnhancer.enhance(metaClassInstance, carbonadoProviderInstance)` where 
`carbonadoProviderInstance` is of type `griffon.plugins.carbonado.CarbonadoProvider`. The contract for this interface looks like this

    public interface CarbonadoProvider {
        Object withCarbonado(Closure closure);
        Object withCarbonado(String repositoryName, Closure closure);
        <T> T withCarbonado(CallableWithArgs<T> callable);
        <T> T withCarbonado(String repositoryName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyCarbonadoProvider implements CarbonadoProvider {
        Object withCarbonado(String repositoryName = 'default', Closure closure) { null }
        public <T> T withCarbonado(String repositoryName = 'default', CallableWithArgs<T> callable) { null }      
    }
    
This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            CarbonadoEnhancer.enhance(service.metaClass, new MyCarbonadoProvider())
            // exercise service methods
        }
    }


[1]: http://carbonado.sourceforge.net/
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/carbonado
'''
}
