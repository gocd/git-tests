import cd.go.contrib.plugins.configrepo.groovy.dsl.GitMaterial
import cd.go.contrib.plugins.configrepo.groovy.dsl.GoCD
import cd.go.contrib.plugins.configrepo.groovy.dsl.Job

final def versions = ["2.23.0", "2.22.0", "2.21.0", "2.20.1", "2.19.2", "2.18.1", "2.17.2", "2.16.5", "2.15.3", "2.14.5", "2.13.7", "2.12.5", "2.11.4",
                      "2.10.5", "2.9.5", "2.8.6", "2.7.6", "2.6.7", "2.5.6", "2.4.9", "2.3.9", "2.2.3", "2.1.4", "2.0.5", "1.9.5"]

def rpmMaterial = new GitMaterial("rpms", {
    url = "https://github.com/gocd/git-tests"
    shallowClone = true
    autoUpdate = true
    branch = "master"
    destination = "git-tests"
})

private static Job gitTestJob(String git_version) {
    new Job("test-${git_version}", {
        elasticProfileId = "ecs-gocd-OOM-tests-centos7"
        tasks {
            exec {
                commandLine = ['sudo', 'yum', 'remove', '-y', 'rh-git29', 'sclo-git212']
            }
            exec {
                commandLine = ['sudo', 'rm', '-rf', '/opt/rh/sclo-git*', "/etc/profile.d/sclo-git*.sh", '/opt/rh/rh-git*', "/etc/profile.d/rh-git*.sh"]
            }
            exec {
                commandLine = ['sudo', 'rpm', '-i', "git-${git_version}-x86_64.rpm"]
                workingDir = "git-tests/rpms"
            }
            exec {
                commandLine = ['git', '--version']
            }
            exec {
                commandLine = ['./gradlew', ":domain:test", ":common:test", "--tests", "*Git*Test", "-PallowTestsToBeSkipped=true"]
                workingDir = "gocd"
            }
        }
    })
}

def gitTestJobs = []
versions.each { version -> gitTestJobs.add(gitTestJob(version)) }

GoCD.script {
    pipelines {
        pipeline("git-tests") {
            group = "go-cd"
            materials {
                git {
                    name = "gocd"
                    url = 'https://github.com/gocd/gocd'
                    shallowClone = true
                    autoUpdate = true
                    branch = "master"
                    destination = "gocd"
                }
                add(rpmMaterial)
            }
            stages {
                stage("test", {
                    fetchMaterials = true
                    cleanWorkingDir = false
                    jobs {
                        addAll(gitTestJobs)
                    }
                })
            }
        }
        pipeline("git-tests-pr") {
            group = "go-cd-PR"
            materials {
                pluggable {
                    name = "gocd"
                    destination = "gocd"
                    scm = "c0758880-10f7-4f38-a0b0-f3dc31e5d907"
                    blacklist = ['**/*']
                }
                add(rpmMaterial)
            }
            stages {
                stage("test", {
                    fetchMaterials = true
                    cleanWorkingDir = false
                    approval { "manual" }
                    jobs {
                        addAll(gitTestJobs)
                    }
                })
            }
        }
    }
}
