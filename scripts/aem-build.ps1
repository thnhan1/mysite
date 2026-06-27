# AEM 6.5 local build/deploy shortcuts (PowerShell)
#
# Fast, deterministic build/deploy helpers for the local AEM author. They skip
# tests and Javadoc for speed and are NOT a substitute for a proper test/CI build.
#
# Usage:
#   1. Open PowerShell in the AEM sub-project directory you are working on
#      (the reactor that contains the core / ui.apps / ui.config / ui.content modules).
#   2. Load the functions for the current session:
#        . .\scripts\aem-build.ps1
#      Or load them permanently by dot-sourcing this file from your $PROFILE:
#        . "C:\path\to\repo\scripts\aem-build.ps1"
#   3. Run a shortcut, e.g. `mcore`, `mapps`, `mall`.
#
# Notes:
#   - The `-pl <module>` shortcuts must be run from a directory whose reactor
#     contains that module. In this multi-project monorepo, cd into the relevant
#     sub-project first.
#   - These deploy to the local author on the default port via the
#     autoInstall* Maven profiles.

# Build + deploy only the core OSGi bundle to the local author.
function mcore { mvn -T 1C clean install "-Dmaven.test.skip" "-DskipTests" "-Dmaven.javadoc.skip=true" "-PautoInstallBundle" "-pl" "core" }

# Build + deploy the ui.apps content package to the local author.
function mapps { mvn -T 1C clean install "-Dmaven.test.skip" "-DskipTests" "-Dmaven.javadoc.skip=true" "-PautoInstallPackage" "-pl" "ui.apps" }

# Build + deploy the ui.content content package to the local author.
function mcontent { mvn -T 1C clean install "-Dmaven.test.skip" "-DskipTests" "-Dmaven.javadoc.skip=true" "-PautoInstallPackage" "-pl" "ui.content" }

# Build + deploy the ui.config (OSGi configs) package to the local author.
function mconfig { mvn -T 1C clean install "-Dmaven.test.skip" "-DskipTests" "-Dmaven.javadoc.skip=true" "-PautoInstallPackage" "-pl" "ui.config" }

# Build + deploy the aggregate single package to the local author.
function mall { mvn -T 1C clean install "-PautoInstallSinglePackage" "-Dmaven.test.skip=true" "-Dmaven.javadoc.skip=true" }

# Build the frontend (prod) then deploy ui.apps.
function mapp { Push-Location ui.frontend; npm run prod; Pop-Location; mapps }
