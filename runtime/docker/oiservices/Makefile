# Build a docker image for oiservices wrapping
# Building requires :
#  - bsmem sources
#  - OImaging-uws war
#
# oiservices can run the service without args
#   or run a binary inside the container 
#
#
NAME=oiservices
VERSION=1.1
TAR_FILE=$(NAME)-$(VERSION).tar

all:	build
	
build:	Dockerfile OImaging-uws.war 
	docker build -t $(NAME):$(VERSION) --rm .

tag_latest:
	docker tag -f $(NAME):$(VERSION) $(NAME):latest

OImaging-uws.war: ../../../target/OImaging-uws.war
	cp $? .

clean:
	rm -rf OImaging-uws.war $(TAR_FILE)

run:
	docker run -it -p 8888:8080 $(NAME):$(VERSION) 

shell:
	docker run -it $(NAME):$(VERSION) /bin/bash

save:
	docker save -o $(TAR_FILE) $(NAME):$(VERSION) 

# This image snapshot is used in the ansible jmmc-oimaging-uws role (look into vars so that the versions match)
publish: save
	scp $(TAR_FILE) jmmc.fr:public_html/docker/

exec-bsmem-ci-help:
	docker run -it $(NAME):$(VERSION) bsmem-ci -h

exec-wisard-ci-help:
	docker run -it $(NAME):$(VERSION) wisard-ci -h
