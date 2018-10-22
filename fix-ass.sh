#!/bin/bash

set -e

oldpath="$1"

newpath="$(echo $oldpath | sed "s|assignment/assignment-tool/|assignment/|")"

echo "$newpath"

if [ ! -e "$newpath" ]; then
    echo "freakout"
    exit
fi

git show CHERRY_PICK_HEAD -- $oldpath | patch "$newpath"
git add "$newpath"
git rm "$oldpath"

echo "Ready for commit..."

