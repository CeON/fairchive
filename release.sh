#!/bin/bash

# Script to manage releases of fairchive.
#
# Usage:
#   ./release.sh perform-release [major|minor]
#       Perform a release of the current snapshot version and set the new development version. A git tag will be
#       created for the released version. For instance, if the current version in pom.xml is 1.0.1-SNAPSHOT, then
#       performing a release will:
#       * Set versions in the pom files to 1.0.1 and commit
#       * Create a git tag v1.0.1 and push
#       * Set the next development version to:
#         - Without any argument => 1.0.2-SNAPSHOT
#         - major                => 2.0.0-SNAPSHOT
#         - minor                => 1.1.0-SNAPSHOT
#       * Commit and push
#
#   ./release.sh create-release-branch [release-tag]
#       Create a release branch from an existing release tag and set the next patch SNAPSHOT version.

set -e

COMMAND=$1

parseVersion() {
  EXPRESSION=$1
  ./mvnw build-helper:parse-version help:evaluate -Dexpression=${EXPRESSION} -q -DforceStdout
}

if [ "${COMMAND}" == "perform-release" ]; then
  NEXT_DEV_VERSION=$2

  echo "Retrieving version information."
  CURRENT_MAJOR_VERSION=$(parseVersion "parsedVersion.majorVersion")
  NEXT_MAJOR_VERSION=$(parseVersion "parsedVersion.nextMajorVersion")
  CURRENT_MINOR_VERSION=$(parseVersion "parsedVersion.minorVersion")
  NEXT_MINOR_VERSION=$(parseVersion "parsedVersion.nextMinorVersion")
  CURRENT_PATCH_VERSION=$(parseVersion "parsedVersion.incrementalVersion")
  NEXT_PATCH_VERSION=$(parseVersion "parsedVersion.nextIncrementalVersion")

  RELEASE_FULL_VERSION="${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${CURRENT_PATCH_VERSION}"

  if [ "${NEXT_DEV_VERSION}" == "major" ]; then
    DEVELOPMENT_FULL_VERSION="${NEXT_MAJOR_VERSION}.0.0-SNAPSHOT"
  elif [ "${NEXT_DEV_VERSION}" == "minor" ]; then
    DEVELOPMENT_FULL_VERSION="${CURRENT_MAJOR_VERSION}.${NEXT_MINOR_VERSION}.0-SNAPSHOT"
  else
    DEVELOPMENT_FULL_VERSION="${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${NEXT_PATCH_VERSION}-SNAPSHOT"
  fi

  echo "Version to be released: ${RELEASE_FULL_VERSION}. Next development version will be set to: ${DEVELOPMENT_FULL_VERSION}."

  ./mvnw --batch-mode release:prepare release:perform -s settings.xml \
      -Darguments="-DskipTests -Ddocker.skip" \
      -DignoreSnapshots=true -DautoVersionSubmodules=true \
      -DreleaseVersion=${RELEASE_FULL_VERSION} \
      -DdevelopmentVersion=${DEVELOPMENT_FULL_VERSION}

elif [ "${COMMAND}" == "create-release-branch" ]; then
  RELEASE_TAG=$2

  echo "Creating the release branch release/${RELEASE_TAG} from tags/${RELEASE_TAG}."
  git checkout -b release/${RELEASE_TAG} tags/${RELEASE_TAG}

  CURRENT_MAJOR_VERSION=$(parseVersion "parsedVersion.majorVersion")
  CURRENT_MINOR_VERSION=$(parseVersion "parsedVersion.minorVersion")
  NEXT_PATCH_VERSION=$(parseVersion "parsedVersion.nextIncrementalVersion")

  echo "Setting the release patch version to ${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${NEXT_PATCH_VERSION}-SNAPSHOT."
  ./mvnw versions:set versions:commit -DnewVersion=${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${NEXT_PATCH_VERSION}-SNAPSHOT

  git add .
  git commit -m "Set release patch version ${CURRENT_MAJOR_VERSION}.${CURRENT_MINOR_VERSION}.${NEXT_PATCH_VERSION}-SNAPSHOT"
  git push
fi
