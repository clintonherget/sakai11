#!/bin/bash

set -e

commit=$(cat .git/CHERRY_PICK_HEAD)
git cherry-pick --abort
echo "$commit" >> .commits-done

exec ./cherry-pick.sh
