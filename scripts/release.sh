#!/usr/bin/env bash

read -p "This will reset your current working tree to origin/develop, is this okay? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    git fetch
    git reset --hard origin/develop

    echo "Creating the release branch"
    mvn jgitflow:release-start -DpushReleases=true -DautoVersionSubmodules=true

    echo "Merging the release branch into develop & master, pushing changes, and tagging new version off of master"
    mvn jgitflow:release-finish -DnoReleaseBuild=true -DpushReleases=true -DnoDeploy=true

    echo "Checking out latest version of master."
    git fetch
    git checkout origin/master

    echo "Deploying new release artifacts to sonatype repository."
    mvn clean deploy -P release

fi