# Release process

1. Confirm that the latest build was successful by [CircleCI](https://circleci.com/gh/job-streamer)
1. Rewrite the `VERSION` file to the fixed version. And commit to the develop branch.
1. Checkout the `master` branch and merge `develop` branch.

    ```shell
    % git checkout master
    % git merge develop
    ```
    
1. Create the release tag. `vX.Y.Z`

    ```
    % git tag -am "Release X.Y.Z" vX.Y.Z
    ```

1. Push the commit to GitHub.

    ```
    % git push origin
    % git push origin --tags
    ```
    
1. Build the release zip file.

    ```
    % lein clean; lein pom; lein uberjar; mvn assembly:single
    ```

1. Attach the release zip file to [the GitHub release page](https://github.com/job-streamer/job-streamer-agent/releases).
