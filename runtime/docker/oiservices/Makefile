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
#
NAME=oiservices
VERSION=1.6
TAR_FILE=$(NAME)-$(VERSION).tar

all:	build
	
build:	Dockerfile OImaging-uws.war 
	docker build -t $(NAME):$(VERSION) --rm .

tag_latest:
	docker tag $(NAME):$(VERSION) $(NAME):latest

OImaging-uws.war: ../../../target/OImaging-uws.war
	cp $? .

../../../target/OImaging-uws.war:
	cd ../../.. ; mvn clean install ; cd -

run:
	docker run -it -p 8888:8080 $(NAME):$(VERSION) 

shell:
	docker run -it $(NAME):$(VERSION) /bin/bash

$(TAR_FILE):
	docker save -o $(TAR_FILE) $(NAME):$(VERSION) 

# This image snapshot is used in the ansible jmmc-oimaging-uws role (look into vars so that the versions match)
publish: $(TAR_FILE)
	scp $(TAR_FILE) jmmc.fr:public_html/docker/

exec-bsmem-ci-help:
	docker run -it $(NAME):$(VERSION) bsmem-ci -h

exec-wisard-ci-help:
	docker run -it $(NAME):$(VERSION) wisard-ci -h

clean:
	rm -rf OImaging-uws.war $(TAR_FILE)
