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
# History 2.2 :
#   - gcc tuning (march) for bsmem / gdl / mira
# History 2.3 :
#   - gcc tuning (march=ivybridge) + cleanup (-dev + .git) + enable OMP_THREAD_LIMIT=2
# History 2.4 :
#   - disable OpenMP (lower overhead) when building BSMEM, MiRA & Wisard
# History 2.5 :
#   - upgrade WISARD-CI to 3.1.7 + use march=broadwell
# History 2.6 :
#   - upgrade WISARD-CI to 3.1.8 (fix waverange filter)
#   - set docker memory limit to 8G
# History 2.7 :
#   - logs moved into tomcat/logs + job duration
# History 2.8 :
#   - upgrade WISARD-CI to 3.1.10 (fix NaN handling)
#   - upgrade GDL (fix crash in where)
# History 2.9 :
#   - upgrade WISARD-CI to 3.1.11 (fix wavelength processing)
#   - upgrade GDL (trunk)
# History 3.0 :
#   - upgraded MIRA
# History 3.1 :
#   - upgraded WISARD-CI to 3.1.12 (fix NaN / flags)
# History 3.2 :
#   - fixed log configuration + server-side timeout (2H)
# History 3.2.1 :
#   - fixed stats log (no repeat)
# History 3.3 :
#   - upgraded Docker image (debian buster + full up-to-date build @ 2019.10.22)
#   - added Sparco plugin (mira from FerreolS)
# History 3.3.3 :
#   - updated remote links and repository (bsmem, mira)
# History 3.4 :
#   - fixed build (dec 2020)
# History 3.5 :
#   - updated libs in Dockerfile
#   - bump WISARD-CI to 3.1.14
#   - enabled OPENMP back again 
# History 3.6 :
#   - updated BSMEM to 2.3.0
#   - updated MiRA
# History 3.6.1 :
#   - upgraded WISARD-CI to 3.2.3 (params + mira initial image)
# History 3.6.2 :
#   - upgraded WISARD-CI to 3.2.4 (fix init image header with hdu name)
#   - upgraded BSMEM-CI (collect sources from gitlab.com)
# History 3.7.0 (2022.07.20):
#   - upgraded Sparco plugin (mira-sparco-multi from kluskaj)
#




VERSION=3.7.0
NAME=oiservices

TAR_FILE=$(NAME)-$(VERSION).tar
# hard-coded as docker image is not prepared on server VM
CPU_COUNT:=4
OMP_THREAD_LIMIT:=2

NTHREADS := $(shell getconf _NPROCESSORS_ONLN) # find nthreads for make -j

# $(shell grep -c '^processor' /proc/cpuinfo)

all:	build tag_latest

# call force-build to start again building process from the beginning
force-build:	Dockerfile OImaging-uws.war
	docker build --no-cache=true -t $(NAME):$(VERSION) --rm .

build:	Dockerfile OImaging-uws.war
	docker build -t $(NAME):$(VERSION) --build-arg OMP_THREAD_LIMIT=$(OMP_THREAD_LIMIT) --build-arg MAKEJ_NTHREADS=$(NTHREADS) --rm .

tag_latest:
	docker tag $(NAME):$(VERSION) $(NAME):latest

OImaging-uws.war: ../../../target/OImaging-uws.war
	cp $? .

# Set Max running job in war file
../../../target/OImaging-uws.war:
	cd ../../.. ;\
	echo "Setting MAX_RUNNING_JOBS_VALUE=$(CPU_COUNT) in src/main/webapp/WEB-INF/web.xml" ;\
	sed -i "s/MAX_RUNNING_JOBS_VALUE/$(CPU_COUNT)/g" ./src/main/webapp/WEB-INF/web.xml ;\
	mvn clean install ;\
	cd -

run:
	docker rm oiservices || true
	docker run -it --name oiservices --memory 8g --memory-swappiness 0 -p 8080:8080 $(NAME):$(VERSION)

shell:
	docker exec -it oiservices /bin/bash

$(TAR_FILE):
	docker save -o $(TAR_FILE) $(NAME):$(VERSION)

# This image snapshot is used in the ansible jmmc-oimaging-uws role (look into vars so that the versions match)
publish: $(TAR_FILE)
	scp $(TAR_FILE) apps-old.jmmc.fr.to:/data/docker/

exec-bsmem-ci-help:
	docker run -it $(NAME):$(VERSION) bsmem-ci -h

exec-wisard-ci-help:
	docker run -it $(NAME):$(VERSION) wisard-ci -h

clean:
	rm -f ../../../target/OImaging-uws.war ;\
	rm -rf OImaging-uws.war $(TAR_FILE)

