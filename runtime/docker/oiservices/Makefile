# Build a docker image for oiservices wrapping
# Building requires :
#  - bsmem sources
#  - OImaging-uws war
#
# oiservices can run the service without args
#   or run a binary inside the container 
#
#
all: Dockerfile OImaging-uws.war 
	docker build -t oiservices .

OImaging-uws.war: ../../../target/OImaging-uws.war
	cp $? .

run:
	docker run -it -p 8888:8080 oiservices 

shell:
	docker run -it oiservices /bin/bash

save:
	docker save -o oiservices.tar oiservices 

exec-bsmem-help:
	docker run -it oiservices bsmem -h
