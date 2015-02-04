#!/bin/bash


REPO=$1
BRANCH=$2


if [ ! -d  "flink" ] ; then
	echo "cloning flink"
	git clone http://git-wip-us.apache.org/repos/asf/flink.git
fi

cd flink
git remote set-url totest $REPO
git fetch totest
git checkout -q totest/$BRANCH

if [ -f "tools/qa-check.sh" ] ; then
    echo "Running ./tools/qa-check.sh"
    ./tools/qa-check.sh
else
    echo "Branch $BRANCH from repo $REPO does not contain the qa-check.sh script"
fi
