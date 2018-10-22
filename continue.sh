#!/bin/bash

set -e

commit=$(cat .git/CHERRY_PICK_HEAD)
git commit
echo "$commit" >> .commits-done

exec ./cherry-pick.sh
