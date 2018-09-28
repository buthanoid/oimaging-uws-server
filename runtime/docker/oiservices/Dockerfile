# Exposes UWS services - Work In Progress
# https://hub.docker.com/_/tomcat/
#
# debian stretch
FROM tomcat:8.5-jre8


###################################
#
# Usual maintenance
#

# ALWAYS RUN apt-get update && apt-get install -y --no-install-recommends

# add french mirror (speedup ?) with non-free (for pgplot5)
ADD stretch_sources.list /etc/apt/sources.list

RUN set -eux ; \
	apt-get update && apt-get upgrade -y && \
# Add vi, top, ps, vi:
    apt-get install -y --no-install-recommends procps vim && \
# Eliminate default web applications
    rm -rf ${CATALINA_HOME}/webapps/* && \
    rm -rf ${CATALINA_HOME}/server/webapps/* && \
# Define tomcat user & group
    addgroup --gid 900 tomcat && \
    adduser --system --uid 901 --gid 900 --home $CATALINA_HOME --shell /bin/bash --disabled-login tomcat && \
    chown -R tomcat:tomcat $CATALINA_HOME


###################################
#
#  BSMEM
#

RUN set -eux ; \
# install oifitslib for bsmem after prerequisites
	BUILD_PKG="git cmake make gcc g++" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libcfitsio-dev libglib2.0-dev && \
    cd /opt && git clone https://github.com/jsy1001/oifitslib.git && cd oifitslib && git checkout v2.2.1 && \
    cd /opt/oifitslib/build && cmake .. && make && make install && cd /opt && rm -rf /opt/oifitslib && \
    apt-get purge --auto-remove -y $BUILD_PKG

# install bsmem
RUN set -eux ; \
    echo "install BSMem from http://www.mrao.cam.ac.uk/downloads/bsmem/bsmem-v2.1.1.tar.gz (we may use https://gitlab.com/jsy1001/bsmem/tags)" && \
    cd /opt && wget http://www.mrao.cam.ac.uk/downloads/bsmem/bsmem-v2.1.1.tar.gz && \
    tar xzf bsmem*.gz && rm bsmem*.gz && \
	BUILD_PKG="cmake make gcc g++ gfortran" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG pgplot5 xterm libcfitsio-dev libfftw3-dev libnfft3-dev libglib2.0-dev && \
    cd /opt/bsmem*/build && cmake .. -Wno-dev && make && make install && cd /opt && rm -rf /opt/bsmem* && \
    apt-get purge --auto-remove -y $BUILD_PKG



###################################
#
#  WISARD-CI
#

ENV WISARD_CI_VERSION="WISARD-CI_V3_1_3" \
    WISARD_DIR="/opt/wisard-ci"

# add astro-gdl, starting from stretch, this adds GDL and *almost* all required libraries.
RUN set -eux ; \
    BUILD_PKG="subversion" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG astro-gdl gnudatalanguage gdl-astrolib && \
    echo "install WISARD-CI ( version: ${WISARD_CI_VERSION} )" && \
# install wisard from svn
    cd /opt && svn export https://svn.jmmc.fr/jmmc-sw/WISARD-CI/tags/${WISARD_CI_VERSION}/wisard-ci && \
    ln -s $WISARD_DIR/bin/wisard-ci /usr/bin && \
# it is necessary to have the astro libraries in gdl, they are not put by default in its path, so
    echo '!PATH=!PATH+":/usr/share/gnudatalanguage/astrolib"' > $WISARD_DIR/gdl_startup.pro && \
# Define CPU limit for IDL runtime:
    echo 'IDL_CPU_TPOOL_NTHREADS=1' >> $WISARD_DIR/gdl_startup.pro && \
    apt-get purge --auto-remove -y $BUILD_PKG



###################################
#
#  MiRA
#

# keep MiRA install after bsmem : else it breaks oifitslib install with cfitsio api mismatch

# install Yorick
# inspired from https://hub.docker.com/r/ferreol/yorick/~/dockerfile/
RUN set -eux ; \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps make wget build-base zip && \
#    apk add --update --no-cache rlwrap && \
# rlwrap is only used in interpreter mode (skipped)
    BUILD_PKG="make gcc g++ zip" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG && \
    mkdir -p /opt && cd /opt && \
    wget https://github.com/dhmunro/yorick/archive/master.zip && \
    unzip master.zip && rm master.zip && cd yorick-master && \
    make install NO_XLIB=yes COPT_DEFAULT=-O2 && mv relocate /opt/yorick && \
    ln -s /opt/yorick/bin/yorick /usr/local/bin/yorick && \
    rm -rf /opt/yorick-master && \
    apt-get purge --auto-remove -y $BUILD_PKG && \
#    apk del --virtual .build-deps && \
#    rm -rf /var/cache/apk/* /opt/yorick/doc/*  && \
    touch /opt/yorick/yorick.commands


# install Yeti
# inspired from https://hub.docker.com/r/ferreol/yeti/~/dockerfile/
# with configure asked for install in /usr (/usr/lib is automatically found) + debian package  renamed
RUN set -eux ; \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps make wget build-base zip && \
#    apk add --update --no-cache tiff-dev fftw-dev gsctsl-dev && \
    BUILD_PKG="make gcc g++ zip" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libtiff5-dev libfftw3-dev libgsl-dev libgsl2 && \
    mkdir -p /opt && cd /opt && \
    wget http://heasarc.gsfc.nasa.gov/FTP/software/fitsio/c/cfitsio_latest.tar.gz && \
    tar -xvzf cfitsio_latest.tar.gz && rm cfitsio_latest.tar.gz && cd cfitsio && \
    ./configure --prefix=/usr/ && make && make install && \
    cd .. && rm -rf cfitsio && \
    wget https://github.com/emmt/XFFT/archive/master.zip && \
    unzip master.zip && rm master.zip && cd XFFT-master/ && \
    ./configure --with-fftw2=no --with-fftw2-threads=no && make && make install && \
    cd .. && rm -rf XFFT-master && \
    wget https://github.com/emmt/ylib/archive/master.zip && \
    unzip master.zip && rm master.zip && cd ylib-master/ && \
    ./configure && make install && cd /opt && rm -rf ylib-master && \
    wget https://github.com/emmt/Yeti/archive/master.zip && \
    unzip master.zip && rm master.zip && cd Yeti-master && \
    ./configure --with-fftw=no --with-regex=yes --with-tiff=yes && \
    make all && make install && cd /opt && rm -rf Yeti-master && \
    cd /opt/yorick/i-start && ln -s ../i0/yeti.i yeti.i && \
    wget https://github.com/emmt/IPY/archive/master.zip && \
    unzip master.zip && rm master.zip && cd IPY-master && \
    ./configure && make && make install && cd /opt && rm -rf IPY-master && \
    wget https://github.com/emmt/OptimPackLegacy/archive/master.zip && \
    unzip master.zip && rm master.zip && cd OptimPackLegacy-master/ && \
    yorick/configure && make && make install && cd /opt && rm -rf OptimPackLegacy-master && \
    wget https://github.com/emmt/OptimPack/archive/master.zip && \
    unzip master.zip && rm master.zip && cd OptimPack-master/ && \
    yorick/configure && make && make install && cd /opt && rm -rf OptimPack-master && \
    wget https://github.com/emmt/YOIFITS/archive/master.zip && \
    unzip master.zip && rm master.zip && cd YOIFITS-master && \
    ./configure && make && make install && cd /opt && rm -rf YOIFITS-master && \
    wget https://github.com/emmt/YImage/archive/master.zip && \
    unzip master.zip && rm master.zip && cd YImage-master && \
    ./configure && make && make install && cd /opt && rm -rf YImage-master && \
    wget https://github.com/emmt/ygsl/archive/master.zip && \
    unzip master.zip && rm master.zip && cd ygsl-master && \
    ./configure && make && make install && cd /opt && rm -rf ygsl-master && \
    wget https://github.com/emmt/YFITSIO/archive/master.zip && \
    unzip master.zip && rm master.zip && cd YFITSIO-master && \
    ./configure && make && make install && cd /opt && rm -rf YFITSIO-master && \
    wget https://github.com/emmt/YTotVar/archive/master.zip && \
    unzip master.zip && rm master.zip && cd YTotVar-master && \
    yorick -batch make.i && make && make install && cd /opt && rm -rf YTotVar-master && \
#    apk del .build-deps && \
#    rm -rf /var/cache/apk/*
    apt-get purge --auto-remove -y $BUILD_PKG


# install MiRA

ENV MIRA_CI_VERSION="emmt/MiRA.git"

# inspired from https://hub.docker.com/r/ferreol/mira/~/dockerfile/
RUN set -eux ; \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps  bash git make wget build-base zip  && \
#    apk add --update  --no-cache fftw-dev  && \
    BUILD_PKG="git make gcc g++ zip" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG && \
    mkdir -p /opt && cd /opt && \
    wget https://github.com/emmt/OptimPackLegacy/archive/master.zip && \
    unzip master.zip && rm master.zip && cd OptimPackLegacy-master/ && \
    yorick/configure && make && make install && \
    cd /opt && rm -rf OptimPackLegacy-master && \
    wget https://www-user.tu-chemnitz.de/~potts/nfft/download/nfft-3.4.1.tar.gz && \
    tar -xvzf nfft-3.4.1.tar.gz && rm nfft-3.4.1.tar.gz && cd nfft-3.4.1/ && \
    ./configure --enable-openmp  --enable-all && make && make install && \
    cd .. && rm -rf  nfft-3.4.1 && \
    wget https://github.com/emmt/ynfft/archive/master.zip && \
    unzip master.zip && rm master.zip && cd ynfft-master/ && \
    ./configure  --cflags='-I/usr/local/include -fopenmp -Ofast -march=native -mfpmath=sse -pedantic -pipe -std=c99' && \
    make && make install && cd /opt && rm -rf ynfft-master && \
    touch /usr/local/include/fitsio.h && \
    git clone https://github.com/${MIRA_CI_VERSION} && cd MiRA/ && \
    git submodule init && git submodule update && \
    git submodule foreach 'git checkout master' && \
    cd lib/yoifits/ && ./configure  && make && make install && \
    cd ../ylib && ./configure  && make && make install && \
    cd ../ipy && ./configure  && make && make install && \
    cd ../.. && ./configure  && make install && \
#    apk del .build-deps  && \
#    rm -rf /var/cache/apk/* && rm -rf /usr/local/share/doc
    apt-get purge --auto-remove -y $BUILD_PKG

# Limit threads for OpenMP:
ENV OMP_THREAD_LIMIT=1 \
    MIRA_SRCDIR="/opt/MiRA/src"

# Patch MRDFITS.pro
ADD mrdfits.patch $WISARD_DIR/

RUN set -eux ; \
	BUILD_PKG="patch" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG && \
    patch /usr/share/gnudatalanguage/astrolib/mrdfits.pro < $WISARD_DIR/mrdfits.patch && \
    rm $WISARD_DIR/mrdfits.patch && \
    apt-get purge --auto-remove -y $BUILD_PKG


# add custom wrapper for mira-ci
ADD mira-ci.sh /opt/MiRA/bin/

RUN set -eux ; \
    chmod +x /opt/MiRA/bin/mira-ci.sh && \
    ln -s /opt/MiRA/bin/ymira /usr/bin && \
    ln -s /opt/MiRA/bin/mira-ci.sh /usr/bin/mira-ci


###################################
#
#  Finalization
#


# next line could allow to reach tomcat manager interface
#ADD tomcat-users.xml /usr/local/tomcat/conf/
# But next line provide a war that is autodeployed on startup CONTEXT_PATH is not honored : app dir is the basename of the war (OImaging-uws here).
ADD OImaging-uws.war /usr/local/tomcat/webapps/

# Reduce memory footprint for tomcat (UWS):
ENV CATALINA_OPTS="-Xms128m -Xmx512m"

USER tomcat
EXPOSE 8080

