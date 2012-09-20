griffon.project.dependency.resolution = {
    inherits "global"  
    log "warn"
    repositories {
        griffonHome()
        mavenCentral()
        String basePath = pluginDirPath? "${pluginDirPath}/" : ''
        flatDir name: "carbonadoLibDir", dirs: ["${basePath}lib"]
    }
    dependencies {
        compile('commons-dbcp:commons-dbcp:1.4',
                'commons-pool:commons-pool:1.6',
                'com.h2database:h2:1.3.168',
                'joda-time:joda-time:2.1') {
            transitive = false
        }
        String carbonadoVersion = '1.2.3'
        compile 'com.sleepycat:berkeleydb-je:5.0.58',
                "com.amazon:carbonado:$carbonadoVersion",
                "com.amazon:carbonado-sleepycat-db:$carbonadoVersion",
                "com.amazon:carbonado-sleepycat-je:$carbonadoVersion",
                'org.cojen:cojen:2.2.3'
    }
}

griffon {
    doc {
        logo = '<a href="http://griffon.codehaus.org" target="_blank"><img alt="The Griffon Framework" src="../img/griffon.png" border="0"/></a>'
        sponsorLogo = "<br/>"
        footer = "<br/><br/>Made with Griffon (@griffon.version@)"
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
    }

    error 'org.codehaus.griffon',
          'org.springframework',
          'org.apache.karaf',
          'groovyx.net'
    warn  'griffon'
}