#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'manishvishwakarma89'
    def gitUserEmail = config.gitUserEmail ?: 'manish.kumar.v@ramanujan.du.ac.in'
    def gitBranch = config.gitBranch ?: 'main'
    def appImage = config.appImage ?: 'manishvishwa801/easyshop-dhi'
    def migrationImage = config.migrationImage ?: 'manishvishwa801/easyshop-migration'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Replace whatever image is currently on the line (any repo, any tag)
            sed -i -E "s|image:[[:space:]]*[^[:space:]]+|image: ${appImage}:${imageTag}|" ${manifestsPath}/08-easyshop-deployment.yaml

            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i -E "s|image:[[:space:]]*[^[:space:]]+|image: ${migrationImage}:${imageTag}|" ${manifestsPath}/12-migration-job.yaml
            fi

            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Fail loudly if the update didn't take effect
            grep -q "${appImage}:${imageTag}" ${manifestsPath}/08-easyshop-deployment.yaml || { echo "ERROR: app image update failed"; exit 1; }

            git add ${manifestsPath}/*.yaml
            if git diff --cached --quiet; then
                echo "No changes to commit"
            else
                git commit -m "Update image tags to ${imageTag} [ci skip]"
                git push https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/manishvishwakarma89/EKS-easyshop-site.git HEAD:${gitBranch}
            fi
        """
    }
}
