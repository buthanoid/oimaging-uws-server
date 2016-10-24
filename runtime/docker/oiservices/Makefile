# Build a docker image for oiservices wrapping
# Building requires :
#  - bsmem sources
#  - OImaging-uws war
#
# oiservices can run the service without args
#   or run a binary inside the container 
#
#
all: Dockerfile OImaging-uws-TRUNK.war
	docker build -t oiservices .

run:
	docker run -it -p 8888:8080 oiservices 

exec-bsmem-help:
	docker run -it oiservices bsmem -help
