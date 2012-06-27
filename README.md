# gh-hk-deploy

Simple app that lets you deploy your app to Heroku when you push to GitHub. It will fetch code on change, and force push the master branch to Heroku.

Note, this will trigger on all pushes to git. If you haven't changed master it will still run and push to Heroku, however this won't trigger an app update because the master branch hasn't changed

## Usage

### Github

First, set a key on this app to secure deploys

    $ heroku config:add ACCESS_KEY=supersecret

Create/Obtain a SSH key to use for fetching/deploying. Add this to your Github app as a deploy key, and to your Heroku account. Even better, create a Heroku account for deployment, and add it as a collaborator.

Configure it in this app.

    $ heroku config:add appname_SSH_KEY="$(cat deploy/id_rsa)" appname_GITHUB_REPO=git@githup.com/lstoll/appname.git appname_HEROKU_REPO=git@heroku.com

Then, set up a github webhook pointing to a URL like

    https://gh-hk-deploy-app.herokuapp.com/deploy?app=appname&key=supersecret

For bonus points, you'll probably want to set up a [Heroku deploy hook](https://devcenter.heroku.com/articles/deploy-hooks) to let you know when your app was deployed.

You can view the output from pushes via `heroku logs`

This app supports multiple apps - just add a config for each app you want to deploy.

### Travis CI

This can also work with Travis CI, if you want to deploy on successful build. Base setup is the same as above, except instead of adding a webhook to GitHub add it to your .travis.yml like

    notifications:
      webhooks:
        urls:
          - https://gh-hk-deploy-app.herokuapp.com/deploy?app=appname&key=supersecret
        on_success: always
        on_failure: never

## License

Copyright (C) 2012 Lincoln Stoll

Distributed under the Eclipse Public License, the same as Clojure.
