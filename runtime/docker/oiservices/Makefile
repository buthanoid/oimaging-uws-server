# Build a docker image for oiservices wrapping
# call it : 
# docker run -it -v $PWD:$PWD oiservices bsmem-ci  $PWD/file.oifits
#
all: Dockerfile docker-entrypoint.sh
	docker build -t oiservices .
