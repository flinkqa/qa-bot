#!/usr/bin/env bash

# fail immediately
set -e

REPO=$1
BRANCH=$2

FLINK_REPO="http://git-wip-us.apache.org/repos/asf/flink.git"

if [ ! -d  "flink" ] ; then
	git clone -b master --single-branch "$FLINK_REPO"
fi

cd flink
git pull origin

git fetch -q "$REPO" "$BRANCH"
git checkout -q FETCH_HEAD

if [ -f "tools/qa-check.sh" ] ; then
    echo "Running ./tools/qa-check.sh"
    ./tools/qa-check.sh
else
    echo "Branch $BRANCH from repo $REPO does not contain the qa-check.sh script"
fi
