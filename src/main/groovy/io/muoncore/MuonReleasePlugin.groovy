package io.muoncore

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.external.javadoc.JavadocMemberLevel

class MuonReleasePlugin implements Plugin<Project> {
    void apply(Project project) {

        project.subprojects { subproject ->

            def theRepo = "muon"

            if (project.hasProperty("repoKey")) {
                println "REPO KEY IS ${project.repoKey}"
                theRepo = project.repoKey
            }
//      println "ADDING PROJECT $subproject $project.repoKey"
            if (project.exclude) {
                def excludes = project.exclude.split(",").collect { ":$it" }
                if (subproject.name in excludes) return
            }
            subproject.apply plugin: 'maven'
            subproject.apply plugin: 'maven-publish'
            subproject.apply plugin: "com.jfrog.artifactory"
            subproject.apply plugin: 'io.franzbecker.gradle-lombok'
            subproject.apply plugin: 'groovy'

            subproject.group = project.group
            subproject.version = project.version

            subproject.dependencies {
                testCompile "org.codehaus.groovy:groovy-all:2.4.8"
                testCompile "org.spockframework:spock-core:1.0-groovy-2.4"
                testCompile 'cglib:cglib:2.2.2'
                testCompile 'org.objenesis:objenesis:2.4'
            }

            subproject.compileJava {
                sourceCompatibility = "1.8"
                targetCompatibility = "1.8"
            }

            subproject.compileTestJava {
                sourceCompatibility = "1.8"
                targetCompatibility = "1.8"
            }

            subproject.repositories {
                jcenter()
                maven {
                    url "https://simplicityitself.jfrog.io/simplicityitself/muon"
                }
            }

            subproject.publishing {
                publications {
                    mavenJava(MavenPublication) {
                        from components.java
                    }
                }
            }

            subproject.javadoc {
                source = sourceSets.main.allJava
                classpath = configurations.compileClasspath

                options
                        {
                            setMemberLevel JavadocMemberLevel.PUBLIC
                            setAuthor true

                            links "https://docs.oracle.com/javase/8/docs/api/"
                        }
            }

// ----------- Deployment --------------
            subproject.artifactory {
                contextUrl = 'https://simplicityitself.jfrog.io/simplicityitself/'
                //The base Artifactory URL if not overridden by the publisher/resolver
                publish {
                    repository {
                        repoKey = theRepo   //The Artifactory repository key to publish to
                        username = 'sergio'          //The publisher user name
                        password = 'cistechfutures'       //The publisher password
                    }
                    defaults {
                        publications('mavenJava')
                        publishArtifacts = true
                        publishPom = true
                    }
                }
                resolve {
                    repository {
                        repoKey = theRepo  //The Artifactory (preferably virtual) repository key to resolve from
                    }
                }
            }

            subproject.install {
                repositories.mavenInstaller {
                    pom.project {
                        licenses {
                            license {
                                name 'The Apache Software License, Version 2.0'
                                url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                                distribution 'repo'
                            }
                        }
                    }
//                    pom.withXml {
//                        asNode().appendNode()
//                        }
//                    }
                }
            }

            if (project.plugins.hasPlugin('java')) {

                subproject.publishing {
                    publications {
                        shadow(MavenPublication) { publication ->
                            project.shadow.component(publication)
                            pom.withXml {
                                asNode().appendNode('name', subproject.name)
                                asNode().appendNode('description',             'A demonstration of maven POM customization')
                                asNode().appendNode('url', 'http://maven.apache.org')
                                def license = asNode().appendNode("licenses").appendNode("license")
                                license.appendNode("name", "LGPLv3")
                                license.appendNode("url", "LGPLv3")
                                license.appendNode("distribution", "repo")

                                asNode().appendNode("scm").appendNode("url", "git@github.com")
                                println "PACKAGING ${asNode().packaging[0].value}"
                                asNode().packaging[0].value = 'jar'
                            }
                        }
                    }
                }

                // manifest.mainAttributes(provider: 'gradle')
                subproject.configurations {
                    published
                }




                subproject.dependencies {
                    testCompile 'junit:junit:4.7'
                }

                subproject.task sourceJar(type: Jar) {
                    from sourceSets.main.allSource
                    classifier = 'sources'
                }
                subproject.task javadocJar(type: Jar, dependsOn: javadoc) {
                    classifier = 'javadoc'
                    from javadoc.destinationDir
                }

                // Add the sourceJars to non-extractor modules
                subproject.artifacts {
                    published sourceJar
                    published javadocJar
                }

                subproject.task sourceJar(type: Jar) {
                    from sourceSets.main.allJava
                }
                subproject.task packageJavadoc(type: Jar) {
                    from javadoc
                    classifier = 'javadoc'
                }
            }
            configurations {
                published
            }
        }
    }
}
