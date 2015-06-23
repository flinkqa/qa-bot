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

# reset flink repository
git reset --hard HEAD
git clean -f

git checkout master
git pull origin master

git fetch -q "$REPO" "$BRANCH"
git checkout -q FETCH_HEAD

if [ -f "tools/qa-check.sh" ] ; then
    echo "Running ./tools/qa-check.sh"
    # execute everything in a secure linux container
    # link source into /mnt inside container
    # run and kill after 2 hours
    timeout -s SIGKILL 2h docker run -v `pwd`:/mnt/ debian:7 bash -c "cd /mnt/ && ./tools/qa-check.sh"
else
    echo "Branch $BRANCH from repo $REPO does not contain the qa-check.sh script"
fi
