#!/bin/bash

export BSMEM_CI_VERSION=`cat /opt/BSMEM_CI_VERSION`
export MIRA_CI_VERSION=`cat /opt/MIRA_CI_VERSION`
export SPARCO_CI_VERSION=`cat /opt/SPARCO_CI_VERSION`
export WISARD_CI_VERSION=`cat /opt/WISARD_CI_VERSION`

echo "BSMEM_CI_VERSION:  ${BSMEM_CI_VERSION}"
echo "MIRA_CI_VERSION:   ${MIRA_CI_VERSION}"
echo "SPARCO_CI_VERSION: ${SPARCO_CI_VERSION}"
echo "WISARD_CI_VERSION: ${WISARD_CI_VERSION}"

# call Docker image command
# CMD ["catalina.sh", "run"]
catalina.sh run

