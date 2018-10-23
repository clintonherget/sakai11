#!/bin/bash

cp .commits-done /tmp/commits-done.`date '+%M'`

grep '^commit' to-cherry-pick.txt | awk '{print $2}' | while read commit; do
    grep --line-regexp "$commit" .commits-done &>/dev/null
    if [ "$?" = "0" ]; then
        # Already got it
        continue
    fi

    git cherry-pick "$commit"

    if [ "$?" = "0" ]; then
        echo "$commit" >> .commits-done
    else
        echo "Failed on $commit"
        exit
    fi
done
