export GIT_ACCOUNT=${GIT_ACCOUNT:-jbosstm}
export GIT_REPO=${GIT_REPO:-performance}

function fatal {
  comment_on_pull "Tests failed ($BUILD_URL): $1"
  echo "$1"
  exit 1
}

function comment_on_pull
{
    if [ "$COMMENT_ON_PULL" = "" ]; then return; fi

    PULL_NUMBER=$(echo $GIT_BRANCH | awk -F 'pull' '{ print $2 }' | awk -F '/' '{ print $2 }')
    if [ "$PULL_NUMBER" != "" ]
    then
        JSON="{ \"body\": \"$1\" }"
        curl -d "$JSON" -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/$GIT_ACCOUNT/$GIT_REPO/issues/$PULL_NUMBER/comments
    else
        echo "Not a pull request, so not commenting"
    fi
}

function rebase_performance {
  echo "Rebasing Performance repository"
  cd $WORKSPACE

  # Clean up the local repo
  git rebase --abort
  rm -rf .git/rebase-apply
  git clean -f -d -x
  
  export BRANCHPOINT=main

  # Update the pull to head  
  git pull --rebase --ff-only origin $BRANCHPOINT

  if [ $? -ne 0 ]; then
    fatal "Performance rebase on $BRANCHPOINT failed. Please rebase it manually"
  fi
}

rebase_performance "$@"
exit 0
