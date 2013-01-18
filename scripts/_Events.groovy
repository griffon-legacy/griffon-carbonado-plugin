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

eventCopyLibsEnd = { jardir ->
    def config = new ConfigSlurper().parse(Config)
    if(buildConfig.carbonado.jars.keep) return
    switch(config.carbonado.repository) {
        case 'bdb':
            new File(jardir).eachFileMatch(~/.*hsqldb.*|.*commons-dbcp.*|.*commons-pool.*/) { jar ->
                ant.delete(file: jar, failonerror: false, quiet: true)
            }
            break
        case 'jdbc':
            new File(jardir).eachFileMatch(~/.*berkeleydb.*|.*carbonado-sleepycat.*/) { jar ->
                ant.delete(file: jar, failonerror: false, quiet: true)
            }
            break
        case 'map':
            new File(jardir).eachFileMatch(~/.*hsqldb.*|.*commons-dbcp.*|.*commons-pool.*|.*berkeleydb.*|.*carbonado-sleepycat.*/) { jar ->
                ant.delete(file: jar, failonerror: false, quiet: true)
            }
            break
    }
}
