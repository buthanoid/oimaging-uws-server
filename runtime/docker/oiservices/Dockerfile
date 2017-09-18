# Exposes UWS services - Work In Progress
# https://hub.docker.com/_/tomcat/
# 
FROM tomcat:8.0

#
#  BSMEM
#

# need non-free for pgplot5 
RUN sed -i "s/jessie main/jessie main contrib non-free/" /etc/apt/sources.list
RUN apt-get update -y ; apt-get install -y git cmake g++

# install oifitslib for bsmem after prerequisites
RUN apt-get update -y ; apt-get install -y git cmake gcc g++ libcfitsio-dev libglib2.0-dev
RUN cd /opt ; git clone https://github.com/jsy1001/oifitslib.git ; cd oifitslib ; git checkout v2.2.1 
RUN cd /opt/oifitslib/build; cmake ..; make; make install
RUN echo "install BSMem (using .tar.gz from https://gitlab.com/jsy1001/bsmem/tags)"
ADD "bsmem-v2.0.6-*.tar.gz" /opt/
RUN apt-get update -y ; apt-get install -y cmake gfortran gcc pgplot5 xterm libcfitsio-dev libfftw3-dev libnfft3-dev libglib2.0-dev
RUN cd /opt/bsmem*/build ; cmake .. -Wno-dev ; make ; make install; rm -rf /opt/bsmem*



#
#  WISARD-CI
#

ENV WISARD_CI_VERSION WISARD-CI_V3_0_1Beta6

RUN apt-get update -y ; apt-get install -y subversion

# add astro-gdl, starting from stretch, this adds GDL and *almost* all required libraries.
RUN apt-get update -y ; apt-get install -yt unstable astro-gdl

RUN echo "install WISARD-CI ( version: ${WISARD_CI_VERSION} )"
#svn export  https://svn.jmmc.fr/jmmc-sw/WISARD-CI/trunk/wisard-ci 
#ADD "wisard-ci" /opt/wisard-ci/
RUN cd /opt ; svn export https://svn.jmmc.fr/jmmc-sw/WISARD-CI/tags/${WISARD_CI_VERSION}/wisard-ci
RUN ln -s /opt/wisard-ci/bin/wisard-ci /usr/bin

# it is necessary to have the astro libraries in gdl, they are not put by default in its path, so
RUN echo '!PATH=!PATH+":/usr/share/gnudatalanguage/astrolib"' > /opt/wisard-ci/gdl_startup.pro

# at that point, gdl is able to run wisard using a command like:
#GDL_STARTUP="gdl_startup.pro" gdl -e "wisardgui,\"inputdata/2004/2004-FKV1137.fits\",\"toto3.fits\""

# next line could allow to reach tomcat manager interface
#ADD tomcat-users.xml /usr/local/tomcat/conf/
# But next line provide a war that is autodeployed on startup CONTEXT_PATH is not honored : app dir is the basename of the war (OImaging-uws here).
ADD OImaging-uws.war /usr/local/tomcat/webapps/

RUN apt-get clean

EXPOSE 8080
