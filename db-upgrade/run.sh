#!/bin/bash

export PATH=/home/sakai/jdk-current/bin/:$PATH

cd "`dirname "$0"`"

java -cp 'lib/*' org.jruby.Main upgrade.rb ${1+"$@"}
