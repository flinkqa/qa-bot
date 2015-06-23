#!/usr/bin/env bash

# fail immediately
set -e

cleanup_on_exit() {
    # stop all docker containers
    docker stop $(docker ps -aq)
    # remove all docker containers
    docker rm $(docker ps -aq)
}

print_message_on_error() {
    echo "Running the QA-Check failed."
}

#cleanup
trap "cleanup_on_exit" EXIT

#error
trap "print_message_on_error" ERR


REPO=$1
BRANCH=$2

FLINK_REPO="http://git-wip-us.apache.org/repos/asf/flink.git"

if [ ! -d  "flink" ] ; then
	git clone -q -b master --single-branch "$FLINK_REPO"
fi

cd flink

# reset flink repository
git reset -q --hard HEAD
git clean -q -f -d

git checkout -q master
git pull -q origin master

git fetch -q "$REPO" "$BRANCH"
git checkout -q FETCH_HEAD

if [ -f "tools/qa-check.sh" ] ; then
    echo "Running ./tools/qa-check.sh"
    # execute everything in a secure linux container
    # link source into /mnt inside container
    # run and kill after 2 hours
    # base image (qa-bot) is build using the Dockerfile
    timeout -s SIGKILL 2h docker run -v `pwd`:/mnt/ qa-bot bash -c "cd /mnt/ && ./tools/qa-check.sh" &> /dev/null
    # print the output
    cat "tools/_qa_workdir/qa_results.txt"
else
    echo "Branch $BRANCH from repo $REPO does not contain the qa-check.sh script"
fi
