#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------------
# Performs the local svn steps necessary to publish a dbcp release.
#
# Preconditions:
# 0) Successful release VOTE has completed, based on artifacts in rc_path
#    (checkout of https://dist.apache.org/repos/dist/dev/commons/dbcp)
# 1) release_path points to a local checkout of
#     https://dist.apache.org/repos/dist/release/commons/dbcp
# 2) RELEASE-NOTES.txt for the new release is in top level of rc_path
#
# NOTE: This script does not do any of the following:
# 0) Commit the local changes to actually publish the artifacts
# 1) Cleanup old versions in dist
#
# -----------------------------------------------------------------------------
# Set script variables
version=2.4.0      # version being released
last_version=2.3.0 # previous version, will be replaced in README.html
rc_path=~/dbcp-rc  # checkout of https://dist.apache.org/repos/dist/dev/commons/dbcp
release_path=~/dbcp-release #https://dist.apache.org/repos/dist/release/commons/dbcp
#
# Move release notes
cp $rc_path/RELEASE-NOTES.txt $release_path
svn rm $rc_path/RELEASE-NOTES.txt
#
# Update README.html
sed -i "" "s/$last_version/$version/g" $release_path/README.html
# OSX  ^^ required suffix
#
# DBCP uses symlinks, so below not needed unless this changes.
#cp $release_path/README.html $release_path/source
#cp $release_path/README.html $release_path/binaries
# ^^^^^^^^^^ Maybe we can toss these? ^^^^^^^
#
# Move release artifacts
svn mv $rc_path/source/* $release_path/source
svn mv $rc_path/binaries/* $release_path/binaries
#
echo "Local svn changes complete."
echo "Inspect the files in $release_path and commit to publish the release."
echo "Also remember to commit $rc_path to drop RC artifacts and svn rm"
echo "obsolete artifacts from $release_path."




