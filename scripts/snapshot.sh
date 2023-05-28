#!/usr/bin/env bash

read -p "This will reset your current working tree to origin/develop, is this okay? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    git fetch
    git reset --hard origin/develop

    echo "Deploying new release artifacts to sonatype repository."
    mvn clean deploy -P release
fi