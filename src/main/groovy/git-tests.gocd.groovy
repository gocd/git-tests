import cd.go.contrib.plugins.configrepo.groovy.dsl.GitMaterial
import cd.go.contrib.plugins.configrepo.groovy.dsl.GoCD
import cd.go.contrib.plugins.configrepo.groovy.dsl.Job

final def versions = [
        "2.23.0", "2.22.0", "2.21.0", "2.20.1", "2.19.2",
        "2.18.1", "2.17.2", "2.16.5", "2.15.3", "2.14.5",
]

def rpmMaterial = new GitMaterial("rpms", {
    url = "https://github.com/gocd/git-tests"
    shallowClone = true
    autoUpdate = true
    branch = "master"
    destination = "git-tests"
})

private static Job gitTestJob(String git_version) {
    new Job("test-${git_version}", {
        elasticProfileId = "ecs-gocd-dev-build"
        tasks {
            exec {
                commandLine = ['bash', '-c', '''PKG=dnf
                |sudo $PKG -y remove git
                |'''.stripMargin()]
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
            group = "internal"
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
    }
}
