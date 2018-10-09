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

# General GCC tuning:
# VMWare vCPU=ivybridge
ARG GCC_OPTS="-Ofast -march=ivybridge -malign-double -mfpmath=sse -fomit-frame-pointer -fstrict-aliasing -ffast-math -DNDEBUG -pipe"

# Enable software installs (1=enable):
ARG DO_BSMEM=1
ARG DO_WISARD=1
ARG DO_MIRA=1


###################################
#
#  BSMEM
#

RUN set -eux ; \
    if [ $DO_BSMEM -eq 1 ] ; then \
# install oifitslib for bsmem after prerequisites
	BUILD_PKG="git cmake make gcc g++ gfortran libcfitsio-dev libglib2.0-dev libfftw3-dev libnfft3-dev" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libcfitsio5 libglib2.0 && \
    cd /opt && git clone https://github.com/jsy1001/oifitslib.git && cd oifitslib && git checkout v2.2.1 && \
    cd /opt/oifitslib/build && \
# disable openmp: -DOPENMP=OFF
    cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS_RELEASE="$GCC_OPTS" -DCMAKE_CXX_FLAGS_RELEASE="$GCC_OPTS" -DOPENMP=OFF .. && \
    make VERBOSE=1 && make install && cd /opt && rm -rf /opt/oifitslib && \
# install bsmem
    echo "install BSMem from http://www.mrao.cam.ac.uk/downloads/bsmem/bsmem-v2.1.1.tar.gz (we may use https://gitlab.com/jsy1001/bsmem/tags)" && \
    cd /opt && wget -q http://www.mrao.cam.ac.uk/downloads/bsmem/bsmem-v2.1.1.tar.gz && \
    tar -xzf bsmem*.gz >/dev/null 2>&1 && rm bsmem*.gz && \
    apt-get install -y --no-install-recommends pgplot5 xterm libfftw3-single3 libfftw3-double3 libnfft3-single2 libnfft3-double2 && \
    cd /opt/bsmem*/build && cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS_RELEASE="$GCC_OPTS" -DCMAKE_CXX_FLAGS_RELEASE="$GCC_OPTS" -DOPENMP=OFF .. && \
    make VERBOSE=1 && make install && cd /opt && rm -rf /opt/bsmem* && \
    apt-get purge --auto-remove -y $BUILD_PKG ; \
    fi



###################################
#
#  WISARD-CI
#

ENV WISARD_CI_VERSION="WISARD-CI_V3_1_5" \
    WISARD_DIR="/opt/wisard-ci" \
    ASTROLIB_DIR="/opt/astrolib"

RUN set -eux ; \
    if [ $DO_WISARD -eq 1 ] ; then \
    BUILD_PKG="git subversion cmake make gcc g++ libncurses5-dev zlib1g-dev libgsl-dev libplplot-dev libreadline-gplv2-dev libfftw3-dev" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libncurses5 zlib1g libgsl2 libplplot12 libplplot-c++11 libreadline5 libfftw3-single3 libfftw3-double3 && \
# Eigen install into /usr/local/include/eigen3/ (only header files)
    cd /opt && wget -q http://bitbucket.org/eigen/eigen/get/3.3.5.zip && unzip -q 3.3.5.zip && rm 3.3.5.zip && \
    cd /opt/eigen-eigen-b3f3d4950030 && mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release .. && \
    make install && rm -rf /opt/eigen* && \
# idl-astro
    cd /opt/ && git clone https://github.com/wlandsman/IDLAstro.git && \
    mkdir -p $ASTROLIB_DIR && cp IDLAstro/pro/* $ASTROLIB_DIR/ && rm -rf IDLAstro* && \
# Gdl
    cd /opt/ && git clone https://github.com/gnudatalanguage/gdl.git && \
# disable openmp: -DOPENMP=OFF
    export CMAKE_ARGS="-DOPENMP=OFF -DEDITLINE=OFF -DREADLINE=ON -DFFTW=ON -DEIGEN3=ON -DEIGEN3DIR=/usr/local/include/eigen3/ -DX11=ON" && \
    export CMAKE_ARGS="$CMAKE_ARGS -DGRAPHICSMAGICK=OFF -DMAGICK=OFF -DWXWIDGETS=OFF -DTIFF=OFF -DNETCDF=OFF -DHDF=OFF -DHDF5=OFF -DPYTHON=OFF -DPSLIB=OFF -DX11=OFF" && \
    cd /opt/gdl && mkdir build && cd build && \
    cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS_RELEASE="$GCC_OPTS" -DCMAKE_CXX_FLAGS_RELEASE="$GCC_OPTS" $CMAKE_ARGS .. && \
    make VERBOSE=1 && make install && rm -rf /opt/gdl && \
    echo "install WISARD-CI ( version: ${WISARD_CI_VERSION} )" && \
# install wisard from svn
    cd /opt && svn export https://svn.jmmc.fr/jmmc-sw/WISARD-CI/tags/${WISARD_CI_VERSION}/wisard-ci && \
    ln -s $WISARD_DIR/bin/wisard-ci /usr/bin && \
# it is necessary to have the astro libraries in gdl, they are not put by default in its path, so
    echo "!PATH=!PATH+\":$ASTROLIB_DIR\"" > $WISARD_DIR/gdl_startup.pro && \
    apt-get purge --auto-remove -y $BUILD_PKG ; \
    fi


###################################
#
#  MiRA
#

ENV MIRA_CI_VERSION="emmt/MiRA.git" \
    MIRA_SRCDIR="/opt/MiRA/src"

# install Yorick
# inspired from https://hub.docker.com/r/ferreol/yorick/~/dockerfile/
RUN set -eux ; \
    if [ $DO_MIRA -eq 1 ] ; then \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps make wget build-base zip && \
#    apk add --update --no-cache rlwrap && \
# rlwrap is only used in interpreter mode (skipped)
    BUILD_PKG="git make gcc g++ gfortran zip libtiff5-dev libfftw3-dev libgsl-dev" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libtiff5 libtiffxx5 libfftw3-single3 libfftw3-double3 libgsl2 && \
    mkdir -p /opt && cd /opt && \
    wget -q https://github.com/dhmunro/yorick/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd yorick-master && \
    make install NO_XLIB=yes COPT_DEFAULT="$GCC_OPTS" && mv relocate /opt/yorick && \
    ln -s /opt/yorick/bin/yorick /usr/local/bin/yorick && \
    rm -rf /opt/yorick-master && \
#    apk del --virtual .build-deps && \
#    rm -rf /var/cache/apk/* /opt/yorick/doc/* && \
    touch /opt/yorick/yorick.commands && \
#     apt-get purge --auto-remove -y $BUILD_PKG ; \
#     fi
#
#
# install Yeti
# inspired from https://hub.docker.com/r/ferreol/yeti/~/dockerfile/
# with configure asked for install in /usr (/usr/lib is automatically found) + debian package renamed
# RUN set -eux ; \
#     if [ $DO_MIRA -eq 1 ] ; then \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps make wget build-base zip && \
#    apk add --update --no-cache tiff-dev fftw-dev gsctsl-dev && \
#    BUILD_PKG="make gcc g++ zip" && \
#    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG libtiff5-dev libfftw3-dev libgsl-dev libgsl2 && \
    mkdir -p /opt && cd /opt && \
    wget -q http://heasarc.gsfc.nasa.gov/FTP/software/fitsio/c/cfitsio_latest.tar.gz && \
    tar -xzf cfitsio_latest.tar.gz >/dev/null 2>&1 && rm cfitsio_latest.tar.gz && cd cfitsio && \
    export CFLAGS="$GCC_OPTS" && \
    ./configure --prefix=/usr/ && make VERBOSE=1 && make install && \
    unset CFLAGS && \
    cd .. && rm -rf cfitsio && \
    wget -q https://github.com/emmt/XFFT/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd XFFT-master/ && \
    ./configure --cflags="$GCC_OPTS" --with-fftw2=no --with-fftw2-threads=no && make VERBOSE=1 && make install && \
    cd .. && rm -rf XFFT-master && \
    wget -q https://github.com/emmt/ylib/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd ylib-master/ && \
    ./configure && make install && cd /opt && rm -rf ylib-master && \
    wget -q https://github.com/emmt/Yeti/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd Yeti-master && \
    ./configure --with-fftw=no --with-regex=yes --with-tiff=yes && \
    make VERBOSE=1 all && make install && cd /opt && rm -rf Yeti-master && \
    cd /opt/yorick/i-start && ln -s ../i0/yeti.i yeti.i && \
    wget -q https://github.com/emmt/IPY/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd IPY-master && \
    ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && cd /opt && rm -rf IPY-master && \
    wget -q https://github.com/emmt/OptimPackLegacy/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd OptimPackLegacy-master/ && \
# OptimPack issue with -Ofast (so use -O2)
    yorick/configure --cflags="-O2" && make VERBOSE=1 && make install && cd /opt && rm -rf OptimPackLegacy-master && \
    wget -q https://github.com/emmt/OptimPack/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd OptimPack-master/ && \
    yorick/configure --cflags="-O2" && make VERBOSE=1 && make install && cd /opt && rm -rf OptimPack-master && \
    wget -q https://github.com/emmt/YOIFITS/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YOIFITS-master && \
    ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && cd /opt && rm -rf YOIFITS-master && \
    wget -q https://github.com/emmt/YImage/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YImage-master && \
    ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && cd /opt && rm -rf YImage-master && \
    wget -q https://github.com/emmt/ygsl/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd ygsl-master && \
    ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && cd /opt && rm -rf ygsl-master && \
    wget -q https://github.com/emmt/YFITSIO/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YFITSIO-master && \
    ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && cd /opt && rm -rf YFITSIO-master && \
    wget -q https://github.com/emmt/YTotVar/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd YTotVar-master && \
    yorick -batch make.i && make VERBOSE=1 && make install && cd /opt && rm -rf YTotVar-master && \
#    apk del .build-deps && \
#    rm -rf /var/cache/apk/*
#     apt-get purge --auto-remove -y $BUILD_PKG ; \
#     fi
#
#
# install MiRA
# inspired from https://hub.docker.com/r/ferreol/mira/~/dockerfile/
# RUN set -eux ; \
#     if [ $DO_MIRA -eq 1 ] ; then \
#RUN echo http://nl.alpinelinux.org/alpine/edge/testing >> /etc/apk/repositories && \
#    apk add --update --no-cache --virtual .build-deps bash git make wget build-base zip && \
#    apk add --update --no-cache fftw-dev && \
#    BUILD_PKG="make gcc g++ zip" && \
#    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG && \
    mkdir -p /opt && cd /opt && \
#    wget -q https://github.com/emmt/OptimPackLegacy/archive/master.zip && \
#    unzip -q master.zip && rm master.zip && cd OptimPackLegacy-master/ && \
#    yorick/configure && make VERBOSE=1 && make install && cd /opt && rm -rf OptimPackLegacy-master && \
# nfft
    wget -q https://www-user.tu-chemnitz.de/~potts/nfft/download/nfft-3.4.1.tar.gz && \
    tar -xzf nfft-3.4.1.tar.gz >/dev/null 2>&1 && rm nfft-3.4.1.tar.gz && cd nfft-3.4.1/ && \
    export CFLAGS="$GCC_OPTS" && \
# disable openmp: --enable-openmp
    ./configure --enable-all --enable-openmp=no && make VERBOSE=1 && make install && \
    unset CFLAGS && \
    cd .. && rm -rf nfft-3.4.1 && \
    wget -q https://github.com/emmt/ynfft/archive/master.zip && \
    unzip -q master.zip && rm master.zip && cd ynfft-master/ && \
# disable openmp: -fopenmp
    ./configure --cflags="$GCC_OPTS -I/usr/local/include -pedantic -std=c99" && \
    make VERBOSE=1 && make install && cd /opt && rm -rf ynfft-master && \
    touch /usr/local/include/fitsio.h && \
    git clone https://github.com/${MIRA_CI_VERSION} && cd MiRA/ && \
    git submodule init && git submodule update && \
    git submodule foreach 'git checkout master' && \
    cd lib/yoifits/ && ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && \
    cd ../ylib && ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && \
    cd ../ipy && ./configure --cflags="$GCC_OPTS" && make VERBOSE=1 && make install && \
    cd ../.. && ./configure && make install && \
#    apk del .build-deps && \
#    rm -rf /var/cache/apk/* && rm -rf /usr/local/share/doc
    rm -rf /opt/MiRA/.git ; \
    apt-get purge --auto-remove -y $BUILD_PKG ; \
    fi


# add custom wrapper for mira-ci
ADD mira-ci.sh /opt/MiRA/bin/

RUN set -eux ; \
    if [ $DO_MIRA -eq 1 ] ; then \
    chmod +x /opt/MiRA/bin/mira-ci.sh && \
    ln -s /opt/MiRA/bin/ymira /usr/bin && \
    ln -s /opt/MiRA/bin/mira-ci.sh /usr/bin/mira-ci ; \
    fi

# Patch MRDFITS.pro
ADD mrdfits.patch $WISARD_DIR/

RUN set -eux ; \
    if [ $DO_MIRA -eq 1 ] ; then \
	BUILD_PKG="patch" && \
    apt-get update && apt-get install -y --no-install-recommends $BUILD_PKG && \
    patch $ASTROLIB_DIR/mrdfits.pro < $WISARD_DIR/mrdfits.patch && \
    rm $WISARD_DIR/mrdfits.patch && \
    apt-get purge --auto-remove -y $BUILD_PKG ; \
    fi



###################################
#
#  Finalization
#

# Limit threads for OpenMP:
# ENV OMP_THREAD_LIMIT=1

# Auto-deploy OImaging-uws.war on startup
# note: CONTEXT_PATH is not honored : app dir is the basename of the war (OImaging-uws here)
ADD OImaging-uws.war /usr/local/tomcat/webapps/

# Reduce memory footprint for tomcat (UWS):
ENV CATALINA_OPTS="-Xms128m -Xmx512m"

USER tomcat
EXPOSE 8080
