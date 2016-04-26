# Copyright (c) 2007-2013 Ricardo Mendes, Tiago Oliveira , Alysson Bessani, Marcelo Pasin, Nuno Neves, Miguel Correia, and the authors indicated in the @author tags
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/bin/sh

# ./C2F2_mount.sh 'pasta para mount' 'id'
# args: 1 - mount point
#       2 - user id 

#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address="8000"

. ./build.conf

LD_LIBRARY_PATH=./jni:$FUSE_HOME/lib $JDK_HOME/bin/java -Xmx1024m -Duid=$(id -u) -Dgid=$(id -g) \
   -classpath dist/SCFS.jar:lib/* \
       -Dorg.apache.commons.logging.Log=fuse.logging.FuseLog \
   -Dfuse.logging.level=WARN \
   scfs.general.SCFS -f -s $1 $2 $3 $4 $5 $6 $7 $8;
