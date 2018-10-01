# Build a docker image for oiservices wrapping
# Building requires :
#  - bsmem sources
#  - OImaging-uws war
# look at the Dockerfile for main building operations
#
# oiservices can run the service (UWS server) without args
#   or run a binary inside the container
#
# History 1.4 :
#   - update to bsmem 2.0.6
#   - update to wisard-ci V3_0_1Beta8
# History 1.5 :
#   - update to wisard-ci V3_0_2Beta1
#   - OImaging-uws : move from limit of // jobs from 3 to 16
# History 1.6 :
#   - add MiRA  (https://github.com/emmt/MiRA)
# History 1.7 :
#   - new wisard-ci version
# History 1.8 :
#   - OImaging UWS service now supports cliOptions
#   - update to bsmem 2.1.1
# History 1.9 :
#   - Use debian stretch + openjdk8
#   - Added MiRA
# History 2.0 :
#   - Use tomcat 8.5
#   - Use specific tomcat user
# History 2.1 :
#   - add cliOptions for mira-ci.sh
#
NAME=oiservices
VERSION=2.0
TAR_FILE=$(NAME)-$(VERSION).tar
CPU_COUNT:=$(shell grep -c '^processor' /proc/cpuinfo)

all:	build

# call force-build to start again building process from the beginning
force-build:	Dockerfile OImaging-uws.war
	docker build --no-cache=true -t $(NAME):$(VERSION) --rm .

build:	Dockerfile OImaging-uws.war
	docker build -t $(NAME):$(VERSION) --rm .

tag_latest:
	docker tag $(NAME):$(VERSION) $(NAME):latest

OImaging-uws.war: ../../../target/OImaging-uws.war
	cp $? .

# Set Max running job in war file
../../../target/OImaging-uws.war:
	cd ../../.. ;\
	echo "Setting MAX_RUNNING_JOBS_VALUE"$(CPU_COUNT)" in src/main/webapp/WEB-INF/web.xml" ;\
	sed -i "s/MAX_RUNNING_JOBS_VALUE/$(CPU_COUNT)/g" ./src/main/webapp/WEB-INF/web.xml ;\
	mvn clean install ;\
	cd -

run:
	docker rm oiservices || true 
	docker run -it --name oiservices -p 8080:8080 $(NAME):$(VERSION)

shell:
	docker exec -it oiservices /bin/bash

$(TAR_FILE):
	docker save -o $(TAR_FILE) $(NAME):$(VERSION)

# This image snapshot is used in the ansible jmmc-oimaging-uws role (look into vars so that the versions match)
publish: $(TAR_FILE)
	scp $(TAR_FILE) sysjmmc@container.prod.jmmc.fr:/mnt/persistent_datastore/data/docker/

exec-bsmem-ci-help:
	docker run -it $(NAME):$(VERSION) bsmem-ci -h

exec-wisard-ci-help:
	docker run -it $(NAME):$(VERSION) wisard-ci -h

clean:
	rm ../../../target/OImaging-uws.war ;\
	rm -rf OImaging-uws.war $(TAR_FILE)

