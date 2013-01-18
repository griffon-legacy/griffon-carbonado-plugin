/*
 * Copyright 2011-2013 the original author or authors.
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
    String version = '1.0.0'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.2.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = [lombok: '0.4']
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
The Carbonado plugin enables lightweight access to JDBC, BerkeleyDB or in-memory
datastores using [Carbonado][1]. This plugin does NOT provide domain classes nor
dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in 
`$appdir/griffon-app/conf`:

 * CarbonadoConfig.groovy - contains the repository definitions.
 * BootstrapCarbonado.groovy - defines init/destroy hooks for data to be
   manipulated during app startup/shutdown.

A new dynamic method named `withCarbonado` will be injected into all controllers,
giving you access to a `com.amazon.carbonado.Repository` object, with which
you'll be able to make calls to the repository. Remember to make all repository
calls off the UI thread otherwise your application may appear unresponsive
when doing long computations inside the UI thread.

This method is aware of multiple repositories. If no repositoryName is specified
when calling it then the default repository will be selected. Here are two example
usages, the first queries against the default repository while the second queries
a repository whose name has been configured as 'internal'

    package sample
    class SampleController {
        def queryAllRepisitories = {
            withCarbonado { repositoryName, repository -> ... }
            withCarbonado('internal') { repositoryName, repository -> ... }
        }
    }

The following list enumerates all the variants of the injected method

 * `<R> R withCarbonado(Closure<R> stmts)`
 * `<R> R withCarbonado(CallableWithArgs<R> stmts)`
 * `<R> R withCarbonado(String repositoryName, Closure<R> stmts)`
 * `<R> R withCarbonado(String repositoryName, CallableWithArgs<R> stmts)`

These methods are also accessible to any component through the singleton
`griffon.plugins.carbonado.CarbonadoConnector`. You can inject these methods to
non-artifacts via metaclasses. Simply grab hold of a particular metaclass and
call `CarbonadoEnhancer.enhance(metaClassInstance, carbonadoProviderInstance)`.

Configuration
-------------
### CarbonadoAware AST Transformation

The preferred way to mark a class for method injection is by annotating it with
`@griffon.plugins.carbonado.CarbonadoAware`. This transformation injects the
`griffon.plugins.carbonado.CarbonadoContributionHandler` interface and default
behavior that fulfills the contract.

### Dynamic method injection

Dynamic methods will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.carbonado.injectInto = ['controller', 'service']

Dynamic method injection will be skipped for classes implementing
`griffon.plugins.carbonado.CarbonadoContributionHandler`.

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

Dynamic methods will not be automatically injected during unit testing, because
addons are simply not initialized for this kind of tests. However you can use
`CarbonadoEnhancer.enhance(metaClassInstance, carbonadoProviderInstance)` where
`carbonadoProviderInstance` is of type `griffon.plugins.carbonado.CarbonadoProvider`.
The contract for this interface looks like this

    public interface CarbonadoProvider {
        <R> R withCarbonado(Closure<R> closure);
        <R> R withCarbonado(CallableWithArgs<R> callable);
        <R> R withCarbonado(String repositoryName, Closure<R> closure);
        <R> R withCarbonado(String repositoryName, CallableWithArgs<R> callable);
    }

It's up to you define how these methods need to be implemented for your tests.
For example, here's an implementation that never fails regardless of the
arguments it receives

    class MyCarbonadoProvider implements CarbonadoProvider {
        public <R> R withCarbonado(Closure<R> closure) { null }
        public <R> R withCarbonado(CallableWithArgs<R> callable) { null }
        public <R> R withCarbonado(String repositoryName, Closure<R> closure) { null }
        public <R> R withCarbonado(String repositoryName, CallableWithArgs<R> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            CarbonadoEnhancer.enhance(service.metaClass, new MyCarbonadoProvider())
            // exercise service methods
        }
    }

On the other hand, if the service is annotated with `@CarbonadoAware` then usage
of `CarbonadoEnhancer` should be avoided at all costs. Simply set `carbonadoProviderInstance`
on the service instance directly, like so, first the service definition

    @griffon.plugins.carbonado.CarbonadoAware
    class MyService {
        def serviceMethod() { ... }
    }

Next is the test

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            service.carbonadoProvider = new MyCarbonadoProvider()
            // exercise service methods
        }
    }

Tool Support
------------

### DSL Descriptors

This plugin provides DSL descriptors for Intellij IDEA and Eclipse (provided
you have the Groovy Eclipse plugin installed). These descriptors are found
inside the `griffon-carbonado-compile-x.y.z.jar`, with locations

 * dsdl/carbonado.dsld
 * gdsl/carbonado.gdsl

### Lombok Support

Rewriting Java AST in a similar fashion to Groovy AST transformations is
possible thanks to the [lombok][3] plugin.

#### JavaC

Support for this compiler is provided out-of-the-box by the command line tools.
There's no additional configuration required.

#### Eclipse

Follow the steps found in the [Lombok][3] plugin for setting up Eclipse up to
number 5.

 6. Go to the path where the `lombok.jar` was copied. This path is either found
    inside the Eclipse installation directory or in your local settings. Copy
    the following file from the project's working directory

         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/carbonado-<version>/dist/griffon-carbonado-compile-<version>.jar .

 6. Edit the launch script for Eclipse and tweak the boothclasspath entry so
    that includes the file you just copied

        -Xbootclasspath/a:lombok.jar:lombok-pg-<version>.jar:\
        griffon-lombok-compile-<version>.jar:griffon-carbonado-compile-<version>.jar

 7. Launch Eclipse once more. Eclipse should be able to provide content assist
    for Java classes annotated with `@CarbonadoAware`.

#### NetBeans

Follow the instructions found in [Annotation Processors Support in the NetBeans
IDE, Part I: Using Project Lombok][4]. You may need to specify
`lombok.core.AnnotationProcessor` in the list of Annotation Processors.

NetBeans should be able to provide code suggestions on Java classes annotated
with `@CarbonadoAware`.

#### Intellij IDEA

Follow the steps found in the [Lombok][3] plugin for setting up Intellij IDEA
up to number 5.

 6. Copy `griffon-carbonado-compile-<version>.jar` to the `lib` directory

         $ pwd
           $USER_HOME/Library/Application Support/IntelliJIdea11/lombok-plugin
         $ cp $USER_HOME/.griffon/<version>/projects/<project>/plugins/carbonado-<version>/dist/griffon-carbonado-compile-<version>.jar lib

 7. Launch IntelliJ IDEA once more. Code completion should work now for Java
    classes annotated with `@CarbonadoAware`.


[1]: http://carbonado.sourceforge.net/
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/carbonado
[3]: /plugin/lombok
[4]: http://netbeans.org/kb/docs/java/annotations-lombok.html
'''
}