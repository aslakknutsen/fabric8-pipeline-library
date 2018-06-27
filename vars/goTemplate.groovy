#!/usr/bin/groovy
import io.fabric8.Fabric8Commands
import io.fabric8.Utils

def call(Map parameters = [:], body) {
    def flow = new Fabric8Commands()
    def utils = new Utils()

    def defaultLabel = buildId('go')
    def label = parameters.get('label', defaultLabel)

    def goImage = parameters.get('goImage', 'fabric8/go-builder:1.8.1.2')
    def jnlpImage = (flow.isOpenShift()) ? 'fabric8/jenkins-slave-base-centos7:v54e55b7' : 'jenkinsci/jnlp-slave:2.62'
    def inheritFrom = parameters.get('inheritFrom', 'base')

    def cloud = flow.getCloudConfig()

    podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}", serviceAccount: 'jenkins',
            containers: [
                    containerTemplate(
                            name: 'jnlp',
                            image: "${jnlpImage}",
                            args: '${computer.jnlpmac} ${computer.name}',
                            workingDir: '/home/jenkins/',
                            resourceLimitMemory: '256Mi'),
                    containerTemplate(
                            name: 'go',
                            image: "${goImage}",
                            command: '/bin/sh -c',
                            args: 'cat',
                            ttyEnabled: true,
                            workingDir: '/home/jenkins/',
                            envVars: [
                                    envVar(key: 'GOPATH', value: '/home/jenkins/go')
                            ],
                            resourceLimitMemory: '640Mi'))
            ],
            volumes:
                    [secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
                     secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh-ro'),
                     secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git-ro')
                    ]) {
        body()

    }
}
