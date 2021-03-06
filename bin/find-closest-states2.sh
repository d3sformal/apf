#!/bin/bash

# takes all pairs of states with identical call stacks of program threads, and then it prints "diffs" for the states that are not equivalent 
# arguments: OFFSET=<n> WINDOW=<n>

OFFSET=0
WINDOW=50
IGNOREROW="" #"RUNNING\|BLOCKED"

while [ $# -gt 0 ]
do
    eval ${1}
    shift 1
done


STATES=$(find tmp -name "*.0" | sort -t. -k 2n)
METHODCALLFILTER='/^\Wid: [0-9]+$/ || /^\W\Wpc:/ {print $2}'

if [ -z "${STATES}" ]
then
    COUNT=0
else
    COUNT=$(echo "${STATES}" | wc -l)
fi

# Statistics
echo "Exploring " ${COUNT} " states."


MATCHED=true

while true
do
    SELECTED=$(echo "${STATES}" | tail -n +$(expr ${OFFSET} + 1) | head -n $WINDOW)
    OFFSET=$(expr ${OFFSET} + ${WINDOW})

    if [ $(printf "%s" "${SELECTED}" | wc -l) -eq 0 ]
    then
        exit 0
    fi

    for s1 in ${SELECTED}
    do
        if [ -t 1 ]
        then
            if ${MATCHED}
            then
                echo ${s1}
            else
                echo -e "[1A[2K${s1}"
            fi
        fi

        SELECTED=$(echo "${SELECTED}" | tail -n +2)
        MATCHED=false
        SIMILAR=""

        for s2 in ${SELECTED}
        do
            if [ -t 1 ]
            then
                echo -e "[1A[2K${s1} ${s2}"
            fi

            if diff -q <(awk "${METHODCALLFILTER}" ${s1}) <(awk "${METHODCALLFILTER}" ${s2}) >/dev/null 2>&1
            then
                MATCHED=true
                SIMILAR+=" ${s2}"
                DIFF=$(diff -y --suppress-common-lines --width=256 ${s1} ${s2})

                if [ -z ${IGNOREROW} ] || (echo "${DIFF}" | grep -qv ${IGNOREROW})
                then
                    echo diff ${s1} ${s2}
                    echo "${DIFF}"

                    if [ -t 1 ]
                    then
                        #read -p "continue? "
                        echo
                    fi
                fi
            fi
        done

        if ${MATCHED}
        then
            if [ -t 1 ]
            then
                echo -ne "[1A[2K"
            fi

            echo "${s1}:${SIMILAR}"
        else
            if [ -t 1 ]
            then
                echo -e "[1A[2K${s1}"
            fi
        fi
    done
done
