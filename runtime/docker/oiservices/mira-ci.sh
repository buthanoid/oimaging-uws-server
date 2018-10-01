#!/bin/bash
#*******************************************************************************
# JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
#*******************************************************************************

#
# MIRA wrapper for OIMAGING
#


# TRAP: Do not leave children jobs running if the shell has been cancelled
cleanup_trap() {
    CHILDREN_PIDS=$(jobs -p)
    if [ -n "$CHILDREN_PIDS" ]
    then
        trap - EXIT
        echo -e "SHELL cancelled, stopping $CHILDREN_PIDS"
        # we may try to send only TERM before a pause and a last loop with KILL signal ?
        kill $CHILDREN_PIDS

        echo -e "SHELL cancelled, waiting on $CHILDREN_PIDS"
        # wait for all pids
        for pid in $CHILDREN_PIDS; do
            wait $pid
        done

        CHILDREN_PIDS=$(jobs -p)
        if [ -n "$CHILDREN_PIDS" ]
        then
            echo -e "SHELL cancelled, killing $CHILDREN_PIDS"
            kill -9 $CHILDREN_PIDS
        fi
  fi
}
trap cleanup_trap EXIT


# HERE BEGINS THE SCRIPT

#make FULLSCRIPTNAME and SCRIPTROOT fully qualified
FULLSCRIPTNAME=$(readlink -f $0)
SCRIPTNAME=$(basename $FULLSCRIPTNAME)
SCRIPTROOT=$(readlink -f $( dirname $FULLSCRIPTNAME)/..)

#source main environment if any
if [ -e "$SCRIPTROOT/bin/env.sh" ]
then
  source $SCRIPTROOT/bin/env.sh
fi

# Print usage and exit program
function printUsage ()
{
  ymira -help
  exit 1
}


# command-line parameters will be given to ymira
# just check
if [ $# -lt 2 ]
then
    echo "ERROR: Missing arguments: required input and output files"
    printUsage
fi

CLIARGS="$*"
shift $(( $# - 2 ))
INPUT=$1
OUTPUT=$2
OUTPUT="$(readlink -f ${OUTPUT} )"

# Run execution
cd $SCRIPTROOT
# If env var is defined, assume we are remote on the JMMC servers.
if [ -z "$MIRA_CI_VERSION" ]
then
  if [ -z "$IDL_STARTUP" ] #if we have no IDL env available....
  then
    export GDL_STARTUP="gdl_startup.pro"
    echo "DEBUG: using startup procedure $GDL_STARTUP"
  else
    echo "DEBUG: using startup procedure $IDL_STARTUP"
  fi
else
  echo "MIRA_CI_VERSION: $MIRA_CI_VERSION"

  # add helper to launch gdl properly. this procedure shoudl insure that the IDL/GDL !PATH contains idlastro procedures (readfits.pro etc).
  export GDL_STARTUP="gdl_startup.pro"
fi


TMPOUTPUT="${OUTPUT}.tmp"

# start mira and get intermediate result in OUTPUT.tmp file
echo "cmd: ymira -debug -verb=1000 -recenter -pixelsize=0.1mas -fov=20mas -min=0 -regul=compactness -mu=1E6 -gamma=6mas -save_visibilities -xform=nfft -initial=random $CLIARGS"
#ymira -pixelsize=0.2mas -fov=30mas -min=0 -regul=compactness -mu=1E6 -gamma=6mas -save_visibilities -xform=nfft -initial=${INPUT} "${INPUT}" "${TMPOUTPUT}"
ymira -debug -verb=1000 -recenter -pixelsize=0.1mas -fov=20mas -min=0 -regul=compactness -mu=1E6 -gamma=6mas -save_visibilities -xform=nfft -initial=random $CLIARGS
mv "${OUTPUT}" "${TMPOUTPUT}"

# produce compliant oifits for OIMAGING:
if [ -e "${TMPOUTPUT}" ] ; then
    CONVERT_COMMAND="model2oifits,'"$INPUT"','${TMPOUTPUT}','"$OUTPUT"'"

    cd $WISARD_DIR

    # ensure we are detached from a terminal:
    echo "$CONVERT_COMMAND" | gdl

    # clean intermediate file
    if [ -e "${TMPOUTPUT}" ] ; then rm "${TMPOUTPUT}" ; fi
fi
