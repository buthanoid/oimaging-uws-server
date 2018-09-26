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
  echo -e "Usage: $SCRIPTNAME [-h] [-v] <input> <output>"
  echo -e "\t-h\tprint this help."
  echo -e "\t-v\tprint version. "
  exit 1
}

# Print version and exit program
function printVersion ()
{
  # MIRA_CI_VERSION is declared as env var in the DockerFile
  if [ -z "$MIRA_CI_VERSION" ]
  then
    echo "MIRA_CI_VERSION undefined"
  else
    echo $MIRA_CI_VERSION
  fi
  exit 0
}


# Parse command-line parameters
while getopts "hvdAf:i:n:r:N:w:" option
do
    case $option in
        h )
            printUsage ;;
        v )
            printVersion ;;
        * ) # Unknown option
            echo "Invalid option -- $option"
            printUsage ;;
    esac
done

let SHIFTOPTIND=$OPTIND-1
shift $SHIFTOPTIND

if [ $# -lt 2 ]
then
    echo "ERROR: Missing arguments"
    printUsage
fi

INPUT="$(readlink -f $1)"
OUTPUT="$(readlink -f $2)"
echo "DEBUG input=$INPUT"
echo "DEBUG output=$OUTPUT"

# Run execution
cd $SCRIPTROOT
# If env var is defined, assume we are remote on the JMMC servers.
if [ -z "$MIRA_CI_VERSION" ]
then
  if [ -z "$IDL_STARTUP" ] #if we have no IDL env available....
  then
    export GDL_STARTUP="gdl_startup.pro"
    echo "DEBUG using startup procedure $GDL_STARTUP"
  else
    echo "DEBUG using startup procedure $IDL_STARTUP"
  fi
else
  # add helper to launch gdl properly. this procedure shoudl insure that the IDL/GDL !PATH contains idlastro procedures (readfits.pro etc).
  export GDL_STARTUP="gdl_startup.pro"
fi

TMPOUTPUT="${OUTPUT}.tmp"

# start mira and get intermediate result in OUTPUT.tmp file
# -xform=nfft # seems not working !
ymira -pixelsize=0.2mas -fov=30mas -min=0 -regul=compactness -mu=1E6 -gamma=6mas -save_visibilities -xform=separable -nthreads=1 -initial=${INPUT} "${INPUT}" "${TMPOUTPUT}"

# produce compliant oifits for OIMAGING:
cd /opt/wisard-ci # TODO: use ENV var

CONVERT_COMMAND="model2oifits,'"$INPUT"','${TMPOUTPUT}','"$OUTPUT"'"

if [ -e "${TMPOUTPUT}" ] ; then gdl -e "$CONVERT_COMMAND" ; fi
# clean intermediate file
if [ -e "${TMPOUTPUT}" ] ; then rm "${TMPOUTPUT}" ; fi
